package com.huawei.android.os;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub for Huawei's framework helper (boot classpath: hwframework.jar).
 * Reports EMUI/Huawei values so device-support checks pass on non-Huawei hardware.
 */
public final class SystemPropertiesEx {
    private static final Map<String, String> PROPS = new HashMap<>();

    static {
        PROPS.put("ro.build.version.emui", "EmotionUI_14.2.0");
        PROPS.put("ro.build.version.emui.sdk", "14");
        PROPS.put("ro.build.hw_emui_api_level", "14");
        PROPS.put("ro.build.version.magic", "0");
        PROPS.put("ro.product.manufacturer", "HUAWEI");
        PROPS.put("ro.product.brand", "HUAWEI");
        PROPS.put("ro.product.model", "BKY-W10");
        PROPS.put("ro.product.name", "BKY-W10");
        PROPS.put("ro.build.characteristics", "tablet");
        PROPS.put("ro.build.hw_emui_lcd_ratio", "1.0");
        PROPS.put("ro.config.hw_emui_version", "14.2.0");
        PROPS.put("ro.board.platform", "kirin990");
        PROPS.put("ro.hardware.chipname", "KIRIN990");
        PROPS.put("ro.config.chipname", "KIRIN990");
        PROPS.put("ro.product.board", "KIRIN990");
        PROPS.put("ro.hardware", "kirin990");
    }

    private SystemPropertiesEx() {}

    public static String get(String key) {
        String live = liveGet(key);
        return live != null ? live : PROPS.get(key);
    }

    public static String get(String key, String def) {
        String v = liveGet(key);
        if (v == null) v = PROPS.get(key);
        return v != null ? v : def;
    }

    private static String liveGet(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = sp.getMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, null);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getInt(String key, int def) {
        String v = PROPS.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static boolean getBoolean(String key, boolean def) {
        String v = PROPS.get(key);
        if (v == null) return def;
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    public static long getLong(String key, long def) {
        String v = PROPS.get(key);
        if (v == null) return def;
        try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static void set(String key, String value) { PROPS.put(key, value); }
}


