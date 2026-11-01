# Keep the Xposed entry point readable by the framework and by libxposed.
-keep class com.orynnx.hicreatehook.entry.** { *; }
-keep class * extends io.github.libxposed.api.XposedModule { *; }
