package com.orynnx.hicreatehook.support.nativepatch;

import android.content.Context;

import com.orynnx.hicreatehook.support.xposed.MethodHookParam;
import com.orynnx.hicreatehook.support.xposed.XC_MethodHook;
import com.orynnx.hicreatehook.support.xposed.XposedBridge;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads a patched copy of libnativelib.so from the target app's own files dir.
 *
 * <p>The target APK uses {@code extractNativeLibs=false}, so there is no
 * extracted library to replace. Instead this copies the app native libs once,
 * validates libnativelib by SHA-256, applies the 37 byte patches, and redirects
 * {@code System.loadLibrary("nativelib")} to the copy. The target app's own
 * ClassLoader is used for the load so JNI_OnLoad can resolve target classes.</p>
 */
public final class NativeCompat {

    private static final String MANIFEST = "manifest.json";
    private static final Object LOCK = new Object();
    private static final Set<String> LOADED = new HashSet<>();

    private static volatile boolean installed;

    private NativeCompat() {
    }

    public static void resetForHotReload() {
        synchronized (LOCK) {
            installed = false;
        }
    }

    public static void install(Context context, ClassLoader targetLoader) {
        if (installed) return;
        installed = true;
        try {
            File stubDex = findStubDex(context);
            if (stubDex == null) {
                return;
            }
            dalvik.system.DexClassLoader loader = new dalvik.system.DexClassLoader(
                    stubDex.getAbsolutePath(),
                    context.getCodeCacheDir().getAbsolutePath(),
                    null,
                    targetLoader);
            Class<?> loaderClass = loader.loadClass(
                    "com.orynnx.hicreatehook.loader.TargetNativeLoader");
            Method load = loaderClass.getMethod("load", String.class);
            XposedBridge.hookAllMethods(System.class, "loadLibrary", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length == 0
                            || !"nativelib".equals(param.args[0])) {
                        return;
                    }
                    synchronized (LOCK) {
                        File lib = prepare(context);
                        if (lib == null) {
                            return;
                        }
                        try {
                            loadDependencies(lib.getParentFile(), load);
                            load.invoke(null, lib.getAbsolutePath());
                            param.setResult(null);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static File findStubDex(Context context) {
        File dir = context.getCodeCacheDir();
        File fixed = new File(dir, "hicreate-stubs.dex");
        if (fixed.isFile()) return fixed;
        File[] files = dir.listFiles();
        if (files == null) return null;
        File newest = null;
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith("hicreate-stubs") || !name.endsWith(".dex")) continue;
            if (newest == null || file.lastModified() > newest.lastModified()) {
                newest = file;
            }
        }
        return newest;
    }

    private static File prepare(Context context) {
        File base = context.getFilesDir();
        File dir = new File(base, "native");
        if (cacheValid(context, dir)) {
            return new File(dir, NativePatch.NATIVELIB);
        }
        File temp = new File(base, "native.tmp");
        deleteRecursive(temp);
        if (!temp.mkdirs()) {
            return null;
        }
        try {
            copyLibraries(context, temp);
            File lib = new File(temp, NativePatch.NATIVELIB);
            if (!NativePatch.ORIGINAL_SHA256.equals(NativePatch.sha256(lib))) {
                // Only the stock APK is a supported input. An APK that already
                // contains a patched lib belongs to another distribution and
                // must not be silently treated as the original.
                deleteRecursive(temp);
                return null;
            }
            Map<String, String> sourceHashes = NativePatch.hashLibraries(temp);
            NativePatch.apply(lib);
            if (!NativePatch.PATCHED_SHA256.equals(NativePatch.sha256(lib))) {
                deleteRecursive(temp);
                return null;
            }
            writeManifest(temp, sourceHashes);
            if (!swap(temp, dir)) {
                deleteRecursive(temp);
                return null;
            }
            return new File(dir, NativePatch.NATIVELIB);
        } catch (Throwable ignored) {
            deleteRecursive(temp);
            return null;
        }
    }

    private static boolean cacheValid(Context context, File dir) {
        try {
            File lib = new File(dir, NativePatch.NATIVELIB);
            File manifestFile = new File(dir, MANIFEST);
            if (!lib.isFile() || !manifestFile.isFile()) return false;
            if (!NativePatch.PATCHED_SHA256.equals(NativePatch.sha256(lib))) return false;
            JSONObject manifest = new JSONObject(read(manifestFile));
            JSONObject source = manifest.optJSONObject("source");
            if (source == null) return false;
            for (String name : NativePatch.LIBRARIES) {
                String expected = source.optString(name, "");
                String current = sourceHash(context, name);
                if (expected.isEmpty() || !expected.equals(current)) return false;
                if (NativePatch.NATIVELIB.equals(name)) {
                    if (!NativePatch.ORIGINAL_SHA256.equals(current)) return false;
                } else {
                    File cached = new File(dir, name);
                    if (!cached.isFile() || !current.equals(NativePatch.sha256(cached))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void copyLibraries(Context context, File dir) throws Exception {
        try (ZipFile zip = new ZipFile(context.getApplicationInfo().sourceDir)) {
            for (String name : NativePatch.LIBRARIES) {
                ZipEntry entry = zip.getEntry("lib/arm64-v8a/" + name);
                if (entry == null) throw new IllegalStateException("missing " + name);
                File out = new File(dir, name);
                try (InputStream in = zip.getInputStream(entry)) {
                    NativePatch.copy(in, out);
                }
            }
        }
    }

    private static String sourceHash(Context context, String name) throws Exception {
        try (ZipFile zip = new ZipFile(context.getApplicationInfo().sourceDir)) {
            ZipEntry entry = zip.getEntry("lib/arm64-v8a/" + name);
            if (entry == null) return "";
            try (InputStream in = zip.getInputStream(entry)) {
                return NativePatch.sha256(in);
            }
        }
    }

    private static void writeManifest(File dir, Map<String, String> sourceHashes) throws Exception {
        JSONObject source = new JSONObject();
        for (Map.Entry<String, String> entry : sourceHashes.entrySet()) {
            source.put(entry.getKey(), entry.getValue());
        }
        JSONObject root = new JSONObject();
        root.put("source", source);
        root.put("patched", NativePatch.PATCHED_SHA256);
        try (FileOutputStream out = new FileOutputStream(new File(dir, MANIFEST))) {
            out.write(root.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static boolean swap(File temp, File dir) {
        File old = new File(dir.getParentFile(), "native.old");
        deleteRecursive(old);
        if (dir.exists() && !dir.renameTo(old)) {
            return false;
        }
        if (!temp.renameTo(dir)) {
            if (old.exists()) old.renameTo(dir);
            return false;
        }
        deleteRecursive(old);
        return true;
    }

    private static void loadDependencies(File dir, Method load) {
        for (String name : NativePatch.LOAD_ORDER) {
            if (LOADED.contains(name)) continue;
            File lib = new File(dir, name);
            if (!lib.isFile()) continue;
            try {
                load.invoke(null, lib.getAbsolutePath());
                LOADED.add(name);
            } catch (Throwable ignored) {
            }
        }
    }

    private static String read(File file) throws Exception {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int offset = 0;
            int read;
            while (offset < data.length && (read = in.read(data, offset, data.length - offset)) > 0) {
                offset += read;
            }
        }
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }
}


