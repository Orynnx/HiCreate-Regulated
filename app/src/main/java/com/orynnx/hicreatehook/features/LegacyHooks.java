package com.orynnx.hicreatehook.features;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.orynnx.hicreatehook.StubDex;
import com.orynnx.hicreatehook.runtime.HookContext;
import com.orynnx.hicreatehook.support.nativepatch.NativeCompat;
import com.orynnx.hicreatehook.support.xposed.MethodHookParam;
import com.orynnx.hicreatehook.support.xposed.XC_MethodHook;
import com.orynnx.hicreatehook.support.xposed.XposedBridge;
import com.orynnx.hicreatehook.support.xposed.XposedHelpers;
/**
 * LSPosed module for com.huawei.hicreate (GoPaint / 天生会画).
 *
 * 1) Grants every Huawei signature/permission check performed inside the app.
 * 2) Fakes MANAGE_EXTERNAL_STORAGE app-ops.
 * 3) Optional: neutralises the first startup blocker (BuildExUtil.a()).
 */
@SuppressLint("ResourceType")
public final class LegacyHooks {

    private static final java.util.Map<Object, Float> WORK_TOUCH_DOWN_X =
            new java.util.WeakHashMap<Object, Float>();
    private static final String TARGET = "com.huawei.hicreate";
    /** Huawei's original signing certificate (DER, base64). */
    private static final String HW_CERT_B64 = "MIIEgjCCAuqgAwIBAgIGAYbGvTXZMA0GCSqGSIb3DQEBDAUAMFAxCzAJBgNVBAYTAkNOMQ8wDQYDVQQKDAZIdWF3ZWkxEzARBgNVBAsMCkh1YXdlaSBDQkcxGzAZBgNVBAMMEmFwa2tleV9oaWNyZWF0ZV92MTAgFw0yMzAzMDkxNDE4NTRaGA8yMDUzMDMwOTE0MTg1NFowUDELMAkGA1UEBhMCQ04xDzANBgNVBAoMBkh1YXdlaTETMBEGA1UECwwKSHVhd2VpIENCRzEbMBkGA1UEAwwSYXBra2V5X2hpY3JlYXRlX3YxMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA5dHgVVtzCXNg4Btcja9W8ocxIWuVNR++fZlRqRBLv7ifphvjd6lmPJ1WuhbchcO9w1gq7uiMRLtTXtj4o5Mriq3wnFuyRdWLjOXJcHC5bRwAdqh1YSCOl8YGmzLvhLi2kmrj6QegX0vPsC69Kt8XjEL/N9n7dOlqY49Rlj1DXEogBAkohGfEBeVraKfg+JleOkMbxWokAuN5oTHJqKg6rul6RVVW/gNQavqiARPx+AU8aHIqTKgUr6bknBmqEpXSPGAZmZbu7yb7mQ/KV9HHDrmVadDleypLIyqEDhXIyFzZi6A/b+yEf2vnOfSxZNT/uGHNYX/KsUt4AvgUtr74M2v2QMFQOtQqA417JoOPcCO//lUHUf5AiUdUzjVlSkFvszjlBFfzzV0NvZT/dOgLrD3ApOpyWcJLRHAY741uW/XZLXUVri4bIuprX9xRd78T8ls7aDWNFZFt3HPV80QFrtxrMC/ebDPFnY70q0lNqfAeLcYHc/vfoRkS4/n2oYfRAgMBAAGjYDBeMB0GA1UdDgQWBBToUqlWa1JZY9x3CT37qT2b/kG2nDAOBgNVHQ8BAf8EBAMCB4AwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBToUqlWa1JZY9x3CT37qT2b/kG2nDANBgkqhkiG9w0BAQwFAAOCAYEABrYHTHG1eyR0t1pTa6jWxiJ4iEimx4SUjkEYq6rXqo1wg3tdAUIIDLOxc0ZxC/JHAuZA4/aYPw9Sn8AIZPRBWM0PcxA1diNLLuKY+8HPT1ac+SWpaND9jWVsT5bk5GCdsx7VuEkfVigvnkG8FLyVQ/bWwUGW6wIe+7k49KbHob4lmfjj/az8rCT/T29AJ35CIV64P9H1qokPwuuYmX0uEyGNjUOuRfjcrIuD9kQT9am1WWXHHikDYjNb/x4JnoVu94pi42y6Y5V8bHJDlCXCf7LLM49ftlyIqejR1wANZPt8BwbBpWtzYDLe15zgq+XmAzkJse0yX8+bBL+eK+9Szbxww8wMJnh/iTkCrl+3DvqHBvOUSIHWl7h/SETIOelB/kCcxweuP9CvhA9DjKWlMAKNujoehqoKPqIntmq+4GyEVB7Ap2hjYnd67uxIBOuxafFPvcrEWeuPGJ9pEeHfUf7mwMJ6ZRQ2g6Y0ojiY7/cENglfASWhFpvcA0pKZYRq";
    private static final boolean BYPASS_STARTUP = false;
    /** Spoof the target's original Huawei signing certificate inside HMS Core. */
    private static final boolean BYPASS_HMS_CERT = true;

    /** Fallback Resources backed by Huawei's framework-res-hwext.apk (package 0x02). */
    private static android.content.res.Resources HW_RES;
    private static android.content.res.Resources HW_BASE;
    private static boolean hwTried;

    /** Locate the target's per-user code-cache directory without assuming user 0. */
    private static java.io.File hwextFile() {
        try {
            Object app = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app instanceof android.content.Context) {
                return new java.io.File(
                        ((android.content.Context) app).getCodeCacheDir(), "hwext-res.apk");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** The hwext APK is bundled in the module assets; unpack it into the app code cache. */
    private static boolean ensureHwext() {
        java.io.File target = hwextFile();
        if (target == null) return false;
        java.io.File dir = target.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
        String modulePath = findModuleApk();
        if (modulePath == null) return false;
        java.io.File temp = null;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(modulePath)) {
            java.util.zip.ZipEntry entry = zip.getEntry("assets/hwext-res.apk");
            if (entry == null) return false;
            long expected = entry.getSize();
            if (target.isFile() && (expected > 0
                    ? target.length() == expected
                    : target.length() > 1000000L)) {
                return true;
            }
            if (dir == null) return false;
            temp = java.io.File.createTempFile("hwext-res", ".tmp", dir);
            try (java.io.InputStream in = zip.getInputStream(entry);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(temp)) {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                out.flush();
                out.getFD().sync();
            }
            if (expected > 0 ? temp.length() != expected : temp.length() <= 1000000L) {
                temp.delete();
                return false;
            }
            if (target.exists() && !target.delete()) {
                temp.delete();
                return false;
            }
            if (!temp.renameTo(target)) {
                temp.delete();
                return false;
            }
            temp = null;
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (temp != null) temp.delete();
        }
    }

    private static synchronized android.content.res.Resources hwResources(
            android.content.res.Resources base) {
        if (base != null) {
            HW_BASE = base;
        } else if (HW_BASE == null) {
            try {
                Object app = Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null);
                if (app instanceof android.content.Context) {
                    HW_BASE = ((android.content.Context) app).getResources();
                }
            } catch (Throwable ignored) {
            }
        }
        if (HW_RES != null) return HW_RES;
        if (hwTried) return null;
        hwTried = true;
        try {
            if (!ensureHwext()) {
                hwTried = false;
                return null;
            }
            java.io.File hwext = hwextFile();
            if (hwext == null || !hwext.isFile()) {
                hwTried = false;
                return null;
            }
            if (HW_BASE == null) {
                hwTried = false;
                return null;
            }
            Class<?> amClass = Class.forName("android.content.res.AssetManager");
            android.content.res.AssetManager am = (android.content.res.AssetManager)
                    amClass.getDeclaredConstructor().newInstance();
            Object added = amClass.getMethod("addAssetPath", String.class)
                    .invoke(am, hwext.getAbsolutePath());
            if (!(added instanceof Integer) || ((Integer) added) == 0) {
                hwTried = false;
                return null;
            }
            HW_RES = new android.content.res.Resources(am,
                    HW_BASE.getDisplayMetrics(), HW_BASE.getConfiguration());
            return HW_RES;
        } catch (Throwable t) {
            hwTried = false;
            return null;
        }
    }

    /** Fill a missing androidhwext (0x02xxxxxx) value from the Huawei resource package. */
    private static boolean fillFromHuawei(android.content.res.Resources base, int id,
            android.util.TypedValue tv, boolean resolveRefs) {
        if (((id >>> 24) & 0xFF) != 0x02) return false;
        if (tv == null) return false;
        android.content.res.Resources hw = hwResources(base != null ? base : HW_BASE);
        if (hw == null) return false;
        try {
            hw.getValue(id, tv, resolveRefs);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Only these exact names are answered from the module dex (avoid recursion). */
    private static final java.util.HashSet<String> STUB_CLASSES = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "com.huawei.android.os.SystemPropertiesEx",
                    "com.huawei.android.os.BuildEx",
                    "com.huawei.android.os.BuildEx$VERSION",
                    "com.huawei.android.os.UserHandleEx",
                    "com.huawei.system.BuildEx",
                    "com.huawei.android.app.PackageManagerEx",
                    "com.huawei.android.app.ActivityManagerEx",
                    "com.huawei.android.content.pm.ApplicationInfoEx",
                    "com.huawei.android.view.WindowManagerEx",
                    "com.huawei.android.view.WindowManagerEx$LayoutParamsEx"));

