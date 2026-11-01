# 第三方依赖与许可

本模块的自有代码只使用官方/开源库，不包含从其他 Xposed 模块复制的代码。
华为接口桩和运行时载荷属于互操作性兼容材料，未包含在本源码仓库中。

| 依赖 | 版本 | 许可 | 用途 |
|---|---|---|---|
| `io.github.libxposed:api` | 102.0.0 | [Apache-2.0](https://github.com/libxposed/api/blob/master/LICENSE) | Xposed API 102、模块入口、Hook 与热重载 |
| `top.yukonga.miuix.kmp:miuix-android` | 0.8.8 | [Apache-2.0](https://github.com/compose-miuix-ui/miuix/blob/main/LICENSE) | Miuix 状态页组件与主题 |
| `io.github.neoruaa.glasmio:glasmio-ui` | 0.1.0 | [Apache-2.0](https://github.com/NEORUAA/Glasmio/blob/main/LICENSE) | Glasmio 状态页组件 |
| JetBrains Compose（由 Miuix 传递） | 1.10.3 | [Apache-2.0](https://github.com/JetBrains/compose-multiplatform/blob/master/LICENSE.txt) | Compose 运行时、布局、UI |
| AndroidX Compose（由 Glasmio 传递） | 1.12.1 | [Apache-2.0](https://developer.android.com/jetpack/androidx/releases/compose) | Glasmio 的 Compose 运行时与控件 |
| `androidx.activity:activity-ktx` | 1.13.0 | [Apache-2.0](https://developer.android.com/jetpack/androidx/releases/activity) | `ComponentActivity` 生命周期宿主 |
| JUnit | 4.13.2 | [EPL-1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt) | 单元测试 |

## 资源文件

`hwext-res.apk`、`hwframework-full.jar`、`hwframework.dex`、`hwemui.dex`、
`featurelayer-widget.dex` 来自华为设备/应用，仅作为兼容运行所需的数据载荷。它们
**不在本源码仓库中**，由使用者在本地通过 `local-assets/` 或 `-PassetsDir` 提供；
本仓库不声明对其拥有版权，也不对其再授权。源码仓库不得提交这些文件；Release
二进制由本地载荷构建，其再分发范围由使用者自行确认。

## 不使用

- 不引入 YukiHookAPI、XposedBridge 旧版 API、XSharedPreferences 等与 API 102
  重复或已禁用的实现。
- 不引入 RemotePreferences、XposedService、Material 组件或任何需要联网的 SDK。
- 不写模块日志、报告；唯一持久化偏好是状态页的界面风格。
