package com.orynnx.hicreatehook.support.xposed;

import io.github.libxposed.api.XposedInterface;

/** Bridge between the familiar before/after style and the API 102 Chain style. */
public abstract class XC_MethodHook implements XposedInterface.Hooker {

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    @Override
    public final Object intercept(XposedInterface.Chain chain) throws Throwable {
        java.lang.reflect.Executable executable = chain.getExecutable();
        java.lang.reflect.Method method = executable instanceof java.lang.reflect.Method
                ? (java.lang.reflect.Method) executable
                : null;
        MethodHookParam param = new MethodHookParam(
                method,
                chain.getThisObject(),
                chain.getArgs().toArray());
        XposedBridge.setCurrentChain(chain);
        try {
            try {
                beforeHookedMethod(param);
            } catch (Throwable ignored) {
            }
            if (param.resultSet) {
                return param.result;
            }
            Object result;
            try {
                result = chain.proceed(param.args);
            } catch (Throwable t) {
                param.throwable = t;
                throw t;
            }
            param.result = result;
            param.resultSet = true;
            try {
                afterHookedMethod(param);
            } catch (Throwable ignored) {
            }
            return param.result;
        } finally {
            XposedBridge.clearCurrentChain();
        }
    }
}
