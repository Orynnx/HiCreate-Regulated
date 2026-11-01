package com.huawei.android.os;

import android.os.Process;
import android.os.UserHandle;

/** Stub for Huawei UserHandle extensions (hwframework.jar). */
public final class UserHandleEx {
    private UserHandleEx() {}

    public static final UserHandle OWNER = Process.myUserHandle();
}
