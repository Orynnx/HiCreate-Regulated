# 原生兼容（libnativelib.so）

目标 APK 使用 `android:extractNativeLibs="false"`，ARM64 库直接从
`base.apk!/lib/arm64-v8a/` 加载，应用目录下没有可替换的已解压副本。模块不修改
APK，也不写 `/data/app`。

模块在目标进程内创建一份应用私有副本，校验、打补丁，然后把
`System.loadLibrary("nativelib")` 重定向到副本。

> 这仍然要求模块运行在 LSPosed 中；常规 LSPosed 需要 root 和 Zygisk/同等
> 注入环境。原生补丁本身不额外申请 root 权限。

## 流程

```text
HiCreateApplication.onCreate
  └─ StubDex 注入完成
      └─ NativeCompat.install(context, appLoader)
          └─ Hook System.loadLibrary("nativelib")
              ├─ files/native 缓存有效 -> 加载缓存副本
              └─ 缓存失效
                  ├─ 从目标 APK 复制 16 个 ARM64 库到 files/native.tmp
                  ├─ 校验 libnativelib.so SHA-256 == 原版哈希
                  ├─ 打 37 处补丁
                  ├─ 校验补丁后 SHA-256 == 预期哈希
                  ├─ 写入 manifest.json（来源和补丁后哈希）
                  ├─ 原子替换 files/native
                  └─ 按依赖顺序加载，再加载补丁后的库
```

复制、哈希或补丁任一步失败都会删除临时目录并回退到原版
`System.loadLibrary`，不会把未知库当作可打补丁目标。

## 哈希门控

```text
原版 libnativelib.so
3CE153F4901CD40B2B3D164B0D345997F688E9AB329B5498CC3BC1D16A285E48

补丁后 libnativelib.so
B505EEA91BB28C171CCA626A544E9EE1010DC40900B0FC7EBAC76B6161934725
```

只支持原版 APK。已经包含补丁后 `libnativelib.so` 的重打包 APK 不属于支持输入。

## 补丁表

共 37 项：

- 10 个基础兼容补丁；
- 18 个设备字符串门；
- 9 个 API/等级门。

这些补丁用于避免相关滤镜进入三秒 `_Exit(0)` 路径，其余原版行为保持不变。

## 缓存

`files/native/manifest.json` 记录来源库哈希和补丁后哈希。下次启动时重新校验
缓存库与目标 APK 中的来源库；哈希或补丁表变化会使缓存失效并重建。

## 失败行为

- 原版哈希不匹配：不改动，回退原版加载。
- 补丁后哈希不匹配：删除临时副本，回退。
- 复制或权限失败：删除临时副本，回退。
- 目标应用更新导致库哈希变化：回退，等待更新补丁表和哈希门控。
- 不写模块日志或 `report.json`；失败不会产生模块侧诊断文件。
