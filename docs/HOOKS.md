# Hook 清单

Hook 在目标进程启动时自动安装；UI 外观开关与 Hook 行为无关，没有 Hook 开关。

## 目标进程

| 领域 | 行为 |
|---|---|
| 运行时 | StubDex / hwframework / hwemui / featurelayer-widget 注入；Resources、TypedArray、ResourcesImpl 回退 |
| 设备身份 | 华为品牌/型号与 EMUI 属性；保留真实 HARDWARE/BOARD/SOC 与硬件属性 |
| 启动 | Splash/设备检查、Home 流程、ProductInfo 能力兼容；保留系统正常退出/崩溃处理 |
| 界面 | 目标 Activity 使用应用自带 AppCompat.NoActionBar、沿用天生会画原有的平板密度计算、TextView 颜色/字号回退、华为控件映射、未知控件/动画回退、disabled alpha、DialogListAdapter |
| 笔刷/会话 | 真实 SwipeLayout helper、操作层可见、滑动/触摸保护、笔刷行图标、WorksTouch / MoveWorksActivity |
| 文件/分享 | 华为文件选择器、导入 Loading 布局恢复、SEND Intent 修复、URI 权限 |
| 权限 | 华为权限检查、AppOps、MANAGE_EXTERNAL_STORAGE |
| 网络/商店 | 强制在线、请求头、`iotopr-dra` → `iotopr-drcn` |
| 厂商兼容 | 原生加载保留调用方与厂商命名空间；移除 MIUI RenderNode 空实现绕过 |
| 原生 | `libnativelib.so` 私有副本、SHA-256 校验、37 处补丁、Runtime 层限定调用方的重定向加载 |

## HMS 进程

- `Build` / 系统属性伪装；
- `ApplicationPackageManager` 的 `PackageInfo`、`checkSignatures`、
  `hasSigningCertificate` 兼容；
- 不写日志或报告。

## 热重载

`onHotReloading` 保留 Hook 句柄；`onHotReloaded` 用当前 ClassLoader 重装
Java Hook，并回收旧代际。原生补丁变化仍需重启目标应用。
