package com.huawei.android.os;

/** Stub for Huawei's framework helper (boot classpath: hwframework.jar). */
public final class BuildEx {
    private BuildEx() {}

    public static String EMUI_VERSION = "EmotionUI_14.2.0";
    public static int EMUI_SDK_INT = 14;

    public static final class VERSION {
        public static int EMUI_SDK_INT = 14;
        public static String EMUI_VERSION = "EmotionUI_14.2.0";
    }

    public static String getEMUIVersion() { return EMUI_VERSION; }
    public static int getEMUISdkInt() { return EMUI_SDK_INT; }
}
