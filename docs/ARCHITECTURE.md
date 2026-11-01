# 架构

## 原则

1. 原版目标 APK 永不修改、重签或替换。模块自身不写 `/data/app`，不启动
   root 守护进程，也不做持久化挂载。
2. 完整运行依赖 LSPosed API 102。常规 LSPosed 需要已 root 的设备以及
   Zygisk/同等注入环境；本模块**不是免 root 方案**。
3. 兼容行为自动安装。唯一的用户偏好是状态页外观开关（Miuix / Apple 风格
   Glasmio），不改变任何 Hook 行为。
4. 不写模块日志：不调用 `android.util.Log`，不调用 `XposedInterface.log`，
   不生成 `report.json`。
5. 原生兼容只操作目标应用私有目录中的副本，在补丁前后都做 SHA-256 校验。
6. 状态页除外观开关外只读，页面恢复时自动重新自检。

## 分层

```text
entry/RegulatedModule       API 102 生命周期；目标包与 HMS 包路由
runtime/ModuleRuntime       每进程安装与热重载
runtime/HookContext         ClassLoader、进程与 Hook 注册表上下文
runtime/HookRegistry        稳定 Hook ID 与旧代际回收
features/LegacyHooks        兼容 Hook 主体
support/xposed              API 102 兼容适配层
support/nativepatch         原生复制、校验、37 处补丁与重定向
ui/StatusActivity           Miuix 或 Glasmio 状态页
ui/AppearanceStore          持久化界面风格
```

## 启动顺序

```text
onModuleLoaded
  └─ onPackageReady(com.huawei.hicreate)
      └─ ModuleRuntime.install(classLoader)
          └─ LegacyHooks.install()
              ├─ StubDex / hwframework / hwemui / widget 注入
              ├─ NativeCompat.install()（复制 + 校验 + 补丁）
              ├─ 启动/设备/界面/IO/权限/网络 Hook
              └─ 商店 host 改写、VibratorEx 兼容、笔刷行恢复
```

HMS 进程只安装 `Build`/系统属性伪装和包签名兼容，不写日志。

## 热重载

`onHotReloading` 保存代际/boot ID。`onHotReloaded` 使用当前 ClassLoader
重新安装 Java Hook，`HookRegistry.retireOld` 回收旧句柄。没有配置需要迁移。

原生补丁不参与热重载；原生兼容更新后必须重启目标应用。

## 支持基线

- 目标应用：`com.huawei.hicreate` `12.4.2.330`
- LSPosed API：102
- 原版库 SHA-256：
  `3CE153F4901CD40B2B3D164B0D345997F688E9AB329B5498CC3BC1D16A285E48`
- 补丁后 SHA-256：
  `B505EEA91BB28C171CCA626A544E9EE1010DC40900B0FC7EBAC76B6161934725`
- 补丁表：37 项（10 个基础补丁 + 18 个设备字符串门 + 9 个等级门）

不同目标版本或已重打包的目标 APK 不属于支持输入；原生路径会回退，不猜测偏移。
