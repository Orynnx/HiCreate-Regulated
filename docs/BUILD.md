# 构建

## 环境

| 项目 | 版本 |
|---|---|
| JDK | 21（`JAVA_HOME`） |
| Gradle | 9.3.1（含 wrapper） |
| AGP | 9.1.0 |
| Kotlin | AGP 9 内置 Kotlin + Compose compiler 2.4.10 |
| Miuix | 0.8.8 |
| Glasmio | 0.1.0 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 31 |

## SDK 路径

`local.properties` 不提交到仓库。复制示例并设置 SDK 路径：

```properties
sdk.dir=/path/to/Android/Sdk
```

## 华为运行时载荷

源码仓库**不包含**以下私有载荷：

```text
featurelayer-widget.dex
hwemui.dex
hwext-res.apk
hwframework-full.jar
hwframework.dex
xposed_init
```

将它们放到项目根目录的 `local-assets/`（已被 git 忽略），或使用：

```powershell
.\gradlew.bat :app:assembleRelease "-PassetsDir=C:\path\to\assets"
```

`syncModuleAssets` 会把载荷复制到构建生成的 assets 目录。release 构建缺少
任意一个文件都会在打包前失败；debug 和测试构建不要求载荷。

## 构建与测试

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:ANDROID_HOME = "C:\path\to\Android\Sdk"

.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk      ~108.6 MB
app/build/outputs/apk/release/app-release.apk  ~40.3 MB
```

## release 签名

签名从环境变量读取：

```text
HICREATE_RELEASE_STORE_FILE
HICREATE_RELEASE_STORE_PASSWORD
HICREATE_RELEASE_KEY_ALIAS
HICREATE_RELEASE_KEY_PASSWORD
```

未配置完整签名时，`assembleRelease` 会直接失败，不会静默使用 debug
签名。仅本地调试可显式放行：

```powershell
.\gradlew.bat :app:assembleRelease -PallowDebugSigning=true
```

分发 release 必须使用独立 keystore；keystore、密码和
`release-signing.txt` 不得提交到仓库。

## 版本

```text
versionName = 1.0.0-alpha-release
versionCode = 10000
```
