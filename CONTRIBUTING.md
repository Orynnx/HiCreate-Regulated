# 参与贡献 / Contributing

欢迎帮助改善设备兼容性、安装说明、翻译和界面展示。问题反馈和实测结果同样有价值。

- [反馈问题](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=bug_report.yml)：提供复现步骤、设备与版本。
- [报告设备兼容性](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml)：逐项填写正常、异常或未测。
- 改进代码或文档：提交 Pull Request，说明修改内容和实际完成的验证。

## 开始开发

构建环境和命令见 [docs/BUILD.md](docs/BUILD.md)。需要 JDK 21 和 Android SDK 37。

源码仓库不包含华为运行时载荷。debug 和单元测试构建不要求载荷；
缺少载荷的 debug APK 不能据此当成完整可运行的兼容模块。
完整运行与 Release 构建需要自行提供文档所列资产，Release 还需要签名配置。

请勿将目标应用 APK、华为载荷、keystore 或签名密码加入源码提交。
先用 [架构](docs/ARCHITECTURE.md)、[Hook 清单](docs/HOOKS.md)和
[原生兼容](docs/NATIVE.md)定位对应实现。

## 提交时说明

1. 改变了什么，以及对应的具体问题。
2. 实际做过哪些验证。编译、单元测试和实机测试分别写清。
3. 涉及设备兼容时，附设备/系统、目标应用、模块及 LSPosed 版本。

文档和图片修改检查链接、文字、排版即可；运行时修改应验证受影响的流程。
无法验证的项目直接说明，避免将未测试状态写成通过。
截图请使用自己的测试内容，并避免包含账号等无关私人信息。

## English

Contributions to compatibility, installation instructions, translations and
presentation are welcome. Bug reports and real device results are useful too.

Use the [bug form](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=bug_report.yml) or
[compatibility form](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml), or open a pull request
explaining the concrete change and validation performed.

The [build guide](docs/BUILD.md) specifies JDK 21 and Android SDK 37.
Huawei runtime assets are not included in the repository. Debug/unit-test builds
do not require them, but a debug APK without those payloads is not a complete
runtime-compatible module. Full runtime/release builds need locally supplied assets;
release builds also need signing configuration.

Do not commit target APKs, proprietary payloads, keystores or signing credentials.
For runtime changes, distinguish compilation, unit tests and device tests.
Include the device/OS, target app, module and LSPosed versions.
Documentation-only changes need link, content and layout checks. State any test
you could not run; use your own test content in screenshots.
