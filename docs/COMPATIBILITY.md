# 兼容性与测试记录

[返回首页](../README.md) · [安装指南](INSTALL.md) · [English](#english)

## 当前基线

| 项目 | 范围 |
|---|---|
| 模块版本 | `1.0.0-alpha-release` |
| 目标应用 | 原版 `com.huawei.hicreate` `12.4.2.330` |
| Android 最低版本 | Android 12 / API 31 |
| 注入要求 | Root、LSPosed API 102、Zygisk/同等注入环境 |
| Apple 风格状态页 | Android 13+；Android 12 使用 Miuix |
| Android 17 | 尚未完成兼容验证 |

## 已有测试记录

| 设备 | 系统 | 当前证据 |
|---|---|---|
| 三星 Samsung Galaxy Tab S8+ | One UI 8.0 | 项目原有 README 记录已测试；仓库提供状态页与天生会画首页截图 |
| 其他非华为 Android 设备 | 各机型分别记录 | 暂无已归档的逐项测试记录，欢迎提交 |

这份表只覆盖列出的设备与环境。模块面向非华为 Android 设备，其他机型需独立测试。
目前仓库尚未提供可逐项核对的完整绘画工作流测试记录；首页截图只能佐证到达首页，
不能据此判定绘画、图层、导出或云端素材全部正常。

## 各类状态分别说明什么

- **版本自检正常**：模块能读取已安装应用的包信息。
- **已进入首页**：启动路径在记录的环境中可走通。
- **代码已有兼容处理**：存在对应 Hook，具体功能结果仍需运行验证。
- **功能实测正常**：应附上设备、系统、应用与模块版本及操作结果。

网络、商店与 HMS 已有兼容处理；云端素材和服务是否可用，应作为独立项目记录。
作用域提示不是实时启用状态。原生补丁有哈希门控，其他目标应用版本不在支持范围。

## 建议测试项目

请使用测试作品，逐项填写“正常 / 异常 / 未测”：

| 项目 | 观察内容 |
|---|---|
| 启动 | 是否进入首页、是否闪退 |
| 绘画 | 新建/进入画布、绘制笔画、撤销 |
| 笔刷与图层 | 切换笔刷、添加/切换图层 |
| 保存 | 关闭并重新打开作品，内容是否保留 |
| 导入导出 | 导入测试图片，导出结果是否可打开 |
| 作品管理 | 复制、收藏、移动等实际操作结果 |
| 云端素材 | 列表是否有内容、素材能否实际下载 |
| 外观 | 深浅色、Miuix/Glasmio 页面显示情况 |

**[提交设备兼容性记录](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml)**

反馈保留在 Issue 中便于追踪；维护者核对后再更新本表。未做过的测试请标为“未测”。

## English

[English README](../README.en.md) · [Installation](INSTALL.en.md)

The supported baseline is module `1.0.0-alpha-release`, stock GoPaint
`com.huawei.hicreate` `12.4.2.330`, Android 12+, Root and LSPosed API 102.

| Device or platform | Current evidence |
|---|---|
| Samsung Galaxy Tab S8+ / One UI 8.0 | The original project README records testing; status-page and GoPaint home-screen screenshots are included |
| Other non-Huawei devices | No archived, item-by-item results yet; individual model testing is welcome |
| Android 17 | Compatibility remains unvalidated |
| Apple-style status page | Requires Android 13+; Android 12 uses Miuix |

The repository does not yet provide a complete, itemized drawing-workflow test
record. A home-screen screenshot demonstrates reaching the home screen, not that
all drawing, layer, export or online-material workflows pass.

Keep version detection, hook implementation and observed feature results separate.
Online services need their own checks; network/HMS compatibility hooks alone do
not establish server acceptance. Scope labels are setup instructions rather than
live activation status.

For a test artwork, report startup, drawing/undo, brushes/layers, save/reopen,
import/export, artwork management, online materials and appearance as
**works / fails / not tested**. Include the device, OS, module version, target
version and LSPosed version.

**[Submit a compatibility report](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml)**.
Reports remain in Issues for traceability and can be added to this table after review.
