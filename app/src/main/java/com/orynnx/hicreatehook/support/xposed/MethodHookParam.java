package com.orynnx.hicreatehook.support.xposed;

import java.lang.reflect.Method;

/** Minimal, classloader-neutral equivalent of Xposed's MethodHookParam. */
public final class MethodHookParam {

    public Method method;
    public Object thisObject;
    public Object[] args;

    Object result;
    boolean resultSet;
    Throwable throwable;

    MethodHookParam(Method method, Object thisObject, Object[] args) {
        this.method = method;
        this.thisObject = thisObject;
        this.args = args;
    }
    public Object getResult() {
        return result;
    }

    public void setResult(Object value) {
        result = value;
        resultSet = true;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean hasThrowable() {
        return throwable != null;
    }
}
