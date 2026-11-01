package com.orynnx.hicreatehook.entry;

import android.content.Context;

import com.orynnx.hicreatehook.runtime.ModuleRuntime;

import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * libxposed API 102 entry point.
 *
 * <p>The module is intentionally zero-configuration: every supported hook is
 * installed, and no preference or log is read or written.</p>
 */
public final class RegulatedModule extends XposedModule {

    private static final String TARGET_PACKAGE = "com.huawei.hicreate";
    private static final String HMS_PACKAGE = "com.huawei.hwid";

    private String bootId = "00000000";
    private String processName = "";
    private ModuleRuntime runtime;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        bootId = Integer.toHexString((int) (System.nanoTime() & 0xFFFFFFFFL));
        processName = param.getProcessName();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!TARGET_PACKAGE.equals(pkg) && !HMS_PACKAGE.equals(pkg)) {
            return;
        }
        if (runtime == null) {
            runtime = new ModuleRuntime(this, bootId, pkg);
            runtime.install(param.getClassLoader());
        }
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        if (runtime != null) {
            runtime.prepareHotReload();
        }
        // Only classloader-neutral state may cross the generation boundary.
        param.setSavedInstanceState(bootId);
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        List<XposedInterface.HookHandle> oldHooks = param.getOldHookHandles();
        Object saved = param.getSavedInstanceState();
        if (saved instanceof String) {
            bootId = (String) saved;
        }
        ClassLoader classLoader = resolveClassLoader();
        if (classLoader == null) {
            return;
        }
        runtime = new ModuleRuntime(this, bootId, param.getProcessName());
        runtime.hotReload(classLoader, oldHooks);
    }

    private ClassLoader resolveClassLoader() {
        try {
            Object app = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app instanceof Context) {
                return ((Context) app).getClassLoader();
            }
        } catch (Throwable ignored) {
            // Fall back to the current thread.
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : RegulatedModule.class.getClassLoader();
    }
}
