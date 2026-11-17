# Installation and usage

[Back to README](../README.en.md) · [简体中文](INSTALL.md)

## 1. Check the requirements

| Item | What you need |
|---|---|
| Android | Android 12+; Android 17 compatibility remains unvalidated |
| Root and injection | Root, LSPosed supporting API 102, and Zygisk or an equivalent environment |
| GoPaint app | Stock `com.huawei.hicreate` `12.4.2.330` |
| Compatibility module | `HiCreate-Regulated-*.apk` from [Releases](https://github.com/Orynnx/HiCreate-Regulated/releases/latest) |
| HMS Core | Also select `com.huawei.hwid` in the scope if installed |

Check the GoPaint version in Android's app info or on the module's status page.
Install the module and GoPaint in the same Android user or work profile.

This repository distributes the compatibility module. Obtain the original drawing
app separately. The repository currently provides no official download link to
the required historical GoPaint version. Confirm the exact version before continuing;
other versions and repackaged APKs are outside the supported input.

## 2. Download and install

Open the [latest release](https://github.com/Orynnx/HiCreate-Regulated/releases/latest) and select
`HiCreate-Regulated-*.apk` under Assets.

- [Download the current 1.0.0-alpha-release APK directly](https://github.com/Orynnx/HiCreate-Regulated/releases/download/v1.0.0-alpha-release/HiCreate-Regulated-1.0.0-alpha-release.apk)
- [All releases](https://github.com/Orynnx/HiCreate-Regulated/releases)

The `Source code (zip)` and `Source code (tar.gz)` downloads are source archives;
choose the APK to install the module.

## 3. Enable it in LSPosed

1. Open LSPosed and find the module, labeled “天生会画 兼容模块”.
2. Enable the module.
3. Select `com.huawei.hicreate` (GoPaint) in its scope.
4. If HMS Core is installed, also select `com.huawei.hwid`.

Compatibility hooks are installed automatically. The appearance switch does not
change their behavior.

## 4. Reopen GoPaint

Force-stop GoPaint from Android's app info, then open it again.
The first launch and first canvas may take a little longer.

The module's status page shows the module version, target version and HMS Core
presence, refreshing when you return. Its appearance switch affects this page only.
Android 12 uses Miuix; Android 13+ can also use Glasmio.

## 5. Check the result on your device

After reaching the home screen, use a test artwork to try:

- Opening a canvas, drawing a few strokes and undoing them.
- Switching brushes, adding a layer and switching layers.
- Importing a test image.
- Saving, closing and reopening the artwork.
- Exporting and inspecting the resulting image.

These are suggested acceptance checks, not a claim that every workflow is verified
on every device. Record “works / fails / not tested” and
[share your result](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=compatibility.yml).

**A successful version check does not confirm that hooks are active.**
The module cannot read LSPosed's selected scopes. Its scope labels are setup instructions.

## Troubleshooting

| Symptom | What to check |
|---|---|
| Module installation fails | Record Android's exact error. For a signature conflict, uninstall the old compatibility module, reinstall and enable it again. GoPaint itself does not need to be removed |
| Target shown as missing | Same Android user/profile, and package name `com.huawei.hicreate` |
| Target shown as unsupported | Stock version `12.4.2.330` |
| Startup still fails | API 102 support, module enabled, correct scopes, force-stop and reopen completed |
| Scope still says it must be selected | This is static guidance; check the actual setting in LSPosed |
| Apple styling cannot be enabled | Android 12 stays on Miuix |
| Android 17 notice | Compatibility on this Android version has not been validated |
| Online materials or store fail | Network/store hooks do not establish server-side acceptance of the device; report the affected screen and behavior separately |

[Report a problem](https://github.com/Orynnx/HiCreate-Regulated/issues/new?template=bug_report.yml) with the device and OS,
module version, target version, LSPosed version, reproduction steps and exact error.
The module does not generate logs or reports. Screenshots and existing system crash
information can help; there is no module log file to retrieve.
