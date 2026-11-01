# Hook 清单

Hook 在目标进程启动时自动安装；UI 外观开关与 Hook 行为无关，没有 Hook 开关。

## 目标进程

| 领域 | 行为 |
|---|---|
| 运行时 | StubDex / hwframework / hwemui / featurelayer-widget 注入；Resources、TypedArray、ResourcesImpl 回退 |
| 设备身份 | `Build` 字段、系统属性、固定华为型号 `DBY-W09` |
| 启动 | Splash/设备检查、Home 流程、ProductInfo 能力兼容、退出拦截 |
| 界面 | 全 Activity NoActionBar、TextView 颜色/字号回退、华为控件映射、未知控件/动画回退、disabled alpha、DialogListAdapter |
| 笔刷/会话 | 真实 SwipeLayout helper、操作层可见、滑动/触摸保护、笔刷行图标、WorksTouch / MoveWorksActivity |
| 文件/分享 | 华为文件选择器、导入 Loading 布局恢复、SEND Intent 修复、URI 权限 |
| 权限 | 华为权限检查、AppOps、MANAGE_EXTERNAL_STORAGE |
| 网络/商店 | 强制在线、请求头、`iotopr-dra` → `iotopr-drcn` |
| 原生 | `libnativelib.so` 私有副本、SHA-256 校验、37 处补丁、重定向加载 |

## HMS 进程

- `Build` / 系统属性伪装；
- `ApplicationPackageManager` 的 `PackageInfo`、`checkSignatures`、
  `hasSigningCertificate` 兼容；
- 不写日志或报告。

## 热重载

`onHotReloading` 保留 Hook 句柄；`onHotReloaded` 用当前 ClassLoader 重装
Java Hook，并回收旧代际。原生补丁变化仍需重启目标应用。
