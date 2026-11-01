package com.huawei.android.view;

import android.view.WindowManager;

/** Stub for Huawei WindowManager extensions (hwframework.jar). */
public final class WindowManagerEx {
    private WindowManagerEx() {}

    public static final class LayoutParamsEx {
        public LayoutParamsEx(WindowManager.LayoutParams params) { }

        public void addPrivateFlags(int flags) { }
        public int getPrivateFlagShowForAllUsers() { return 0; }
    }
}
