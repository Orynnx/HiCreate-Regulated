package com.orynnx.hicreatehook.runtime;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;

/** Per-process hook installation context. */
public final class HookContext {

    private final XposedInterface xposed;
    private final ClassLoader classLoader;
    private final HookRegistry registry;
    private final String processName;
    private final String bootId;

    public HookContext(XposedInterface xposed, ClassLoader classLoader,
            String processName, String bootId) {
        this.xposed = xposed;
        this.classLoader = classLoader;
        this.processName = processName;
        this.bootId = bootId;
        this.registry = new HookRegistry(xposed);
    }

    public XposedInterface xposed() {
        return xposed;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public HookRegistry registry() {
        return registry;
    }

    public String processName() {
        return processName;
    }

    public String bootId() {
        return bootId;
    }

    public HookRegistry.Handle hook(Executable executable, XposedInterface.Hooker hooker) {
        return registry.hook(executable, hooker);
    }
}
