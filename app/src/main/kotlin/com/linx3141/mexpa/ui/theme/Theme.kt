package com.linx3141.mexpa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.linx3141.mexpa.ui.component.blur.LocalBlurEnabled
import com.linx3141.mexpa.ui.component.blur.LocalTopBarBlurStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.platformDynamicColors

/**
 * 应用主题入口。Miuix 是默认外观（与改造前逐像素一致），[UiMode.Material] 走
 * Material 3 Expressive；两套风格共用同一份 [ThemeConfig]（深浅、取色、字号缩放、模糊开关）。
 *
 * 页面与组件不要直接读 `isSystemInDarkTheme()`/`MiuixTheme`，深浅一律走 [LocalAppDarkMode]，
 * 风格一律走 [LocalUiMode]——这样强制深浅或切换风格时整棵树才会一起跟随。
 */
/** 代理页节点网格：true = 二维 segmented grid，false = 每个节点独立卡片（Material 下生效，默认独立卡片） */
val LocalProxyGridSegmented = compositionLocalOf { false }

/** 全局动画速度倍率（1 = 原始速度），供 Material MotionScheme 与自定义动画读取 */
val LocalAnimationSpeed = compositionLocalOf { 1f }

@Composable
fun MishkaTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val isDark = themeConfig.resolveIsDark(isSystemInDarkTheme())
    val controller = rememberMiuixThemeController(themeConfig, isDark)
    val colors = controller.currentColors()
    // pureBlack 只在 Monet 开启时生效，与改造前 Miuix 分支的判据保持一致
    val themedColors = remember(colors, isDark, themeConfig.pureBlack, themeConfig.useMonet) {
        if (themeConfig.useMonet && themeConfig.pureBlack && isDark) {
            colors.copy(background = Color.Black, surface = Color.Black)
        } else {
            colors
        }
    }

    val currentDensity = LocalDensity.current
    val appDensity = remember(currentDensity, themeConfig.densityScale) {
        Density(
            density = currentDensity.density * themeConfig.densityScale,
            fontScale = currentDensity.fontScale,
        )
    }

    // 主题分支切换时 AppNavigation 会换到另一个调用点；用 movable content 把整棵子树搬过去，
    // 保留返回栈与页面状态（否则在「外观与主题」里切风格会被弹回首页）
    val latestContent by rememberUpdatedState(content)
    val movableContent = remember { movableContentOf { latestContent() } }

    CompositionLocalProvider(
        LocalAppDarkMode provides isDark,
        LocalAppMonetEnabled provides themeConfig.useMonet,
        LocalPlatformDensity provides currentDensity,
        LocalDensity provides appDensity,
        LocalBlurEnabled provides themeConfig.blurEnabled,
        LocalTopBarBlurStyle provides themeConfig.topBarBlurStyle,
        LocalUiMode provides themeConfig.uiMode,
        LocalProxyGridSegmented provides themeConfig.proxyGridSegmented,
        LocalAnimationSpeed provides themeConfig.animationSpeed,
    ) {
        when (themeConfig.uiMode) {
            UiMode.Miuix -> MiuixMishkaTheme(themedColors) { movableContent() }
            UiMode.Material -> MaterialMishkaTheme(themeConfig, isDark, themedColors) { movableContent() }
        }
    }
}

@Composable
internal fun MiuixMishkaTheme(
    colors: Colors,
    content: @Composable () -> Unit,
) {
    MiuixTheme(colors = colors) {
        CompositionLocalProvider(
            LocalContentColor provides MiuixTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

@Composable
internal fun rememberMiuixThemeController(
    themeConfig: ThemeConfig,
    isDark: Boolean,
): ThemeController {
    val colorSchemeMode = when {
        !themeConfig.useMonet && themeConfig.colorMode == 1 -> ColorSchemeMode.Light
        !themeConfig.useMonet && themeConfig.colorMode == 2 -> ColorSchemeMode.Dark
        !themeConfig.useMonet -> ColorSchemeMode.System
        themeConfig.colorMode == 1 -> ColorSchemeMode.MonetLight
        themeConfig.colorMode == 2 -> ColorSchemeMode.MonetDark
        else -> ColorSchemeMode.MonetSystem
    }
    val keyColor = when {
        !themeConfig.useMonet -> null
        themeConfig.accentColor == ThemeAccentColor.Default -> platformDynamicColors(isDark).primary
        else -> themeConfig.accentColor.seedColor
    }
    return remember(themeConfig, colorSchemeMode, keyColor, isDark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = themeConfig.paletteStyle,
        )
    }
}
