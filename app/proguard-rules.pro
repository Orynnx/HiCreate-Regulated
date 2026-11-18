# Keep the Xposed entry point readable by the framework and by libxposed.
-keep class com.orynnx.hicreatehook.entry.** { *; }
-keep class * extends io.github.libxposed.api.XposedModule { *; }

# These execute inside ClassLoader.loadClass hooks. Merging their types into
# lazily initialized UI classes can reenter the loader before installation ends.
-keep class com.orynnx.hicreatehook.support.xposed.** { *; }
-keep class com.orynnx.hicreatehook.runtime.** { *; }
