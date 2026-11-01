package com.orynnx.hicreatehook.support.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection helpers matching the small XposedHelpers surface used by the port. */
public final class XposedHelpers {

    private XposedHelpers() {
    }

    public static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException first) {
            if (loader != XposedBridge.moduleLoader()) {
                try {
                    return Class.forName(name, false, XposedBridge.moduleLoader());
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw first;
        }
    }

    public static Object callMethod(Object object, String name, Object... args) throws Throwable {
        Class<?> cls = object.getClass();
        for (Class<?> current = cls; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (method.getParameterTypes().length != args.length) continue;
                try {
                    method.setAccessible(true);
                    return method.invoke(object, args);
                } catch (IllegalArgumentException ignored) {
                    // Try another overload.
                }
            }
        }
        throw new NoSuchMethodException(cls.getName() + "#" + name);
    }

    public static Object getObjectField(Object object, String name) throws Throwable {
        Class<?> current = object.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static void setObjectField(Object object, String name, Object value) throws Throwable {
        Class<?> current = object.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static void setStaticObjectField(Class<?> cls, String name, Object value)
            throws Throwable {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