    private static final ThreadLocal<Boolean> IN_STUB_LOAD = new ThreadLocal<>();

    private static final String DEVICE_MODEL = "DBY-W09";

    private final HookContext ctx;

    private static boolean isProcessOf(String processName, String packageName) {
        return packageName.equals(processName) || processName.startsWith(packageName + ":");
    }

    public LegacyHooks(HookContext context) {
        this.ctx = context;
    }

    public void install() {
        String packageName = ctx.processName();
        if (isProcessOf(packageName, "com.huawei.hwid")) {
            spoofBuildFields();
            hookSystemProperties();
            hookHmsCertCheck(ctx.classLoader());
            return;
        }
        if (!isProcessOf(packageName, TARGET)) {
            return;
        }
        installStubClassLoader();
        hookResourceFallbacks();
        hookTypedValueFallbacks();
        hookResourcesImplGetValue();
        spoofBuildFields();
        hookSystemProperties();
        hookExit();
        hookDeviceSupport(ctx.classLoader());
        hookHomeActivity(ctx.classLoader());
        hookProductSupport(ctx.classLoader());
        hookApplicationCreate(ctx.classLoader());
        hookDarkMode(ctx.classLoader());
        hookTextColors();
        hookDisabledAlpha();
        hookLayoutInflaterMapping();
        hookInflaterFallback();
        hookAnimationFallback();
        hookStandardActivitiesTheme(ctx.classLoader());
        hookDialogs(ctx.classLoader());
        hookPermissionChecks(ctx.classLoader());
        hookAppOps();
        hookNetworkChecks(ctx.classLoader());
        if (BYPASS_STARTUP) {
            hookStartupBlockers(ctx.classLoader());
        }
    }


    private static void hookHmsCertCheck(ClassLoader cl) {
        if (!BYPASS_HMS_CERT) {
            return;
        }
        try {
            final byte[] cert = android.util.Base64.decode(HW_CERT_B64, android.util.Base64.NO_WRAP);
            Class<?> apm = XposedHelpers.findClass("android.app.ApplicationPackageManager", cl);
            XC_MethodHook spoof = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (!(r instanceof android.content.pm.PackageInfo)) return;
                    android.content.pm.PackageInfo pi = (android.content.pm.PackageInfo) r;
                    if (!TARGET.equals(pi.packageName)) return;
                    android.content.pm.Signature sig = new android.content.pm.Signature(cert);
                    pi.signatures = new android.content.pm.Signature[]{sig};
                    // HMS Core 6.x requests GET_SIGNING_CERTIFICATES and reads this
                    // field; changing only the deprecated signatures field is ignored.
                    try {
                        pi.signingInfo = makeSigningInfo(sig);
                    } catch (Throwable t) {
                    }
                }
            };
            // Cover both the legacy int-flags and the Android 13+ PackageInfoFlags
            // overloads. The hook checks the returned PackageInfo, so unrelated
            // package queries pass through untouched.
            for (String m : new String[]{"getPackageInfo", "getPackageInfoAsUser",
                    "getPackageArchiveInfo"}) {
                XposedBridge.hookAllMethods(apm, m, spoof);
            }

