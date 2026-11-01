package com.orynnx.hicreatehook.ui

import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Status page. It has exactly one user preference: the component system used to
 * render the page. The compatibility hooks remain zero-configuration.
 * The checks are re-run whenever the Activity resumes.
 */
class StatusActivity : ComponentActivity() {

    private val checkTick = mutableIntStateOf(0)
    private val style = mutableStateOf(UiStyle.MIUIX)
    private val showAndroid17Warning = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style.value = AppearanceStore.load(this)
        showAndroid17Warning.value = shouldShowAndroid17Warning(Build.VERSION.SDK_INT)
        setContentView(ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StatusActivity)
            setViewTreeViewModelStoreOwner(this@StatusActivity)
            setViewTreeSavedStateRegistryOwner(this@StatusActivity)
            setContent {
                StatusRoot(
                    tick = checkTick.intValue,
                    style = style.value,
                    showAndroid17Warning = showAndroid17Warning.value,
                    onDismissAndroid17Warning = { showAndroid17Warning.value = false },
                    onStyleChange = ::selectStyle,
                )
            }
        })
        updateSystemBars()
    }

    override fun onResume() {
        super.onResume()
        updateSystemBars()
        checkTick.intValue++
    }

    private fun selectStyle(next: UiStyle) {
        val safe = if (next == UiStyle.GLASMIO && android.os.Build.VERSION.SDK_INT < 33) {
            UiStyle.MIUIX
        } else {
            next
        }
        style.value = safe
        AppearanceStore.save(this, safe)
        updateSystemBars()
    }

    private fun updateSystemBars() {
        val night = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val lightBars = if (night) 0 else
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(
            lightBars,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    }
}

internal data class CheckState(
    val moduleVersion: String,
    val targetVersion: String?,
    val targetSupported: Boolean,
    val hmsInstalled: Boolean,
    val checkedAt: String,
)

@Composable
private fun StatusRoot(
    tick: Int,
    style: UiStyle,
    showAndroid17Warning: Boolean,
    onDismissAndroid17Warning: () -> Unit,
    onStyleChange: (UiStyle) -> Unit,
) {
    if (style == UiStyle.GLASMIO && Build.VERSION.SDK_INT >= 33) {
        GlasmioStatusPage(
            tick = tick,
            showAndroid17Warning = showAndroid17Warning,
            onDismissAndroid17Warning = onDismissAndroid17Warning,
            onStyleChange = onStyleChange,
        )
    } else {
        MiuixStatusPage(
            tick = tick,
            style = style,
            showAndroid17Warning = showAndroid17Warning,
            onDismissAndroid17Warning = onDismissAndroid17Warning,
            onStyleChange = onStyleChange,
        )
    }
}

@Composable
private fun MiuixStatusPage(
    tick: Int,
    style: UiStyle,
    showAndroid17Warning: Boolean,
    onDismissAndroid17Warning: () -> Unit,
    onStyleChange: (UiStyle) -> Unit,
) {
    val context = LocalContext.current
    val checks = remember(tick) { runChecks(context) }

    MiuixTheme(controller = remember { ThemeController(ColorSchemeMode.System) }) {
        val colors = MiuixTheme.colorScheme
        val styles = MiuixTheme.textStyles
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = "天生会画 兼容模块",
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                SmallTitle("外观")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "使用 Apple 风格",
                        endActions = {
                            Switch(
                                checked = style == UiStyle.GLASMIO,
                                onCheckedChange = {
                                    onStyleChange(if (it) UiStyle.GLASMIO else UiStyle.MIUIX)
                                },
                            )
                        },
                    )
                }

                SmallTitle("自检")
                Text(
                    text = "上次 ${checks.checkedAt}",
                    style = styles.body2,
                    color = colors.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(title = "模块版本", summary = checks.moduleVersion)
                    HorizontalDivider()
                    BasicComponent(
                        title = "目标应用",
                        summary = checks.targetVersion ?: "未安装",
                        endActions = {
                            if (checks.targetVersion != null && !checks.targetSupported) {
                                Text(
                                    text = "不支持",
                                    style = styles.body2,
                                    color = colors.error,
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "HMS Core",
                        summary = if (checks.hmsInstalled) "已安装" else "未安装",
                    )
                }

                SmallTitle("LSPosed 作用域")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicComponent(
                        title = "com.huawei.hicreate",
                        endActions = { Text("需勾选", style = styles.body2, color = colors.primary) },
                    )
                    HorizontalDivider()
                    BasicComponent(
                        title = "com.huawei.hwid",
                        endActions = {
                            Text("按需勾选", style = styles.body2, color = colors.onSurfaceVariantSummary)
                        },
                    )
                }
            }
        }
        if (showAndroid17Warning) {
            MiuixAndroid17WarningDialog(onDismissAndroid17Warning)
        }
    }
    }
}

@Composable
private fun MiuixAndroid17WarningDialog(onDismiss: () -> Unit) {
    SuperDialog(
        show = true,
        title = "Android 17 兼容性提示",
        summary = "检测到 Android 17。模块可能尚未兼容 Android 17，如有需要，请联系模块作者。",
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            MiuixButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    }
}

internal fun runChecks(context: Context): CheckState {
    val pm = context.packageManager
    val module = packageInfo(pm, context.packageName)
    val target = packageInfo(pm, TARGET)
    val hms = packageInfo(pm, HMS)
    return CheckState(
        moduleVersion = module?.versionName ?: "unknown",
        targetVersion = target?.versionName,
        targetSupported = target?.longVersionCode == EXPECTED_TARGET_VERSION,
        hmsInstalled = hms != null,
        checkedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    )
}

private fun packageInfo(pm: PackageManager, packageName: String): PackageInfo? {
    return try {
        pm.getPackageInfo(packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

private const val TARGET = "com.huawei.hicreate"
private const val HMS = "com.huawei.hwid"
private const val EXPECTED_TARGET_VERSION = 120402330L

internal fun shouldShowAndroid17Warning(sdkInt: Int): Boolean = sdkInt >= 37
