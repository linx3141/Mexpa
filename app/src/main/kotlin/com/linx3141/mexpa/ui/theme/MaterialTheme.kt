package com.linx3141.mexpa.ui.theme

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import kotlin.math.roundToInt

/**
 * Material 3 Expressive 主题。生成方式、动画、纯黑处理与 KernelSU 的
 * `MaterialKernelSUTheme` 对齐：material-kolor 出 scheme → amoled 刷黑 → 颜色 spring 过渡 →
 * [MaterialExpressiveTheme]。
 *
 * Material 分支仍嵌套一层 [MiuixTheme]：迁移期还有少量 miuix 组件（搜索框、部分图标）在用，
 * 没有它会退回默认浅色调。两套 theme 的 CompositionLocal 互不覆盖。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MaterialMishkaTheme(
    themeConfig: ThemeConfig,
    isDark: Boolean,
    miuixColors: Colors,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val paletteStyle = remember(themeConfig.paletteStyle) { materialPaletteStyleOf(themeConfig.paletteStyle) }
    val colorSpec = remember(paletteStyle, themeConfig.colorSpec) {
        themeConfig.colorSpec.effectiveFor(paletteStyle)
    }
    val seedColor = remember(themeConfig.useMonet, themeConfig.accentColor, isDark, context) {
        materialSeedColor(themeConfig, isDark, context)
    }
    val isAmoled = themeConfig.useMonet && themeConfig.pureBlack && isDark

    val colorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        specVersion = colorSpec,
    ).amoledBackground(isAmoled)
    val animatedColorScheme = colorScheme.animateAsState(themeConfig.animationSpeed)
    val motionScheme = remember(themeConfig.animationSpeed) {
        if (themeConfig.animationSpeed == 1f) {
            MotionScheme.expressive()
        } else {
            ScaledMotionScheme(themeConfig.animationSpeed)
        }
    }

    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = motionScheme,
        typography = MishkaMaterialTypography,
    ) {
        MiuixTheme(colors = miuixColors) {
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
                MaterialTheme.colorScheme.let { scheme ->
                    androidx.compose.material3.LocalContentColor provides scheme.onBackground
                },
            ) {
                content()
            }
        }
    }
}

private fun materialSeedColor(themeConfig: ThemeConfig, isDark: Boolean, context: Context): Color {
    if (themeConfig.useMonet && themeConfig.accentColor == ThemeAccentColor.Default) {
        // 与 KernelSU 一致：未指定 keyColor 时用系统动态主色作种子
        return if (isDark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    }
    return themeConfig.accentColor.seedColor
}

private fun materialPaletteStyleOf(style: ThemePaletteStyle): PaletteStyle =
    PaletteStyle.entries.firstOrNull { it.name == style.name } ?: PaletteStyle.TonalSpot

/** 把 Material 的六组 MotionScheme 规格按用户速度缩放；speed > 1 更快，< 1 更慢 */
private class ScaledMotionScheme(private val speed: Float) : MotionScheme {
    private val base = MotionScheme.expressive()

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = base.defaultSpatialSpec<T>().scaled(speed)
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = base.fastSpatialSpec<T>().scaled(speed)
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = base.slowSpatialSpec<T>().scaled(speed)
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = base.defaultEffectsSpec<T>().scaled(speed)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = base.fastEffectsSpec<T>().scaled(speed)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = base.slowEffectsSpec<T>().scaled(speed)
}

private fun <T> FiniteAnimationSpec<T>.scaled(speed: Float): FiniteAnimationSpec<T> {
    if (speed == 1f) return this
    return when (this) {
        is TweenSpec<T> -> TweenSpec(
            durationMillis = (durationMillis / speed).roundToInt().coerceAtLeast(1),
            delay = (delay / speed).roundToInt(),
            easing = easing,
        )

        is SpringSpec<T> -> SpringSpec(
            dampingRatio = dampingRatio,
            stiffness = (stiffness * speed * speed).coerceAtLeast(1f),
            visibilityThreshold = visibilityThreshold,
        )

        else -> this
    }
}
