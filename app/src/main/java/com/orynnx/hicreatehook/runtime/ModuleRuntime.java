package com.orynnx.hicreatehook.runtime;

import com.orynnx.hicreatehook.features.LegacyHooks;
import com.orynnx.hicreatehook.support.nativepatch.NativeCompat;
import com.orynnx.hicreatehook.support.xposed.XposedBridge;

import java.util.List;

import io.github.libxposed.api.XposedInterface;

/** Owns the per-process hook generation and hot-reload replacement. */
public final class ModuleRuntime {

    private final XposedInterface xposed;
    private final String bootId;
    private final String packageName;
    private HookContext context;
    private LegacyHooks hooks;

    public ModuleRuntime(XposedInterface xposed, String bootId, String packageName) {
        this.xposed = xposed;
        this.bootId = bootId;
        this.packageName = packageName;
    }

    public void install(ClassLoader classLoader) {
        installGeneration(classLoader);
    }

    public void hotReload(ClassLoader classLoader, List<XposedInterface.HookHandle> oldHandles) {
        installGeneration(classLoader);
        // Application.onCreate has already run; API 102 does not replay package
        // callbacks, so reinstall the late hooks before retiring old handles.
        installLateIfReady(classLoader);
        if (context != null) {
            context.registry().retireOld(oldHandles);
        }
    }

    public void prepareHotReload() {
        // No external state to release; hooks stay owned by the framework.
    }

    public void close() {
        if (context != null) {
            context.registry().close();
        }
    }

    public HookContext context() {
        return context;
    }

    private void installGeneration(ClassLoader classLoader) {
        if (classLoader == null) return;
        HookContext next = new HookContext(xposed, classLoader, packageName, bootId);
        XposedBridge.init(next);
        context = next;
        hooks = new LegacyHooks(next);
        hooks.install();
    }

    private void installLateIfReady(ClassLoader classLoader) {
        try {
            Object app = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app instanceof android.content.Context) {
                NativeCompat.resetForHotReload();
                hooks.installLate((android.content.Context) app, classLoader);
            }
        } catch (Throwable ignored) {
        }
    }
}
