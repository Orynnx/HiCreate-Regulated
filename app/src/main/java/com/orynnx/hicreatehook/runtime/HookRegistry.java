package com.orynnx.hicreatehook.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

/** Stable-id hook registry used by the ported compatibility layer. */
public final class HookRegistry {

    public static final class Handle {
        private final String id;
        private final Executable executable;
        private final XposedInterface.HookHandle delegate;

        private Handle(String id, Executable executable, XposedInterface.HookHandle delegate) {
            this.id = id;
            this.executable = executable;
            this.delegate = delegate;
        }

        public String id() {
            return id;
        }

        public Executable executable() {
            return executable;
        }

        public void unhook() {
            try {
                delegate.unhook();
            } catch (Throwable ignored) {
            }
        }
    }

    private final XposedInterface xposed;
    private final Map<String, Handle> handles = new LinkedHashMap<>();
    private final Set<String> attempted = new LinkedHashSet<>();

    public HookRegistry(XposedInterface xposed) {
        this.xposed = xposed;
    }

    public synchronized Handle hook(Executable executable, XposedInterface.Hooker hooker) {
        String base = baseId(executable);
        String id = base;
        int suffix = 1;
        while (handles.containsKey(id)) {
            id = base + "#" + suffix++;
        }
        // Record the attempt before intercept(): if installation fails, the old
        // handle for this id must remain active.
        attempted.add(id);
        XposedInterface.HookHandle delegate = xposed.hook(executable).setId(id).intercept(hooker);
        Handle handle = new Handle(id, executable, delegate);
        handles.put(id, handle);
        return handle;
    }

    /** Unhook old-generation handles that the new generation did not replace. */
    public synchronized void retireOld(List<XposedInterface.HookHandle> oldHandles) {
        if (oldHandles == null || oldHandles.isEmpty()) return;
        List<XposedInterface.HookHandle> stale = new ArrayList<>();
        for (XposedInterface.HookHandle old : oldHandles) {
            if (old == null) continue;
            if (!attempted.contains(old.getId()) && !handles.containsKey(old.getId())) {
                stale.add(old);
            }
        }
        for (XposedInterface.HookHandle old : stale) {
            try {
                old.unhook();
            } catch (Throwable ignored) {
            }
        }
    }

    public synchronized void close() {
        for (Handle handle : handles.values()) {
            handle.unhook();
        }
        handles.clear();
        attempted.clear();
    }

    public synchronized int size() {
        return handles.size();
    }

    private static String baseId(Executable executable) {
        StringBuilder sb = new StringBuilder();
        sb.append(executable.getDeclaringClass().getName())
                .append('#')
                .append(executable instanceof Method ? executable.getName() : "<init>")
                .append('(');
        Class<?>[] params = executable.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(params[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }
}
