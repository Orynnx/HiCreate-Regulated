# 安装与使用

[返回首页](../README.md) · [English](INSTALL.en.md)

## 1. 确认准备条件

| 项目 | 需要准备 |
|---|---|
| Android | 12 或更高；Android 17 尚待兼容验证 |
| Root 与注入环境 | 已配置 Root、支持 API 102 的 LSPosed、Zygisk/同等注入环境 |
| 天生会画本体 | 原版 `com.huawei.hicreate` `12.4.2.330` |
| 兼容模块 | [Release 下载页](https://github.com/Orynnx/HiCreate-Regulated/releases/latest)中的 `HiCreate-Regulated-*.apk` |
| HMS Core | 如果已安装，则同时为 `com.huawei.hwid` 勾选作用域 |

在 Android“应用信息”中查看天生会画版本，也可安装模块后从状态页查看。
请将模块与天生会画安装在同一 Android 用户或工作资料中。

本仓库分发的是兼容模块。原版天生会画需要另行准备；
当前没有在本仓库提供目标应用历史版本的官方下载链接。
请先确认持有的安装包是上表所列版本，其他版本及重打包 APK 不在支持范围。

## 2. 下载并安装模块

打开 [最新 Release](https://github.com/Orynnx/HiCreate-Regulated/releases/latest)，在 Assets 中选择名称为
`HiCreate-Regulated-*.apk` 的文件。

- [直接下载当前 1.0.0-alpha-release 模块 APK](https://github.com/Orynnx/HiCreate-Regulated/releases/download/v1.0.0-alpha-release/HiCreate-Regulated-1.0.0-alpha-release.apk)
- [查看全部版本](https://github.com/Orynnx/HiCreate-Regulated/releases)

`Source code (zip)` 和 `Source code (tar.gz)` 是源码归档，安装时请选择 APK。

## 3. 在 LSPosed 中启用

1. 打开 LSPosed 管理器，找到“天生会画 兼容模块”。
2. 启用模块。
3. 作用域勾选 `com.huawei.hicreate`（天生会画）。
4. 如果安装了 HMS Core，再勾选 `com.huawei.hwid`。

模块会自动安装兼容 Hook，外观开关不影响适配行为。

## 4. 重新打开天生会画

在 Android 应用信息中强制停止天生会画，再重新打开。
首次启动和首次进入画布可能稍慢。

模块状态页可查看模块版本、天生会画版本与 HMS Core 安装情况。
返回页面时会重新检查；“使用 Apple 风格”仅改变该页面外观，
Android 12 保持 Miuix，Android 13+ 可选择 Glasmio。

## 5. 确认你的设备上的结果

先观察天生会画是否能进入首页，再用一份测试作品依次尝试：

- 新建或进入画布，绘制几笔并撤销；
- 切换笔刷，添加和切换图层；
- 导入测试图片；
- 保存、关闭并重新打开作品；
- 导出并检查导出的图片。

以上是建议的验收步骤，不表示所有机型上的各项功能均已验证。
记录“正常 / 异常 / 未测”，可通过[设备兼容性表单](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml)分享结果。

**状态页显示版本正常，不等于 Hook 已生效。**
模块不能读取 LSPosed 的作用域配置，页面中的“需勾选 / 按需勾选”是要求清单。

## 常见情况

| 现象 | 先检查什么 |
|---|---|
| 模块安装失败 | 记录 Android 的具体错误；若为签名冲突，卸载旧兼容模块后重装，再启用作用域。无需因此卸载天生会画本体 |
| 目标应用显示“未安装” | 是否安装在同一用户或工作资料；目标包名是否为 `com.huawei.hicreate` |
| 目标应用显示“不支持” | 是否为原版 `12.4.2.330`，而不是其他版本 |
| 启动仍失败 | 框架是否支持 API 102、模块是否启用、作用域是否正确，以及是否已强制停止后重开 |
| “需勾选”一直存在 | 这是静态要求文案，请到 LSPosed 确认实际设置 |
| Apple 外观无法启用 | Android 12 会自动保持 Miuix |
| Android 17 出现提示 | 表示该系统版本尚未完成兼容验证 |
| 云端素材或商店异常 | 网络/商店 Hook 的存在不代表服务端已接受当前设备；请单独反馈具体页面与现象 |

[提交问题反馈](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=bug_report.yml)时，请提供设备与系统、模块版本、
目标应用版本、LSPosed 版本、复现步骤及具体错误。模块本身不写日志或生成报告；
截图和已有的系统崩溃信息可以作为补充，无需寻找模块日志文件。
