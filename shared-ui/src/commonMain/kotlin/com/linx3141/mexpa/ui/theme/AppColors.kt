package com.linx3141.mexpa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 语义色入口：页面/组件不再直接读 `MiuixTheme.colorScheme`，而是读这里。
 *
 * Miuix 模式逐个透传 miuix token（颜色值不变）；Material 模式映射到最接近的 M3
 * ColorScheme token。两套调色板由同一个种子色/风格生成，因此风格切换时不会出现
 * 「卡片是 M3 的、文字还是 miuix 的」这种色差。
 */
object MishkaColors {

    val onSurface: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface

    val onSurfaceVariantSummary: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        }

    val onSurfaceVariantActions: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantActions
        }

    val surface: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.surface else MiuixTheme.colorScheme.surface

    val surfaceContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        }

    val onSurfaceContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceContainer

    val onBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.onBackground else MiuixTheme.colorScheme.onBackground

    val primary: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.primary else MiuixTheme.colorScheme.primary

    val secondaryContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MiuixTheme.colorScheme.secondaryContainer
        }

    val onSecondaryContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MiuixTheme.colorScheme.onSecondaryContainer
        }

    val disabledOnSecondaryVariant: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        } else {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        }

    val errorContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.errorContainer else MiuixTheme.colorScheme.errorContainer

    val onErrorContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onErrorContainer

    val dividerLine: Color
        @Composable @ReadOnlyComposable
        get() = if (isMaterial()) MaterialTheme.colorScheme.outlineVariant else MiuixTheme.colorScheme.dividerLine

    @Composable
    @ReadOnlyComposable
    private fun isMaterial(): Boolean = LocalUiMode.current == UiMode.Material
}
