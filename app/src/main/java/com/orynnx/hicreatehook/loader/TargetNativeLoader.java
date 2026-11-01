package com.orynnx.hicreatehook.loader;

/**
 * Loaded through a DexClassLoader whose parent is the target app loader. This
 * makes Runtime.nativeLoad resolve JNI_OnLoad's FindClass calls against the
 * target app while still letting the loader find this tiny class in StubDex.
 */
public final class TargetNativeLoader {

    private TargetNativeLoader() {
    }

    public static void load(String absolutePath) {
        System.load(absolutePath);
    }
}
