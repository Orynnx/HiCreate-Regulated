package com.huawei.android.app;

import android.app.Activity;
import android.os.Bundle;

/** Stub for Huawei ActivityManager extensions (hwframework.jar). */
public final class ActivityManagerEx {
    private ActivityManagerEx() {}

    public static int getActivityWindowMode(Activity activity) { return 0; }
    public static Bundle getHwMultiWindowState() { return null; }
}
