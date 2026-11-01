package com.orynnx.hicreatehook.support.nativepatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/** The single native library we patch, plus the app libraries it needs. */
public final class NativePatch {

    private NativePatch() {
    }

    public static final String NATIVELIB = "libnativelib.so";

    public static final String[] LIBRARIES = {
            "libnativelib.so",
            "libsec.so",
            "librenderengine.so",
            "libhicreatesqlite.so",
            "libyuv.so",
            "libopencv_imgcodecs.so",
            "libopencv_imgproc.so",
            "libopencv_core.so",
            "liblibpng.so",
            "libminizip.so",
            "libavcodec.so",
            "libavfilter.so",
            "libavformat.so",
            "libavutil.so",
            "libswscale.so",
            "libswresample.so"
    };

    /** Load order for the shared libraries; libnativelib comes last. */
    public static final String[] LOAD_ORDER = {
            "libavutil.so", "libavcodec.so", "libswresample.so", "libswscale.so",
            "libavformat.so", "libavfilter.so",
            "libopencv_core.so", "libopencv_imgproc.so", "libopencv_imgcodecs.so",
            "libhicreatesqlite.so", "liblibpng.so", "libminizip.so",
            "librenderengine.so", "libsec.so", "libyuv.so"
    };

    public static final String ORIGINAL_SHA256 =
            "3CE153F4901CD40B2B3D164B0D345997F688E9AB329B5498CC3BC1D16A285E48";
    public static final String PATCHED_SHA256 =
            "B505EEA91BB28C171CCA626A544E9EE1010DC40900B0FC7EBAC76B6161934725";

    /**
     * 37 patches: the original 10 support/signature/exit fixes, then the
     * filter gates.
     *
     * <p>Nine heavy filters each arm a three-second {@code _Exit(0)} from four
     * places in {@code Init}. Two of those checks compare a device string
     * (brand / characteristics / chipset) with the value the filter expects;
     * a third is an API-level check. Both are forced to continue at the next
     * instruction so the timer is never armed. The strings, the level value,
     * the memory gate and {@code _Exit} itself stay untouched.</p>
     */
    public static final long[] OFFSETS = {
            0xB7F5F8L, 0xB7F9A4L, 0x91ACD4L, 0xB810D4L,
            0x581F78L, 0x5822F0L, 0x582518L, 0x582754L,
            0xB23544L, 0x6DE470L,
            0x9B7308L, 0x9BC128L, 0x9C7630L, 0x9CDC00L, 0x9D344CL,
            0x9DC2E0L, 0x9E43C0L, 0x9EED38L, 0x9F25FCL,
            0x9B7124L, 0x9B72BCL, 0x9BBF44L, 0x9BC0DCL, 0x9C7448L,
            0x9C75E0L, 0x9CDA1CL, 0x9CDBB4L, 0x9D3268L, 0x9D3400L,
            0x9DC0F8L, 0x9DC290L, 0x9E41D8L, 0x9E4370L, 0x9EEB50L,
            0x9EECE8L, 0x9F2418L, 0x9F25B0L
    };

    public static final String[] OLD_BYTES = {
            "fd7bbfa9fd030091",
            "fd7bbea9f30b00f9",
            "ff0301d1fd7b02a9",
            "ff0301d1fd7b02a9",
            "fd7bbda9f65701a9",
            "ff8301d1fd7b02a9",
            "ff8301d1fd7b02a9",
            "ffc301d1fd7b03a9",
            "e0031f2aca4c0394f30300aae0230091",
            "e0031f2aff601494f30300aae0230091",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "4c000054",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "58000037",
            "58000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037",
            "57000037"
    };

    public static final String[] NEW_BYTES = {
            "20008052c0035fd6",
            "20008052c0035fd6",
            "e00300aac0035fd6",
            "e00300aac0035fd6",
            "20008052c0035fd6",
            "20008052c0035fd6",
            "20008052c0035fd6",
            "20008052c0035fd6",
            "f44f43a9fd7b42a9ff030191c0035fd6",
            "f44f43a9fd7b42a9ff030191c0035fd6",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014",
            "02000014"
    };

    public static void apply(File file) throws Exception {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
            for (int i = 0; i < OFFSETS.length; i++) {
                byte[] oldBytes = hex(OLD_BYTES[i]);
                byte[] newBytes = hex(NEW_BYTES[i]);
                byte[] actual = new byte[oldBytes.length];
                raf.seek(OFFSETS[i]);
                raf.readFully(actual);
                if (!java.util.Arrays.equals(actual, oldBytes)) {
                    throw new IllegalStateException("unexpected bytes at 0x"
                            + Long.toHexString(OFFSETS[i]));
                }
                raf.seek(OFFSETS[i]);
                raf.write(newBytes);
            }
        }
    }

    public static Map<String, String> hashLibraries(File dir) throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        for (String name : LIBRARIES) {
            hashes.put(name, sha256(new File(dir, name)));
        }
        return hashes;
    }

    public static String sha256(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return sha256(in);
        }
    }

    public static String sha256(InputStream in) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[1 << 16];
        int read;
        while ((read = in.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        StringBuilder out = new StringBuilder(64);
        for (byte b : digest.digest()) {
            out.append(String.format("%02x", b));
        }
        return out.toString().toUpperCase(java.util.Locale.US);
    }

    public static void copy(InputStream in, File out) throws Exception {
        try (FileOutputStream os = new FileOutputStream(out)) {
            byte[] buffer = new byte[1 << 16];
            int read;
            while ((read = in.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        }
    }

    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}

