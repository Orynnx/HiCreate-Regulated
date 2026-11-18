package com.orynnx.hicreatehook.support.xposed;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

import static org.junit.Assert.*;

/** Regression coverage for nested resource hooks and void-method recovery. */
public class HookChainTest {
    public static String outer() { return "reflection"; }
    public static String inner() { return "inner-reflection"; }
    public static void missingResource() { throw new IllegalStateException("missing"); }

    private interface Original { Object call() throws Throwable; }

    private static XposedInterface.Chain chain(Method method, Original original) {
        return (XposedInterface.Chain) Proxy.newProxyInstance(
                XposedInterface.Chain.class.getClassLoader(),
                new Class<?>[]{XposedInterface.Chain.class}, (proxy, called, args) -> {
                    switch (called.getName()) {
                        case "getExecutable": return method;
                        case "getThisObject": return null;
                        case "getArgs": return Collections.emptyList();
                        case "proceed": return original.call();
                        default: throw new UnsupportedOperationException(called.getName());
                    }
                });
    }

    @Test
    public void nestedHookRestoresEnclosingOriginalAndClearsAfterExit() throws Throwable {
        checkNestedRestoration(false);
    }

    @Test
    public void nestedFailureRestoresEnclosingOriginalAndClearsAfterExit() throws Throwable {
        checkNestedRestoration(true);
    }

    private static void checkNestedRestoration(boolean failInner) throws Throwable {
        Method outerMethod = HookChainTest.class.getMethod("outer");
        Method innerMethod = HookChainTest.class.getMethod("inner");
        AtomicInteger outerCalls = new AtomicInteger();
        AtomicInteger innerCalls = new AtomicInteger();
        XC_MethodHook innerHook = new XC_MethodHook() {};
        XposedInterface.Chain innerChain = chain(innerMethod, () -> {
            innerCalls.incrementAndGet();
            if (failInner) throw new IllegalStateException("inner failure");
            return "inner-chain";
        });
        XC_MethodHook outerHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    innerHook.intercept(innerChain);
                } catch (IllegalStateException expected) {
                    if (!failInner) throw expected;
                }
                param.setResult(XposedBridge.invokeOriginalMethod(param.method, null, param.args));
            }
        };
        Object result = outerHook.intercept(chain(outerMethod, () -> {
            outerCalls.incrementAndGet();
            return "outer-chain";
        }));
        assertEquals("outer-chain", result);
        assertEquals(1, outerCalls.get());
        assertEquals(1, innerCalls.get());
        // Once the outer hook has exited, no stale chain may remain on this thread.
        assertEquals("reflection", XposedBridge.invokeOriginalMethod(outerMethod, null, null));
        assertEquals(1, outerCalls.get());
    }

    @Test
    public void recoveredVoidOriginalIsNotExecutedAgain() throws Throwable {
        AtomicInteger attempts = new AtomicInteger();
        Method method = HookChainTest.class.getMethod("missingResource");
        XC_MethodHook recoveringHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    XposedBridge.invokeOriginalMethod(param.method, null, param.args);
                } catch (IllegalStateException expected) {
                    // Mirrors a successful out-parameter resource fallback.
                    param.setResult(null);
                }
            }
        };
        assertNull(recoveringHook.intercept(chain(method, () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("missing");
        })));
        assertEquals(1, attempts.get());
    }

    @Test
    public void failedOuterOriginalDoesNotLeakChain() throws Throwable {
        Method method = HookChainTest.class.getMethod("outer");
        IllegalStateException originalFailure = new IllegalStateException("original failure");
        try {
            new XC_MethodHook() {}.intercept(chain(method, () -> { throw originalFailure; }));
            fail("Expected original failure");
        } catch (IllegalStateException actual) {
            assertSame(originalFailure, actual);
        }
        assertEquals("reflection", XposedBridge.invokeOriginalMethod(method, null, null));
    }
}
