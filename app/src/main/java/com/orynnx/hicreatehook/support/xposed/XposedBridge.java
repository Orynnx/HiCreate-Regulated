package com.orynnx.hicreatehook.support.xposed;

import com.orynnx.hicreatehook.runtime.HookContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

/** Static facade used by the ported compatibility hooks. */
public final class XposedBridge {

    private static volatile HookContext context;
    private static final ThreadLocal<XposedInterface.Chain> CURRENT_CHAIN = new ThreadLocal<>();

    private XposedBridge() {
    }

    public static void init(HookContext hookContext) {
        context = hookContext;
    }

    public static HookContext context() {
        return context;
    }

    public static ClassLoader moduleLoader() {
        return XposedBridge.class.getClassLoader();
    }

    public static void hookAllMethods(Class<?> cls, String name, XC_MethodHook callback) {
        HookContext ctx = context;
        if (ctx == null) {
            return;
        }
        List<Method> methods = methodsNamed(cls, name);
        if (methods.isEmpty()) {
            return;
        }
        for (Method method : methods) {
            try {
                ctx.hook(method, callback);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void hookAllConstructors(Class<?> cls, XC_MethodHook callback) {
        HookContext ctx = context;
        if (ctx == null) {
            return;
        }
        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            try {
                constructor.setAccessible(true);
                ctx.hook(constructor, callback);
            } catch (Throwable ignored) {
            }
        }
    }

    public static Object invokeOriginalMethod(Method method, Object thisObject, Object[] args)
            throws Throwable {
        XposedInterface.Chain chain = CURRENT_CHAIN.get();
        if (chain != null && chain.getExecutable().equals(method)) {
            return chain.proceed(args == null ? new Object[0] : args);
        }
        try {
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    static void setCurrentChain(XposedInterface.Chain chain) {
        CURRENT_CHAIN.set(chain);
    }

    static void clearCurrentChain() {
        CURRENT_CHAIN.remove();
    }

    private static List<Method> methodsNamed(Class<?> cls, String name) {
        List<Method> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Class<?> current = cls; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                // Walking superclasses also returns the abstract declaration
                // (e.g. Context.checkPermission) after the concrete override
                // (ContextWrapper/ContextImpl). LSPosed cannot hook a method
                // without a body, so keep the concrete override and skip the
                // abstract base.
                if (Modifier.isAbstract(method.getModifiers())) continue;
                String key = method.getName()
                        + Arrays.toString(method.getParameterTypes());
                if (!seen.add(key)) continue;
                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                }
                result.add(method);
            }
        }
        return result;
    }
}