            // HMS provider checks also compare package signatures directly. Return
            // SIGNATURE_MATCH only for the target and an HMS provider package.
            XposedBridge.hookAllMethods(apm, "checkSignatures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length < 2
                            || !(p.args[0] instanceof String)
                            || !(p.args[1] instanceof String)) {
                        return;
                    }
                    String a = (String) p.args[0];
                    String b = (String) p.args[1];
                    if (isHmsSignaturePeer(a, b)) {
                        p.setResult(Integer.valueOf(0)); // PackageManager.SIGNATURE_MATCH
                    }
                }
            });

            // Some HMS branches use the certificate-bytes API instead of
            // checkSignatures. It is scoped to the target package and the HMS
            // process, so it cannot alter ordinary package queries elsewhere.
            XposedBridge.hookAllMethods(apm, "hasSigningCertificate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args != null && p.args.length > 0 && TARGET.equals(p.args[0])) {
                        p.setResult(Boolean.TRUE);
                    }
                }
            });
        } catch (Throwable t) {
        }
        try {
            Class<?> c = XposedHelpers.findClass("R0.c", cl);
            XposedBridge.hookAllMethods(c, "d", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(Integer.valueOf(0));
                }
            });
        } catch (Throwable t) {
        }
        try {
            // HMS Core keeps the app's granted kit scopes in AppScope. On the
            // Samsung port the local cache has no entry for HiCreate's JOS
            // permission, so AuthService returns 6004 before it reaches the
            // account feature. Let only this appId pass that local gate; the
            // real account/plugin request still has to succeed afterwards.
            Class<?> scope = XposedHelpers.findClass(
                    "com.huawei.hms.auth.scope.bean.AppScope", cl);
            XposedBridge.hookAllMethods(scope, "checkPermission", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.thisObject == null || p.args == null || p.args.length == 0
                            || !(p.args[0] instanceof String)) {
                        return;
                    }
                    String appId = null;
                    try {
                        Object v = XposedHelpers.callMethod(p.thisObject, "getAppID");
                        appId = v == null ? null : String.valueOf(v);
                    } catch (Throwable ignored) {
                    }
                    if (!"110011447".equals(appId)) return;
                    String permission = (String) p.args[0];
                    if (permission.startsWith("com.huawei.android.hms.common.jos")
                            || permission.startsWith("com.huawei.android.hms.game")) {
                        p.setResult(Integer.valueOf(0));
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * Build a SigningInfo without compiling against its hidden constructor.
     * Android 14's SDK stubs expose only the no-arg constructor, while the
     * device runtime still carries the four-argument constructor used by the
     * package manager. Keep a field-based fallback for runtimes that hide it.
     */
    private static android.content.pm.SigningInfo makeSigningInfo(
            android.content.pm.Signature sig) throws Exception {
        java.util.List<android.content.pm.Signature> signers =
                java.util.Collections.singletonList(sig);
        try {
            java.lang.reflect.Constructor<android.content.pm.SigningInfo> c =
                    android.content.pm.SigningInfo.class.getDeclaredConstructor(
                            int.class, java.util.Collection.class,
                            java.util.Collection.class, java.util.Collection.class);
            c.setAccessible(true);
            return c.newInstance(3, signers, null, null);
        } catch (NoSuchMethodException ignored) {
            android.content.pm.SigningInfo info = new android.content.pm.SigningInfo();
            setField(info, "mSchemeVersion", Integer.valueOf(3));
            setField(info, "mApkContentsSigners",
                    new android.content.pm.Signature[]{sig});
            setField(info, "mSigningCertificateHistory", null);
            return info;
        }
    }

    private static void setField(Object object, String name, Object value) throws Exception {
        Class<?> c = object.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(object, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static boolean isHmsSignaturePeer(String a, String b) {
        if (a == null || b == null) return false;
        if (TARGET.equals(a)) return isHmsPackage(b);
        if (TARGET.equals(b)) return isHmsPackage(a);
        return false;
    }

    private static boolean isHmsPackage(String pkg) {
        return "com.huawei.hwid".equals(pkg)
                || "com.huawei.hwid.tv".equals(pkg)
                || "com.huawei.hms".equals(pkg);
    }

    private static boolean isHuaweiPermission(String p) {
        if (p == null) {
            return false;
        }
        return p.startsWith("huawei.android.permission.")
                || p.startsWith("com.huawei.permission.")
                || p.startsWith("com.huawei.android.permission.")
                || p.equals("android.permission.MANAGE_EXTERNAL_STORAGE")
                || p.equals("android.permission.REQUEST_INSTALL_PACKAGES");
    }

    /**
     * ART resolves type references straight from the app's dex files and does
     * not go through ClassLoader.loadClass(), so the stub classes must become
     * part of the app class loader's dex path. The module APK path is obtained
     * from PackageManager once a Context exists, i.e. right before
     * Application.onCreate() runs.
     */
    private void hookApplicationCreate(final ClassLoader appLoader) {
        try {
            Class<?> appCls = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.HiCreateApplication", appLoader);
            XposedBridge.hookAllMethods(appCls, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof android.content.Context)) {
                        return;
                    }
                    installLate((android.content.Context) param.thisObject, appLoader);
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Application-level hooks that need a live Context; also replayed after hot reload. */
    public void installLate(android.content.Context ctx, ClassLoader appLoader) {
        if (!isProcessOf(this.ctx.processName(), TARGET)) {
            return;
        }
        // Keep one failed injection from suppressing unrelated hooks.
        runSafely(() -> injectStubDex(ctx, appLoader));
        runSafely(() -> injectHwDex(ctx, appLoader));
        runSafely(() -> injectHwEmuiDex(ctx, appLoader));
        runSafely(() -> injectFullFrameworkDex(ctx, appLoader));
        runSafely(() -> injectWidgetDex(ctx, appLoader));
        runSafely(() -> NativeCompat.install(ctx, appLoader));
        runSafely(() -> hookStoreHostRewrite(appLoader));
        runSafely(() -> hookVibratorCompat(appLoader));
        // The adapter references huawei.android.widget.SwipeLayout, so it can
        // only be resolved after the stub dex is on the class path.
        runSafely(() -> hookBrushRowIcons(appLoader));
        runSafely(() -> hookSwipeDispatch(appLoader));
        runSafely(() -> hookWorksTouch(appLoader));
        runSafely(() -> hookMoveWorksActivity(appLoader));
        runSafely(() -> hookHuaweiFilePicker(appLoader));
        runSafely(() -> hookImportLoadingBinding(appLoader));
        runSafely(LegacyHooks::hookShareIntent);
        runSafely(() -> hookDialogListAdapter(appLoader));
        runSafely(() -> hookNetworkChecks(appLoader));
    }

    private static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
        }
    }

    /** Write the stub dex through a temporary file so a crash cannot leave a partial dex. */
    private static void injectStubDex(android.content.Context ctx, ClassLoader appLoader) {
        java.io.File temp = null;
        try {
            byte[] dex = android.util.Base64.decode(StubDex.BASE64, android.util.Base64.NO_WRAP);
            java.io.File dir = ctx.getCodeCacheDir();
            java.io.File target = new java.io.File(dir, "hicreate-stubs.dex");
            if (target.exists() && !target.delete()) {
                target = new java.io.File(dir, "hicreate-stubs-" + System.nanoTime() + ".dex");
            }
            temp = java.io.File.createTempFile("hicreate-stubs", ".tmp", dir);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(temp)) {
                out.write(dex);
                out.getFD().sync();
            }
            if (target.exists() && !target.delete()) {
                target = new java.io.File(dir, "hicreate-stubs-" + System.nanoTime() + ".dex");
            }
            if (!temp.renameTo(target)) {
                return;
            }
            temp = null;
            addDexPath(appLoader, target.getAbsolutePath());
        } catch (Throwable ignored) {
        } finally {
            if (temp != null) temp.delete();
        }
    }

    /** Diagnose the native Huawei folder-move screen without replacing its UI. */
    private static void hookMoveWorksActivity(final ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.workmanage.MoveWorksActivity", cl);
            XposedBridge.hookAllMethods(c, "onCreate", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                }
            });
            XposedBridge.hookAllMethods(c, "onResume", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Trace the real work-list gesture path; Huawei uses WorksTouchListener rather than
     * SwipeLayout for the home grid. This also prevents the compatibility layer from
     * silently swallowing the gesture while the Huawei action surface is absent. */
    private static void hookWorksTouch(ClassLoader cl) {
        try {
            Class<?> touch = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.workmanage.drag.WorksTouchListener", cl);
            XposedBridge.hookAllMethods(touch, "onTouch", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args != null && p.args.length > 1 && p.args[0] instanceof android.view.View
                            && p.args[1] instanceof android.view.MotionEvent) {
                        android.view.MotionEvent e = (android.view.MotionEvent) p.args[1];
                        if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                            ((android.view.View) p.args[0]).getParent().requestDisallowInterceptTouchEvent(true);
                            synchronized (WORK_TOUCH_DOWN_X) { WORK_TOUCH_DOWN_X.put(p.thisObject, e.getX()); }
                        }
                    }
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (p.args != null && p.args.length > 1 && p.args[1] instanceof android.view.MotionEvent) {
                        android.view.MotionEvent e = (android.view.MotionEvent) p.args[1];
                        if (e.getActionMasked() != android.view.MotionEvent.ACTION_MOVE) {
                            if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                                Float down;
                                synchronized (WORK_TOUCH_DOWN_X) { down = WORK_TOUCH_DOWN_X.remove(p.thisObject); }
                                if (down != null && down.floatValue() - e.getX() > 80f) {
                                    try {
                                        // The item is translated left during the swipe, so its
                                        // ACTION_UP local X is negative. showMenu uses that value
                                        // as a popup anchor; normalize it back into the item.
                                        android.view.MotionEvent menuEvent = android.view.MotionEvent.obtain(e);
                                        menuEvent.setLocation(0f, e.getY());
                                        XposedHelpers.callMethod(p.thisObject, "showMenu", menuEvent);
                                        menuEvent.recycle();
                                    } catch (Throwable t) {
                                    }
                                }
                            }
                        }
                    }
                }
            });
            Class<?> callback = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.workmanage.adapter.holder.WorkGridViewHolder$WorksTouchCallbackImpl", cl);
            for (String method : new String[]{"startDrag", "d", "f", "a", "b", "c", "e", "k", "l", "m"}) {
                XposedBridge.hookAllMethods(callback, method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if ("startDrag".equals(p.method.getName())) {
                            try {
                                Object holder = XposedHelpers.getObjectField(p.thisObject, "d");
                                Object action = XposedHelpers.getObjectField(holder, "d");
                                if (action instanceof android.view.View) {
                                    android.view.View v = (android.view.View) action;
                                }
                                Object binding = XposedHelpers.getObjectField(holder, "g");
                                if (binding != null) {
                                    for (java.lang.reflect.Field bf : binding.getClass().getDeclaredFields()) {
                                        try {
                                            bf.setAccessible(true);
                                            Object bv = bf.get(binding);
                                            if (bv instanceof android.view.View) {
                                                android.view.View vv = (android.view.View) bv;
                                            }
                                        } catch (Throwable ignored) { }
                                    }
                                }
                            } catch (Throwable ignored) { }
                        }
                    }
                });
            }
            Class<?> menuCb = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.workmanage.adapter.item.ItemClickCallbackImpl", cl);
            XposedBridge.hookAllMethods(menuCb, "onMenuClick", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args != null) {
                        String ids = "";
                        for (Object a : p.args) if (a instanceof int[]) {
                            int[] menuIds = (int[]) a;
                            ids = java.util.Arrays.toString(menuIds);
                            // The native MoveWorksActivity blocks in its
                            // native folder engine. There is no folder target in the stock shelf,
                            // so consume this no-op action instead of taking down HomeActivity.
                            if (false && menuIds.length > 1 && menuIds[1] == 163) {
                                p.setResult(null);
                                try {
                                    android.view.View source = (p.args[0] instanceof android.view.View)
                                            ? (android.view.View) p.args[0] : null;
                                    android.content.Context ctx = source == null ? null : source.getContext();
                                    if (ctx != null) android.widget.Toast.makeText(ctx,
                                            "当前设备没有可移动的作品文件夹", android.widget.Toast.LENGTH_SHORT).show();
                                } catch (Throwable ignored) { }
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Package visibility hides third-party packages from a targetSdk-30 app. */
    private static String findModuleApkPath(android.content.Context ctx) {
        final String pkg = "com.orynnx.hicreatehook";
        try {
            Class<?> ag = Class.forName("android.app.AppGlobals");
            Object ipm = ag.getDeclaredMethod("getPackageManager").invoke(null);
            String path = sourceDirOf(invokeGetApplicationInfo(ipm, pkg));
            if (path != null) {
                return path;
            }
        } catch (Throwable t) {
        }
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Object binder = sm.getDeclaredMethod("getService", String.class)
                    .invoke(null, "package");
            Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
            Object ipm = stub.getDeclaredMethod("asInterface", android.os.IBinder.class)
                    .invoke(null, binder);
            String path = sourceDirOf(invokeGetApplicationInfo(ipm, pkg));
            if (path != null) {
                return path;
            }
        } catch (Throwable t) {
        }
        try {
            return ctx.getPackageManager().getApplicationInfo(pkg, 0).sourceDir;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeGetApplicationInfo(Object ipm, String pkg) {
        for (java.lang.reflect.Method m : ipm.getClass().getMethods()) {
            if (!"getApplicationInfo".equals(m.getName())) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            try {
                if (p.length == 3 && p[0] == String.class && p[1] == long.class) {
                    return m.invoke(ipm, pkg, 0L, 0);
                }
                if (p.length == 3 && p[0] == String.class && p[1] == int.class) {
                    return m.invoke(ipm, pkg, 0, 0);
                }
            } catch (Throwable ignored) {
                // try next overload
            }
        }
        return null;
    }

    private static String sourceDirOf(Object appInfo) {
        if (appInfo == null) {
            return null;
        }
        try {
            java.lang.reflect.Field f = appInfo.getClass().getField("sourceDir");
            return (String) f.get(appInfo);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Huawei's hwframework.jar contains the real Gson fork and other helpers. */
    private static void injectHwDex(android.content.Context ctx, ClassLoader appLoader) {
        injectDexAsset(ctx, appLoader, "assets/hwframework.dex", "hwframework.dex");
    }

    /** Locate the module APK through the class loader that owns this class. */
    private static String findModuleApk() {
        try {
            ClassLoader cl = LegacyHooks.class.getClassLoader();
            Object pathList = null;
            for (Class<?> c = cl.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField("pathList");
                    f.setAccessible(true);
                    pathList = f.get(cl);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (pathList == null) return null;
            String text = String.valueOf(pathList);
            int idx = text.indexOf("com.orynnx.hicreatehook");
            if (idx < 0) return null;
            // The module dex lives in memory; the APK path shows up as
            // /data/app/.../base.apk!/lib/arm64-v8a, so cut at '!'.
            int start = text.lastIndexOf("/data/app/", idx);
            int alt = text.lastIndexOf("zip file ", idx);
            if (alt >= 0 && (start < 0 || alt > start)) {
                start = alt + "zip file ".length();
            }
            if (start < 0) return null;
            int end = text.indexOf('!', start);
            if (end < 0) end = text.indexOf(',', start);
            if (end < 0) end = text.indexOf(']', start);
            if (end < 0) end = text.length();
            String path = text.substring(start, end).trim();
            if (path.startsWith("\"") && path.endsWith("\"")) {
                path = path.substring(1, path.length() - 1);
            }
            return path;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void injectWidgetDex(android.content.Context ctx, ClassLoader appLoader) {
        java.util.zip.ZipFile zip = null;
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            String modulePath = findModuleApk();
            if (modulePath == null) {
                return;
            }
            java.io.File moduleApk = new java.io.File(modulePath);
            zip = new java.util.zip.ZipFile(moduleApk);
            java.util.zip.ZipEntry entry = zip.getEntry("assets/featurelayer-widget.dex");
            if (entry == null) {
                return;
            }
            java.io.File outFile = new java.io.File(ctx.getCodeCacheDir(),
                    "featurelayer-widget.dex");
            if (outFile.exists() && !outFile.delete()) {
            }
            in = zip.getInputStream(entry);
            out = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            outFile.setReadOnly();
            addDexPath(appLoader, outFile.getAbsolutePath());
        } catch (Throwable t) {
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (zip != null) zip.close(); } catch (Throwable ignored) {}
        }
    }

    /** HMS/networkit helpers report "no network" on this port; the device is online. */
    private static void hookNetworkChecks(ClassLoader cl) {
        String[] names = {
                "com.huawei.hms.framework.common.NetworkUtil",
                "com.huawei.networkit.grs.common.NetworkUtil",
                "com.huawei.phoneservice.feedbackcommon.utils.NetworkUtils"
        };
        for (String name : names) {
            try {
                Class<?> c = XposedHelpers.findClass(name, cl);
                for (String m : new String[]{"isNetworkAvailable", "isNetworkConnected",
                        "isNetworkConnectedOrConnecting", "isConnected"}) {
                    XposedBridge.hookAllMethods(c, m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            p.setResult(Boolean.TRUE);
                        }
                    });
                }
            } catch (Throwable t) {
            }
        }
        try {
            Class<?> bra = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.resourcescenter.http.BaseRequestApi", cl);
            XposedBridge.hookAllMethods(bra, "a", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object result = p.getResult();
                    if (!(result instanceof java.util.Map)) return;
                    ((java.util.Map) result).put("x-device-type", "HWDBY");
                }
            });
        } catch (Throwable t) {
        }
        try {
            Class<?> rc = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.resourcescenter.activity.main.RcMainActivity", cl);
            XposedBridge.hookAllMethods(rc, "e0", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(Boolean.TRUE);
                }
            });
        } catch (Throwable t) {
        }
        try {
            Class<?> appUtil = XposedHelpers.findClass(
                    "com.huawei.hicreate.common.utils.network.NetworkUtil", cl);
            for (java.lang.reflect.Method m : appUtil.getDeclaredMethods()) {
                if (m.getReturnType() != boolean.class) continue;
                final String name = m.getName();
                XposedBridge.hookAllMethods(appUtil, name, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        p.setResult(Boolean.TRUE);
                    }
                });
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Samsung GRS resolves the store to the "dra" shard, which answers with an
     * empty result. The "drcn" shard returns the real catalog, so rewrite the
     * host before the request leaves the app. Mirrors alpha2's STORE_FIX.
     */
    private static void hookStoreHostRewrite(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.resourcescenter.http.NetworkClientWrapper", cl);
            XC_MethodHook rewrite = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length < 1
                            || !(p.args[0] instanceof String)) {
                        return;
                    }
                    String url = (String) p.args[0];
                    String fixed = url
                            .replace("iotopr-dra.things.dbankcloud.com",
                                    "iotopr-drcn.things.dbankcloud.cn")
                            .replace("iotopr-dra.things.dbankcloud.cn",
                                    "iotopr-drcn.things.dbankcloud.cn");
                    if (!fixed.equals(url)) {
                        p.args[0] = fixed;
                    }
                }
            };
            for (String method : new String[]{"h", "i", "g", "c"}) {
                XposedBridge.hookAllMethods(c, method, rewrite);
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Alpha3 keeps the real Huawei SwipeLayout helper. That helper constructs
     * VibratorEx through a framework AIDL that Samsung's boot image lacks, so
     * skip the constructor and report no hardware-vibrator support.
     */
    private static void hookVibratorCompat(ClassLoader cl) {
        try {
            Class<?> vibrator = XposedHelpers.findClass(
                    "com.huawei.android.os.VibratorEx", cl);
            XposedBridge.hookAllConstructors(vibrator, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(null);
                }
            });
            for (String method : new String[]{"isSupportHwVibrator", "isHwVibratorSupported"}) {
                XposedBridge.hookAllMethods(vibrator, method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        p.setResult(Boolean.FALSE);
                    }
                });
            }
        } catch (Throwable t) {
        }
    }

    private static void hookProductSupport(ClassLoader cl) {
        try {
            Class<?> product = XposedHelpers.findClass(
                    "com.huawei.hicreate.nativelib.product.ProductInfo", cl);
            hookConstant(product, "isNativeSupportDevice", Boolean.TRUE);
            hookConstant(product, "isNativeSupportSystem", Boolean.TRUE);
            hookConstant(product, "getNativeMaxLayerNumFactor", Float.valueOf(1.0f));
            hookConstant(product, "getNativeMaxSideValue", Integer.valueOf(8192));
            hookConstant(product, "getNativeMaxCanvasPixel", Long.valueOf(8192L * 8192L));
            hookConstant(product, "getNativeProductLevel", Integer.valueOf(3));
            hookConstant(product, "getNativeFeature", Boolean.TRUE);
        } catch (Throwable t) {
        }
    }

    private static void hookConstant(Class<?> cls, String method, final Object value) {
        XposedBridge.hookAllMethods(cls, method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(value);
            }
        });
    }

    /**
     * HiCreate only probes Huawei's hidisk picker. Samsung has no
     * com.huawei.filemanager, so the original import path stops before it
     * launches any picker. Make the probe succeed and translate that picker
     * intent to Android's standard DocumentsUI contract.
     */
    private static void hookHuaweiFilePicker(ClassLoader cl) {
        try {
            Class<?> fileUtils = XposedHelpers.findClass(
                    "com.huawei.hicreate.common.utils.file.FileUtils", cl);
            XposedBridge.hookAllMethods(fileUtils, "T", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(Boolean.TRUE);
                }
            });
        } catch (Throwable t) {
        }
    }

    /** The import loading layout also loses its Huawei child ids on Samsung. */
    private static void hookImportLoadingBinding(ClassLoader cl) {
        try {
            Class<?> binding = XposedHelpers.findClass(
                    "com.huawei.hicreate.databinding.LayoutImportLoadingDialogBinding", cl);
            XposedBridge.hookAllMethods(binding, "a", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length == 0
                            || !(p.args[0] instanceof android.view.View)) return;
                    android.view.View root = (android.view.View) p.args[0];
                    android.view.ViewGroup parent = root instanceof android.view.ViewGroup
                            ? (android.view.ViewGroup) root : null;
                    ensureImportChild(root, parent, 0x7f0a015d, "import_image");
                    ensureImportChild(root, parent, 0x7f0a0473, "import_progress");
                    ensureImportChild(root, parent, 0x7f0a084f, "web_loading_text");
                }
            });
        } catch (Throwable t) {
        }
    }

    private static android.view.View ensureImportChild(android.view.View root,
            android.view.ViewGroup parent, int id, String name) {
        android.view.View v = root.findViewById(id);
        if (v != null || parent == null) return v;
        try {
            android.content.Context ctx = root.getContext();
            if (id == 0x7f0a015d) {
                Class<?> c = XposedHelpers.findClass(
                        "com.huawei.uikit.hwimageview.widget.HwImageView", ctx.getClass().getClassLoader());
                v = (android.view.View) c.getConstructor(android.content.Context.class).newInstance(ctx);
            } else if (id == 0x7f0a0473) {
                Class<?> c = XposedHelpers.findClass(
                        "com.huawei.uikit.hwprogressbar.widget.HwProgressBar", ctx.getClass().getClassLoader());
                v = (android.view.View) c.getConstructor(android.content.Context.class).newInstance(ctx);
            } else {
                v = new android.widget.TextView(ctx);
            }
            v.setId(id);
            parent.addView(v, new android.view.ViewGroup.LayoutParams(-2, -2));
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Android 16's sharesheet reads URI grants from the chooser intent's
     * ClipData.  HiCreate's original FileUtils creates a chooser around the
     * SEND intent, then overwrites the inner intent flags with FLAG_ACTIVITY_NEW_TASK;
     * on Huawei framework this was tolerated, while Samsung's resolver refuses
     * to preview the FileProvider URI.  Repair only SEND/SEND_MULTIPLE intents
     * at the final Activity launch boundary and leave all other navigation alone.
     */
    private static void hookShareIntent() {
        final XC_MethodHook shareHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (p.args == null) return;
                for (int i = 0; i < p.args.length; i++) {
                    if (p.args[i] instanceof android.content.Intent) {
                        android.content.Intent intent = (android.content.Intent) p.args[i];
                        if (repairHuaweiPickerIntent(intent)) {
                        } else if (repairShareIntent(intent)) {
                        }
                        return;
                    }
                }
            }
        };
        try {
            XposedBridge.hookAllMethods(android.app.Activity.class,
                    "startActivity", shareHook);
            XposedBridge.hookAllMethods(android.app.Activity.class,
                    "startActivityForResult", shareHook);
        } catch (Throwable t) {
        }
        try {
            Class<?> instrumentation = XposedHelpers.findClass(
                    "android.app.Instrumentation", null);
            XposedBridge.hookAllMethods(instrumentation, "execStartActivity", shareHook);
        } catch (Throwable t) {
        }
    }

    private static boolean repairHuaweiPickerIntent(android.content.Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        String pkg = intent.getPackage();
        if (!"com.huawei.hidisk.intent.action.PICK".equals(action)
                && !"com.huawei.hidisk.action.SHARE_SAVE".equals(action)
                && !"com.huawei.filemanager".equals(pkg)) {
            return false;
        }
        String type = intent.getType();
        intent.setPackage(null);
        intent.setComponent(null);
        intent.setSelector(null);
        intent.setAction(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType(type == null || type.length() == 0 ? "*/*" : type);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                | android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return true;
    }

    private static boolean repairShareIntent(android.content.Intent intent) {
        if (intent == null) return false;
        android.content.Intent nested = null;
        String action = intent.getAction();
        if (android.content.Intent.ACTION_CHOOSER.equals(action)) {
            try {
                nested = intent.getParcelableExtra(android.content.Intent.EXTRA_INTENT);
            } catch (Throwable ignored) {
            }
            if (nested == null) return false;
        } else if (!android.content.Intent.ACTION_SEND.equals(action)
                && !android.content.Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            return false;
        }
        boolean changed = false;
        changed |= addShareGrant(nested == null ? intent : nested);
        if (nested != null) {
            changed |= addShareGrant(intent);
            android.content.ClipData clip = nested.getClipData();
            if (clip != null && intent.getClipData() == null) {
                intent.setClipData(clip);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean addShareGrant(android.content.Intent intent) {
        if (intent == null) return false;
        boolean changed = false;
        int wanted = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if ((intent.getFlags() & wanted) == 0) {
            intent.addFlags(wanted);
            changed = true;
        }
        android.content.ClipData clip = intent.getClipData();
        if (clip == null) {
            java.util.ArrayList<android.net.Uri> uris = new java.util.ArrayList<>();
            try {
                if (android.content.Intent.ACTION_SEND.equals(intent.getAction())) {
                    Object one = intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM);
                    if (one instanceof android.net.Uri) uris.add((android.net.Uri) one);
                } else if (android.content.Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                    java.util.ArrayList list = intent.getParcelableArrayListExtra(
                            android.content.Intent.EXTRA_STREAM);
                    if (list != null) {
                        for (Object item : list) {
                            if (item instanceof android.net.Uri) uris.add((android.net.Uri) item);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            if (!uris.isEmpty()) {
                android.content.ClipData repaired =
                        android.content.ClipData.newRawUri(null, uris.get(0));
                for (int i = 1; i < uris.size(); i++) {
                    repaired.addItem(new android.content.ClipData.Item(uris.get(i)));
                }
                intent.setClipData(repaired);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * The Huawei dialog_list_item layout loses its child ids when inflated with
     * Samsung's resource table. DialogListAdapter assumes all four children are
     * present and crashes while binding the share/menu list. Recreate only the
     * missing views and attach the expected ids so the original adapter logic
     * and click listeners remain unchanged.
     */
    private static void hookDialogListAdapter(ClassLoader cl) {
        try {
            Class<?> holder = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.common.view.dialog.DialogListAdapter$DialogListHolder",
                    cl);
            XposedBridge.hookAllConstructors(holder, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length == 0
                            || !(p.args[0] instanceof android.view.View)) {
                        return;
                    }
                    android.view.View root = (android.view.View) p.args[0];
                    android.view.ViewGroup parent = root instanceof android.view.ViewGroup
                            ? (android.view.ViewGroup) root : null;
                    android.view.View item = ensureDialogChild(root, parent,
                            0x7f0a03af, "item_view");
                    android.view.View icon = ensureDialogChild(root, parent,
                            0x7f0a0354, "icon_iv");
                    android.view.View title = ensureDialogChild(root, parent,
                            0x7f0a03ab, "item_title");
                    android.view.View divider = ensureDialogChild(root, parent,
                            0x7f0a0702, "small_divider");
                    try { XposedHelpers.setObjectField(p.thisObject, "a", item); }
                    catch (Throwable ignored) { }
                    try { XposedHelpers.setObjectField(p.thisObject, "b", icon); }
                    catch (Throwable ignored) { }
                    try { XposedHelpers.setObjectField(p.thisObject, "c", title); }
                    catch (Throwable ignored) { }
                    try { XposedHelpers.setObjectField(p.thisObject, "d", divider); }
                    catch (Throwable ignored) { }
                }
            });
        } catch (Throwable t) {
        }
    }

    private static android.view.View ensureDialogChild(android.view.View root,
            android.view.ViewGroup parent, int id, String name) {
        android.view.View v = root.findViewById(id);
        if (v != null || parent == null) return v;
        android.content.Context ctx = root.getContext();
        if (id == 0x7f0a03ab) {
            v = new android.widget.TextView(ctx);
        } else if (id == 0x7f0a0354) {
            v = new android.widget.ImageView(ctx);
        } else if (id == 0x7f0a03af) {
            v = new android.widget.RelativeLayout(ctx);
        } else {
            v = new android.view.View(ctx);
        }
        v.setId(id);
        android.view.ViewGroup.LayoutParams lp;
        if (v instanceof android.widget.TextView) {
            lp = new android.widget.RelativeLayout.LayoutParams(-1, -2);
        } else if (v instanceof android.widget.ImageView) {
            lp = new android.widget.RelativeLayout.LayoutParams(48, 48);
        } else {
            lp = new android.widget.RelativeLayout.LayoutParams(-1, 1);
        }
        parent.addView(v, lp);
        return v;
    }

    private static void injectFullFrameworkDex(android.content.Context ctx, ClassLoader appLoader) {
        try {
            java.io.File f = new java.io.File(ctx.getCodeCacheDir(), "hwframework-full.jar");
            if (!f.exists() || f.length() < 40_000_000L) {
                injectDexAsset(ctx, appLoader,
                        "assets/hwframework-full.jar", "hwframework-full.jar");
                return;
            }
            addDexPath(appLoader, f.getAbsolutePath());
        } catch (Throwable t) {
        }
    }

    private static void injectHwEmuiDex(android.content.Context ctx, ClassLoader appLoader) {
        injectDexAsset(ctx, appLoader, "assets/hwemui.dex", "hwemui.dex");
    }

    private static void injectDexAsset(android.content.Context ctx, ClassLoader appLoader,
            String assetName, String fileName) {
        java.util.zip.ZipFile zip = null;
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            String modulePath = findModuleApk();
            if (modulePath == null) {
                return;
            }
            zip = new java.util.zip.ZipFile(new java.io.File(modulePath));
            java.util.zip.ZipEntry entry = zip.getEntry(assetName);
            if (entry == null) {
                return;
            }
            java.io.File outFile = new java.io.File(ctx.getCodeCacheDir(), fileName);
            if (outFile.exists() && !outFile.delete()) {
            }
            in = zip.getInputStream(entry);
            out = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            outFile.setReadOnly();
            addDexPath(appLoader, outFile.getAbsolutePath());
        } catch (Throwable t) {
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (zip != null) zip.close(); } catch (Throwable ignored) {}
        }
    }

    private static void copyFile(java.io.File src, java.io.File dst) throws java.io.IOException {
        if (dst.exists() && dst.length() == src.length()) {
            return;
        }
        java.io.FileInputStream in = new java.io.FileInputStream(src);
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static void addDexPath(ClassLoader loader, String path) {
        try {
            Class<?> bdc = Class.forName("dalvik.system.BaseDexClassLoader");
            try {
                java.lang.reflect.Method m =
                        bdc.getDeclaredMethod("addDexPath", String.class, boolean.class);
                m.setAccessible(true);
                m.invoke(loader, path, false);
                return;
            } catch (NoSuchMethodException ignored) {
                // older ART
            }
            java.lang.reflect.Method m = bdc.getDeclaredMethod("addDexPath", String.class);
            m.setAccessible(true);
            m.invoke(loader, path);
        } catch (Throwable t) {
        }
    }

    /**
     * Huawei framework classes (SystemPropertiesEx, BuildEx, ...) live in the
     * boot classpath on Huawei devices. On Samsung they do not exist at all, so
     * ART's verifier aborts as soon as it resolves them. This hook answers
     * ClassLoader.loadClass() for com.huawei.* with the stub classes that are
     * packaged inside this module APK.
     */
    private static void installStubClassLoader() {
        final ClassLoader stubLoader = LegacyHooks.class.getClassLoader();
        XposedBridge.hookAllMethods(ClassLoader.class, "loadClass", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length < 1) {
                    return;
                }
                Object arg = param.args[0];
                if (!(arg instanceof String)) {
                    return;
                }
                String name = (String) arg;
                if (!STUB_CLASSES.contains(name) || param.thisObject == stubLoader) {
                    return;
                }
                if (Boolean.TRUE.equals(IN_STUB_LOAD.get())) {
                    return;
                }
                IN_STUB_LOAD.set(Boolean.TRUE);
                try {
                    Class<?> stub = stubLoader.loadClass(name);
                    param.setResult(stub);
                } catch (Throwable ignored) {
                    // fall through to normal resolution
                } finally {
                    IN_STUB_LOAD.remove();
                }
            }
        });
    }

    /** SplashActivity bails out with "not support this device" on non-Huawei builds. */
    private void spoofBuildFields() {
        String model = DEVICE_MODEL;
        String[][] fields = {
                {"MANUFACTURER", "HUAWEI"},
                {"BRAND", "HUAWEI"},
                {"MODEL", model},
                {"DEVICE", "HWDBY"},
                {"PRODUCT", model},
                {"HARDWARE", "kirin990"},
                {"BOARD", "KIRIN990"},
                {"SOC_MANUFACTURER", "HiSilicon"},
                {"SOC_MODEL", "Kirin 990"}
        };
        for (String[] pair : fields) {
            try {
                XposedHelpers.setStaticObjectField(android.os.Build.class, pair[0], pair[1]);
            } catch (Throwable t) {
            }
        }
    }

    private static void hookHomeActivity(ClassLoader cl) {
        try {
            Class<?> home = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.workmanage.HomeActivity", cl);
            XposedBridge.hookAllMethods(home, "f1", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(null);
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * Huawei framework-res contains resources that Samsung's framework-res lacks
     * (e.g. color 0x02050178). Missing lookups crash the app, so return a neutral
     * fallback instead of letting Resources$NotFoundException escape.
     */
    private static void hookResourceFallbacks() {
        hookColorFallback(android.content.Context.class, "getColor");
        hookColorFallback(android.content.res.Resources.class, "getColor");
        hookValueFallback(android.content.res.Resources.class, "getDimension", Float.valueOf(0f));
        hookValueFallback(android.content.res.Resources.class, "getDimensionPixelSize", Integer.valueOf(0));
        hookValueFallback(android.content.res.Resources.class, "getDimensionPixelOffset", Integer.valueOf(0));
        hookValueFallback(android.content.res.Resources.class, "getString", "");
        hookValueFallback(android.content.res.Resources.class, "getText", "");
        hookValueFallback(android.content.res.Resources.class, "getInteger", Integer.valueOf(0));
        hookValueFallback(android.content.res.Resources.class, "getBoolean", Boolean.FALSE);
        // A Huawei style can leave a TYPE_ATTRIBUTE value in a TypedArray.  On
        // Samsung, the framework resolver then throws before the view is built;
        // preserve inflation with a neutral value for that individual field.
        // Zero text sizes are restored by the TextView constructor/color hooks.
        hookValueFallback(android.content.res.TypedArray.class, "getDimension", Float.valueOf(0f));
        hookValueFallback(android.content.res.TypedArray.class, "getDimensionPixelSize", Integer.valueOf(0));
        hookValueFallback(android.content.res.TypedArray.class, "getDimensionPixelOffset", Integer.valueOf(0));
        try {
            XposedBridge.hookAllMethods(android.content.res.Resources.class, "getColorStateList",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null) return;
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                p.setResult(android.content.res.ColorStateList.valueOf(fallbackInk(p.thisObject)));
                            }
                        }
                    });
            XposedBridge.hookAllMethods(android.content.res.Resources.class, "getDrawable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null) return;
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                p.setResult(new android.graphics.drawable.ColorDrawable(0xFF000000));
                            }
                        }
                    });
        } catch (Throwable t) {
        }
    }

    /** LayoutInflater resolves attributes through Resources.getValue(); missing framework ids must not throw. */
    private static void hookTypedValueFallbacks() {
        try {
            XposedBridge.hookAllMethods(android.content.res.Resources.class, "getValue",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null) return;
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                Object idObj = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                                if (p.args != null && p.args.length > 1
                                        && p.args[1] instanceof android.util.TypedValue
                                        && idObj instanceof Integer) {
                                    android.util.TypedValue tv = (android.util.TypedValue) p.args[1];
                                    if (fillFromHuawei((android.content.res.Resources) p.thisObject,
                                            ((Integer) idObj).intValue(), tv, true)) {
                                        return;
                                    }
                                    tv.type = android.util.TypedValue.TYPE_NULL;
                                    tv.data = 0;
                                    tv.resourceId = 0;
                                    tv.string = null;
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
        }
        hookValueFallback(android.content.res.Resources.class, "getResourceName", "android:unknown/unknown");
        hookValueFallback(android.content.res.Resources.class, "getResourceTypeName", "string");
        hookValueFallback(android.content.res.Resources.class, "getResourceEntryName", "unknown");
    }

    /**
     * Latest's global standard-theme hook: every Activity gets the NoActionBar
     * theme before onCreate, and its ActionBar is hidden after onResume.
     */
    private static void hookStandardActivitiesTheme(ClassLoader cl) {
        try {
            XposedBridge.hookAllMethods(android.app.Activity.class, "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (!(p.thisObject instanceof android.app.Activity)) return;
                            android.app.Activity activity = (android.app.Activity) p.thisObject;
                            try {
                                activity.setTheme(android.R.style.Theme_DeviceDefault_NoActionBar);
                            } catch (Throwable t) {
                            }
                        }
                    });

            // The concrete onCreate may install its ActionBar after
            // super.onCreate() returns; onResume is late enough to hide it.
            XposedBridge.hookAllMethods(android.app.Activity.class, "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            if (!(p.thisObject instanceof android.app.Activity)) return;
                            android.app.Activity activity = (android.app.Activity) p.thisObject;
                            hideActionBar(activity, activity.getClass().getSimpleName());
                        }
                    });
        } catch (Throwable t) {
        }
    }

    private static void hideActionBar(android.app.Activity activity, String where) {
        try {
            android.app.ActionBar ab = activity.getActionBar();
            if (ab != null) {
                ab.hide();
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Huawei widgets dim themselves through androidhwext state lists. After the theme is
     * replaced those lists are gone, so a disabled button still looks fully opaque.
     * Restore the standard Android convention: disabled leaf widgets render at 40% alpha.
     */
    /** Keep the Huawei/AppCompat theme; only force the night mode, never replace the theme. */
    private static void hookDarkMode(ClassLoader cl) {
        try {
            Class<?> delegate = XposedHelpers.findClass(
                    "androidx.appcompat.app.AppCompatDelegate", cl);
            XposedBridge.hookAllMethods(delegate, "setDefaultNightMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length > 0) {
                        param.args[0] = Integer.valueOf(2); // MODE_NIGHT_YES
                    }
                }
            });
        } catch (Throwable t) {
        }
    }
    /** The app pins AppCompat to night mode; follow the system setting instead. */
    private static void hookNightMode(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClass(
                    "androidx.appcompat.app.AppCompatDelegate", cl);
            XposedBridge.hookAllMethods(c, "setDefaultNightMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args != null && p.args.length > 0) {
                        p.args[0] = Integer.valueOf(-1);
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    private static void hookDisabledAlpha() {
        try {
            XposedBridge.hookAllMethods(android.view.View.class, "setEnabled", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (!(p.thisObject instanceof android.view.View)) return;
                    android.view.View v = (android.view.View) p.thisObject;
                    if (v instanceof android.view.ViewGroup) return;
                    if (!(v instanceof android.widget.ImageView) && !(v instanceof android.widget.TextView)) {
                        return;
                    }
                    float alpha = v.getAlpha();
                    if (!v.isEnabled() && alpha > 0.99f) {
                        v.setAlpha(0.4f);
                    } else if (v.isEnabled() && Math.abs(alpha - 0.4f) < 0.01f) {
                        v.setAlpha(1.0f);
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * Every brush row inflates hover_ink_image / market_image / collect_image (the three
     * colored circles). Huawei hides them for local/system brushes via account/market
     * state that does not exist here, so they must be hidden explicitly.
     */
    private static void hookBrushRowIcons(ClassLoader cl) {
        try {
            Class<?> adapter = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.brush.adapter.BrushListAdapter", cl);
            XposedBridge.hookAllMethods(adapter, "onBindViewHolder", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.args == null || p.args.length < 1) return;
                    try {
                        Object holder = p.args[0];
                        android.view.View item = (android.view.View)
                                XposedHelpers.getObjectField(holder, "itemView");
                        if (item == null) return;
                        // Alpha3 restores the real Huawei SwipeLayout helper, so
                        // the action layer can stay visible and the native swipe
                        // gesture handles the reveal/rebound.
                        android.view.View actions = item.findViewById(0x7f0a01cf);
                        if (actions != null
                                && actions.getVisibility() != android.view.View.VISIBLE) {
                            actions.setVisibility(android.view.View.VISIBLE);
                        }
                    } catch (Throwable t) {
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * The app was compiled against an older Huawei SwipeListener that lacks
     * onStartClose; the newer featurelayer interface declares it abstract, so
     * dispatching it throws AbstractMethodError. Keep the native drag but drop
     * that event instead of crashing.
     */
    private static void hookSwipeDispatch(ClassLoader cl) {
        try {
            final Class<?> swipe = XposedHelpers.findClass(
                    "huawei.android.widget.SwipeLayout", cl);
            XposedBridge.hookAllMethods(swipe, "dispatchSwipeEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.method == null) return;
                    try {
                        p.setResult(XposedBridge.invokeOriginalMethod(
                                p.method, p.thisObject, p.args));
                    } catch (Throwable t) {
                        p.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Missing Huawei interpolators/animations would abort the brush list; use standard ones. */
    private static void hookAnimationFallback() {
        try {
            XposedBridge.hookAllMethods(android.view.animation.AnimationUtils.class,
                    "loadInterpolator", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null) return;
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(
                                        p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                p.setResult(new android.view.animation.LinearInterpolator());
                            }
                        }
                    });
            XposedBridge.hookAllMethods(android.view.animation.AnimationUtils.class,
                    "loadAnimation", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null) return;
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(
                                        p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                p.setResult(new android.view.animation.AlphaAnimation(1f, 1f));
                            }
                        }
                    });
        } catch (Throwable t) {
        }
    }

    /**
     * Last-resort standard fallback: if a view cannot be inflated because a Huawei
     * framework attribute is missing, substitute a plain Android widget.
     */
    private static void hookInflaterFallback() {
        try {
            XposedBridge.hookAllMethods(android.view.LayoutInflater.class, "createViewFromTag",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.method == null || p.args == null || p.args.length < 3) return;
                            if (!(p.args[1] instanceof String)
                                    || !(p.args[2] instanceof android.content.Context)) {
                                return;
                            }
                            String tag = (String) p.args[1];
                            android.content.Context ctx = (android.content.Context) p.args[2];
                            try {
                                p.setResult(XposedBridge.invokeOriginalMethod(
                                        p.method, p.thisObject, p.args));
                            } catch (Throwable t) {
                                Throwable cause = t.getCause() != null ? t.getCause() : t;
                                android.view.View v;
                                String declaredClass = "";
                                if (p.args.length > 3 && p.args[3] instanceof android.util.AttributeSet) {
                                    declaredClass = ((android.util.AttributeSet) p.args[3])
                                            .getAttributeValue(null, "class");
                                    if (declaredClass == null) declaredClass = "";
                                }
                                String widgetName = tag + " " + declaredClass;
                                if (widgetName.endsWith("TextView") || widgetName.contains("TextView")) {
                                    v = new android.widget.TextView(ctx);
                                } else if (widgetName.contains("ImageView")) {
                                    v = new android.widget.ImageView(ctx);
                                } else if (widgetName.contains("Button")) {
                                    v = new android.widget.Button(ctx);
                                } else {
                                    v = new android.widget.FrameLayout(ctx);
                                }
                                p.setResult(v);
                            }
                        }
                    });
        } catch (Throwable t) {
        }
    }

    /** The actual throw happens in ResourcesImpl.getValue(); soften it to a NULL TypedValue. */
    private static void hookResourcesImplGetValue() {
        try {
            Class<?> impl = Class.forName("android.content.res.ResourcesImpl");
            XposedBridge.hookAllMethods(impl, "getValue", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.method == null || p.args == null || p.args.length < 2
                            || !(p.args[1] instanceof android.util.TypedValue)) {
                        return;
                    }
                    try {
                        p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                    } catch (Throwable t) {
                        android.util.TypedValue tv = (android.util.TypedValue) p.args[1];
                        if (p.args[0] instanceof Integer
                                && fillFromHuawei(HW_BASE, ((Integer) p.args[0]).intValue(), tv, true)) {
                            return;
                        }
                        tv.type = android.util.TypedValue.TYPE_NULL;
                        tv.data = 0;
                        tv.resourceId = 0;
                        tv.string = null;
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * Huawei custom widgets read Huawei-only framework resources in their constructors.
     * Replace them with the closest standard Android widget while inflating.
     */
    private static void hookLayoutInflaterMapping() {
        try {
            XposedBridge.hookAllMethods(android.view.LayoutInflater.class, "createView",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (p.args == null || p.args.length < 3
                                    || !(p.args[0] instanceof String)) {
                                return;
                            }
                            String name = (String) p.args[0];
                            if (!name.startsWith("huawei.android.widget.")
                                    && !name.startsWith("androidhwext.")) {
                                return;
                            }
                            String mapped = mapToStandardWidget(name);
                            p.args[0] = mapped;
                        }
                    });
        } catch (Throwable t) {
        }
    }

    private static String mapToStandardWidget(String huaweiName) {
        String simple = huaweiName.substring(huaweiName.lastIndexOf('.') + 1);
        if (simple.equals("SwipeLayout")) {
            // Provided by our stub dex (extends FrameLayout); the app casts to this type.
            return "huawei.android.widget.SwipeLayout";
        }
        // Classes we ship as standard-Android-backed stubs must inflate as themselves,
        // because databinding classes cast the inflated view to the Huawei type.
        switch (simple) {
            case "ScrollView":
            case "ImageView":
            case "ListView":
            case "TabHost":
            case "TabWidget":
            case "VideoView":
            case "HwTextView":
            case "HwToolbar":
            case "TabIndicator":
            case "TimeAxisWidget":
                return "huawei.android.widget." + simple;
            default:
                break;
        }
        if (simple.endsWith("RecyclerView")) {
            return "android.widget.FrameLayout";
        }
        if (simple.endsWith("Toolbar")) {
            return "android.widget.Toolbar";
        }
        if (simple.endsWith("TextView") || simple.endsWith("EditText")) {
            return "android.widget.TextView";
        }
        if (simple.endsWith("ImageView")) {
            return "android.widget.ImageView";
        }
        if (simple.endsWith("Button")) {
            return "android.widget.Button";
        }
        if (simple.endsWith("LinearLayout")) {
            return "android.widget.LinearLayout";
        }
        if (simple.endsWith("RelativeLayout")) {
            return "android.widget.RelativeLayout";
        }
        if (simple.endsWith("FrameLayout")) {
            return "android.widget.FrameLayout";
        }
        if (simple.endsWith("ScrollView")) {
            return "android.widget.ScrollView";
        }
        if (simple.endsWith("ListView")) {
            return "android.widget.ListView";
        }
        return "android.widget.FrameLayout";
    }

    /** Huawei styles often resolve textColor to transparent on Samsung; restore ink. */
    private static void hookTextColors() {
        try {
            XposedBridge.hookAllMethods(android.widget.TextView.class, "setTextColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            if (!(p.thisObject instanceof android.widget.TextView)) return;
                            android.widget.TextView v = (android.widget.TextView) p.thisObject;
                            try {
                                int alpha = -1;
                                if (p.args != null && p.args.length > 0) {
                                    Object a = p.args[0];
                                    if (a instanceof Integer) {
                                        alpha = ((Integer) a).intValue() >>> 24;
                                    } else if (a instanceof android.content.res.ColorStateList) {
                                        alpha = ((android.content.res.ColorStateList) a)
                                                .getDefaultColor() >>> 24;
                                    }
                                }
                                if (alpha == 0) {
                                    v.setTextColor(fallbackInk(v.getContext()));
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
            XposedBridge.hookAllMethods(android.widget.TextView.class, "setTextSize",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            if (!(p.thisObject instanceof android.widget.TextView)) return;
                            android.widget.TextView v = (android.widget.TextView) p.thisObject;
                            try {
                                if (v.getTextSize() <= 0.5f) {
                                    v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15.5f);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
            XposedBridge.hookAllConstructors(android.widget.TextView.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (!(p.thisObject instanceof android.widget.TextView)) return;
                    android.widget.TextView v = (android.widget.TextView) p.thisObject;
                    try {
                        if (v.getTextSize() <= 0.5f) {
                            v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15.5f);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /**
     * Missing Huawei colors must stay readable on both light and dark themes:
     * pick a dark ink on day mode and a light ink on night mode.
     */
    private static int fallbackInk(Object thisObject) {
        int color = 0xFF1A1A1A;
        try {
            android.content.res.Resources res = null;
            if (thisObject instanceof android.content.res.Resources) {
                res = (android.content.res.Resources) thisObject;
            } else if (thisObject instanceof android.content.Context) {
                res = ((android.content.Context) thisObject).getResources();
            }
            if (res != null) {
                int mode = res.getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                if (mode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    color = 0xFFE6E6E6;
                }
            }
        } catch (Throwable ignored) {
        }
        return color;
    }

    private static void hookColorFallback(Class<?> cls, String name) {
        try {
            XposedBridge.hookAllMethods(cls, name, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.method == null) return;
                    try {
                        p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                    } catch (Throwable t) {
                        int color = 0xFF1A1A1A;
                        try {
                            android.content.res.Resources res = null;
                            if (p.thisObject instanceof android.content.res.Resources) {
                                res = (android.content.res.Resources) p.thisObject;
                            } else if (p.thisObject instanceof android.content.Context) {
                                res = ((android.content.Context) p.thisObject).getResources();
                            }
                            if (res != null) {
                                int mode = res.getConfiguration().uiMode
                                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                                if (mode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                                    color = 0xFFE6E6E6;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                        p.setResult(Integer.valueOf(color));
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    private static void hookValueFallback(Class<?> cls, String name, final Object fallback) {
        try {
            XposedBridge.hookAllMethods(cls, name, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.method == null) return;
                    try {
                        p.setResult(XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args));
                    } catch (Throwable t) {
                        p.setResult(fallback);
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Trace the consent page: does the tap reach the app at all? */
    private static void hookContract(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClass("com.huawei.hicreate.app.ContractActivity", cl);
            for (final String m : new String[]{"onClick", "onResume", "P", "G", "H", "finish"}) {
                XposedBridge.hookAllMethods(c, m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        StringBuilder sb = new StringBuilder();
                        if (param.args != null) {
                            for (Object a : param.args) {
                                sb.append('[').append(a).append(']');
                            }
                        }
                    }
                });
            }
        } catch (Throwable t) {
        }
    }

    /** Log every dialog/toast content so the "empty" alert box becomes readable. */
    private static void hookDialogs(ClassLoader cl) {
        try {
            Class<?> builder = XposedHelpers.findClass("android.app.AlertDialog$Builder", cl);
            for (final String m : new String[]{"setMessage", "setTitle"}) {
                XposedBridge.hookAllMethods(builder, m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object v = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                    }
                });
            }
            XposedBridge.hookAllMethods(android.app.Dialog.class, "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                }
            });
        } catch (Throwable t) {
        }
    }

    /** The app calls exit() between splash stages; keep the process alive for testing. */
    private static void hookExit() {
        XC_MethodHook swallow = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        };
        try {
            XposedBridge.hookAllMethods(java.lang.System.class, "exit", swallow);
            XposedBridge.hookAllMethods(java.lang.Runtime.class, "exit", swallow);
            XposedBridge.hookAllMethods(java.lang.Runtime.class, "halt", swallow);
            XposedBridge.hookAllMethods(android.os.Process.class, "killProcess", swallow);
        } catch (Throwable t) {
        }
    }

    /**
     * SplashActivity.onCreate calls M()Z; when it returns false the app logs
     * "SplashActivity - not support this device." and finishes.
     */
    private static void hookDeviceSupport(ClassLoader cl) {
        try {
            Class<?> splash = XposedHelpers.findClass(
                    "com.huawei.hicreate.app.SplashActivity", cl);
            XposedBridge.hookAllMethods(splash, "M", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(Boolean.TRUE);
                }
            });
        } catch (Throwable t) {
        }
    }

    /** Property spoof table (kept local: the module dex must not reference hwframework stubs). */
    private String spoofProp(String key) {
        if ("ro.build.version.emui".equals(key)) return "EmotionUI_14.2.0";
        if ("ro.build.version.emui.sdk".equals(key)) return "14";
        if ("ro.build.hw_emui_api_level".equals(key)) return "14";
        if ("ro.build.version.magic".equals(key)) return "0";
        if ("ro.product.manufacturer".equals(key)) return "HUAWEI";
        if ("ro.product.brand".equals(key)) return "HUAWEI";
        if ("ro.product.model".equals(key)) return DEVICE_MODEL;
        if ("ro.product.name".equals(key)) return DEVICE_MODEL;
        if ("ro.build.characteristics".equals(key)) return "tablet";
        if ("ro.config.hw_emui_version".equals(key)) return "14.2.0";
        if ("ro.board.platform".equals(key)) return "kirin990";
        if ("ro.hardware.chipname".equals(key)) return "KIRIN990";
        if ("ro.config.chipname".equals(key)) return "KIRIN990";
        if ("ro.product.board".equals(key)) return "KIRIN990";
        if ("ro.hardware".equals(key)) return "kirin990";
        return null;
    }

    /** Some Huawei checks read android.os.SystemProperties directly, not SystemPropertiesEx. */
    private void hookSystemProperties() {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            XposedBridge.hookAllMethods(sp, "get", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 1
                            || !(param.args[0] instanceof String)) {
                        return;
                    }
                    String v = spoofProp((String) param.args[0]);
                    if (v != null) {
                        param.setResult(v);
                    }
                }
            });
            XposedBridge.hookAllMethods(sp, "getInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2
                            || !(param.args[0] instanceof String)) {
                        return;
                    }
                    String v = spoofProp((String) param.args[0]);
                    if (v != null) {
                        try {
                            param.setResult(Integer.parseInt(v.trim()));
                        } catch (NumberFormatException ignored) {
                            // keep original
                        }
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    private static void hookPermissionChecks(ClassLoader cl) {
        String[] frameworkClasses = {
                "android.app.ContextImpl",
                "android.content.ContextWrapper",
                "android.app.Activity",
                "android.app.ApplicationPackageManager",
                "android.content.pm.PackageManager"
        };
        for (String name : frameworkClasses) {
            try {
                Class<?> c = XposedHelpers.findClass(name, cl);
                hookPerm(c, "checkPermission", 0, false);
                hookPerm(c, "checkCallingPermission", 0, false);
                hookPerm(c, "checkCallingOrSelfPermission", 0, false);
                hookPerm(c, "checkSelfPermission", 0, false);
                hookPerm(c, "enforcePermission", 0, true);
                hookPerm(c, "enforceCallingPermission", 0, true);
                hookPerm(c, "enforceCallingOrSelfPermission", 0, true);
            } catch (Throwable t) {
            }
        }

        // AndroidX helpers used by the app (permission is argument #1: Context, String).
        String[][] androidx = {
                {"androidx.core.content.PermissionChecker", "checkSelfPermission"},
                {"androidx.core.content.ContextCompat", "checkSelfPermission"},
                {"androidx.core.app.ActivityCompat", "checkSelfPermission"}
        };
        for (String[] pair : androidx) {
            try {
                Class<?> c = XposedHelpers.findClass(pair[0], cl);
                hookPerm(c, pair[1], 1, false);
            } catch (Throwable t) {
            }
        }
    }

    private static void hookPerm(Class<?> c, String method, int permIndex, boolean enforce) {
        try {
            XposedBridge.hookAllMethods(c, method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length <= permIndex) {
                        return;
                    }
                    Object v = param.args[permIndex];
                    if (v instanceof String && isHuaweiPermission((String) v)) {
                        param.setResult(enforce ? null : (Object) PackageManager.PERMISSION_GRANTED);
                    }
                }
            });
        } catch (Throwable t) {
        }
    }

    private static void hookAppOps() {
        try {
            Class<?> c = XposedHelpers.findClass("android.app.AppOpsManager", null);
            String[] methods = {"checkOp", "checkOpNoThrow", "noteOp", "noteOpNoThrow",
                    "unsafeCheckOp", "unsafeCheckOpNoThrow"};
            for (String m : methods) {
                XposedBridge.hookAllMethods(c, m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args.length > 0
                                && "android:manage_external_storage".equals(param.args[0])) {
                            param.setResult(AppOpsManager.MODE_ALLOWED);
                        }
                    }
                });
            }
        } catch (Throwable t) {
        }

        try {
            XposedBridge.hookAllMethods(Environment.class, "isExternalStorageManager",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(Boolean.TRUE);
                        }
                    });
        } catch (Throwable t) {
        }
    }

    private static void hookStartupBlockers(ClassLoader cl) {
        try {
            Class<?> beu = XposedHelpers.findClass(
                    "com.huawei.hicreate.common.utils.ui.resource.BuildExUtil", cl);
            XposedBridge.hookAllMethods(beu, "a", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(Boolean.FALSE);
                }
            });
        } catch (Throwable t) {
        }

        // Logger's device/EMUI probes dereference SystemPropertiesEx; force the
        // boolean probes to false so static initialisation can complete.
        try {
            Class<?> logger = XposedHelpers.findClass("com.huawei.hicreate.common.utils.Logger", cl);
            for (String m : new String[]{"q", "r", "s", "a", "c"}) {
                XposedBridge.hookAllMethods(logger, m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(Boolean.FALSE);
                    }
                });
            }
        } catch (Throwable t) {
        }
    }
}











































