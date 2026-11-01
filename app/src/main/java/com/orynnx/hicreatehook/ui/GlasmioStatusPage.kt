package com.orynnx.hicreatehook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioGroupedTableFooter
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import io.github.neoruaa.glasmio.ui.GlasmioTopBar

/** Glasmio's own component hierarchy; never combined with Miuix widgets. */
@Composable
internal fun GlasmioStatusPage(
    tick: Int,
    showAndroid17Warning: Boolean,
    onDismissAndroid17Warning: () -> Unit,
    onStyleChange: (UiStyle) -> Unit,
) {
    val context = LocalContext.current
    val checks = remember(tick) { runChecks(context) }

    GlasmioTheme(isDark = isSystemInDarkTheme()) {
        val colors = GlasmioTheme.colors
        GlasmioBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Column(Modifier.fillMaxSize().background(colors.groupedBackground)) {}
            },
            overlayContent = {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    GlasmioTopBar(title = "天生会画 兼容模块")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                        ) {
                        GlasmioSectionTitle("外观")
                        GlasmioGroupedList(modifier = Modifier.padding(horizontal = 16.dp)) {
                            GlasmioListRow(
                                title = "使用 Apple 风格",
                                trailing = {
                                    GlasmioSwitch(
                                        checked = true,
                                        onCheckedChange = {
                                            if (!it) onStyleChange(UiStyle.MIUIX)
                                        },
                                    )
                                },
                                showTopSeparator = false,
                            )
                        }

                        GlasmioSectionTitle("自检")
                        GlasmioGroupedList(modifier = Modifier.padding(horizontal = 16.dp)) {
                            GlasmioListRow(
                                title = "模块版本",
                                detail = checks.moduleVersion,
                                showTopSeparator = false,
                            )
                            GlasmioListRow(
                                title = "目标应用",
                                subtitle = if (checks.targetVersion != null && !checks.targetSupported) {
                                    checks.targetVersion
                                } else {
                                    null
                                },
                                detail = when {
                                    checks.targetVersion == null -> "未安装"
                                    checks.targetSupported -> checks.targetVersion
                                    else -> "不支持"
                                },
                            )
                            GlasmioListRow(
                                title = "HMS Core",
                                detail = if (checks.hmsInstalled) "已安装" else "未安装",
                            )
                        }
                        GlasmioGroupedTableFooter(
                            "上次 ${checks.checkedAt}",
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        GlasmioSectionTitle("LSPosed 作用域")
                        GlasmioGroupedList(modifier = Modifier.padding(horizontal = 16.dp)) {
                            GlasmioListRow(
                                title = "com.huawei.hicreate",
                                detail = "需勾选",
                                showTopSeparator = false,
                            )
                            GlasmioListRow(
                                title = "com.huawei.hwid",
                                detail = "按需勾选",
                            )
                        }
                    }
                }
            }
        },
        )
        if (showAndroid17Warning) {
            GlasmioAlertDialog(
                onDismissRequest = onDismissAndroid17Warning,
                title = "Android 17 兼容性提示",
                message = "检测到 Android 17。模块可能尚未兼容 Android 17，如有需要，请联系模块作者。",
                buttons = listOf(
                    GlasmioAlertButtonSpec(
                        label = "知道了",
                        onClick = onDismissAndroid17Warning,
                    )
                ),
            )
        }
    }
}
