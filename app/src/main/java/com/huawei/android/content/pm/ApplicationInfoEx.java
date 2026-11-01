package com.huawei.android.content.pm;

import android.content.pm.ApplicationInfo;

/** Stub for Huawei ApplicationInfo extensions (hwframework.jar). */
public final class ApplicationInfoEx {
    private final ApplicationInfo mInfo;

    public ApplicationInfoEx(ApplicationInfo info) { mInfo = info; }

    public int getHwFlags() { return 0; }

    public static int[] getSplitVersionCodes(ApplicationInfo info) { return new int[0]; }
}
