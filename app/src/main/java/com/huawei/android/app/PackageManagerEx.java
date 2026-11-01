package com.huawei.android.app;

import android.content.pm.FeatureInfo;

/** Stub for Huawei PackageManager extensions (hwframework.jar). */
public final class PackageManagerEx {
    private PackageManagerEx() {}

    public static boolean hasHwSystemFeature(String name) { return false; }
    public static FeatureInfo[] getHwSystemAvailableFeatures() { return new FeatureInfo[0]; }
}
