package com.linx3141.mexpa.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode

data class ThemeConfig(
    val colorMode: Int = 0,
    val uiMode: UiMode = UiMode.Default,
    val pureBlack: Boolean = false,
    val useMonet: Boolean = false,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val accentColor: ThemeAccentColor = ThemeAccentColor.Default,
    val blurEnabled: Boolean = true,
    val topBarBlurStyle: TopBarBlurStyle = TopBarBlurStyle.Gaussian,
    val floatingBottomBar: Boolean = false,
    val materialFloatingBottomBar: Boolean = true,
    val floatingBottomBarStyle: FloatingBottomBarStyle = FloatingBottomBarStyle.Miuix,
    val bottomBarMode: BottomBarMode = BottomBarMode.IconAndText,
    val pagerInterceptionMode: PagerInterceptionMode = PagerInterceptionMode.CrossAxisInterceptor,
    val densityScale: Float = DefaultDensityScale,
    val animationSpeed: Float = DefaultAnimationSpeed,
    val proxyGridSegmented: Boolean = false,
)

const val MinDensityScale = 0.8f
const val MaxDensityScale = 1.1f
const val DefaultDensityScale = 1f
const val MinAnimationSpeed = 0.5f
const val MaxAnimationSpeed = 2f
const val DefaultAnimationSpeed = 1f

/** colorMode → 深色判定的唯一权威实现；systemDark 由调用方传入所在平台的系统深色状态 */
fun ThemeConfig.resolveIsDark(systemDark: Boolean): Boolean = when (colorMode) {
    1 -> false
    2 -> true
    else -> systemDark
}

fun normalizeDensityScale(value: Float): Float =
    if (value.isFinite()) value.coerceIn(MinDensityScale, MaxDensityScale) else DefaultDensityScale

fun normalizeAnimationSpeed(value: Float): Float =
    if (value.isFinite()) value.coerceIn(MinAnimationSpeed, MaxAnimationSpeed) else DefaultAnimationSpeed

enum class FloatingBottomBarStyle(val storageValue: String) {
    Miuix("miuix"),
    IosLike("ios_like");

    companion object {
        fun fromStorage(value: String): FloatingBottomBarStyle =
            entries.firstOrNull { it.storageValue == value } ?: Miuix
    }
}

enum class BottomBarMode(val storageValue: String) {
    IconAndText("icon_and_text"),
    IconOnly("icon_only");

    companion object {
        fun fromStorage(value: String): BottomBarMode =
            entries.firstOrNull { it.storageValue == value } ?: IconAndText
    }
}

enum class TopBarBlurStyle(val storageValue: String) {
    Gaussian("gaussian"),
    Progressive("progressive");

    companion object {
        fun fromStorage(value: String): TopBarBlurStyle =
            entries.firstOrNull { it.storageValue == value } ?: Gaussian
    }
}

enum class ThemeAccentColor(
    val storageValue: String,
    val seedColor: Color,
) {
    Default("default", Color(0xFF3482FF)),
    Blue("blue", Color(0xFF3482FF)),
    Purple("purple", Color(0xFF6750A4)),
    Pink("pink", Color(0xFFB0006D)),
    Red("red", Color(0xFFBA1A1A)),
    Orange("orange", Color(0xFFB65D00)),
    Yellow("yellow", Color(0xFF7D5700)),
    Green("green", Color(0xFF006D3B)),
    Teal("teal", Color(0xFF006A6A));

    companion object {
        fun fromStorage(value: String): ThemeAccentColor =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

val ThemePaletteStyles: List<ThemePaletteStyle> = ThemePaletteStyle.entries.toList()

fun themePaletteStyleFromStorage(value: String): ThemePaletteStyle =
    ThemePaletteStyles.firstOrNull { it.name == value } ?: ThemePaletteStyle.TonalSpot

fun readThemeConfig(storage: PlatformStorage): ThemeConfig {
    val colorMode = when (storage.getString(StorageKeys.DARK_MODE, "system")) {
        "light" -> 1
        "dark" -> 2
        else -> 0
    }
    return ThemeConfig(
        colorMode = colorMode,
        uiMode = UiMode.fromStorage(storage.getString(StorageKeys.THEME_UI_MODE, UiMode.Default.storageValue)),
        pureBlack = storage.getString(StorageKeys.THEME_PURE_BLACK, "false") == "true",
        useMonet = storage.getString(StorageKeys.THEME_MONET, "false") == "true",
        paletteStyle = themePaletteStyleFromStorage(
            storage.getString(StorageKeys.THEME_PALETTE_STYLE, ThemePaletteStyle.TonalSpot.name),
        ),
        colorSpec = ColorSpec.SpecVersion.entries.firstOrNull {
            it.name == storage.getString(StorageKeys.THEME_COLOR_SPEC, ColorSpec.SpecVersion.SPEC_2025.name)
        } ?: ColorSpec.SpecVersion.SPEC_2025,
        accentColor = ThemeAccentColor.fromStorage(
            storage.getString(StorageKeys.THEME_ACCENT_COLOR, ThemeAccentColor.Default.storageValue),
        ),
        blurEnabled = storage.getString(StorageKeys.THEME_BLUR, "true") != "false",
        topBarBlurStyle = TopBarBlurStyle.fromStorage(
            storage.getString(StorageKeys.THEME_BLUR_STYLE, TopBarBlurStyle.Gaussian.storageValue),
        ),
        floatingBottomBar = storage.getString(StorageKeys.THEME_FLOATING_BOTTOM_BAR, "false") == "true",
        materialFloatingBottomBar = storage.getString(
            StorageKeys.THEME_MATERIAL_FLOATING_BOTTOM_BAR,
            "true",
        ) == "true",
        floatingBottomBarStyle = FloatingBottomBarStyle.fromStorage(
            storage.getString(StorageKeys.THEME_FLOATING_BOTTOM_BAR_STYLE, FloatingBottomBarStyle.Miuix.storageValue),
        ),
        bottomBarMode = BottomBarMode.fromStorage(
            storage.getString(StorageKeys.THEME_BOTTOM_BAR_MODE, BottomBarMode.IconAndText.storageValue),
        ),
        pagerInterceptionMode = pagerInterceptionModeFromStorage(
            storage.getString(
                StorageKeys.THEME_PAGER_GESTURE_MODE,
                PagerInterceptionMode.CrossAxisInterceptor.name,
            ),
        ),
        densityScale = normalizeDensityScale(
            storage.getString(StorageKeys.THEME_DENSITY_SCALE, DefaultDensityScale.toString()).toFloatOrNull()
                ?: DefaultDensityScale,
        ),
        animationSpeed = normalizeAnimationSpeed(
            storage.getString(StorageKeys.THEME_ANIMATION_SPEED, DefaultAnimationSpeed.toString())
                .toFloatOrNull() ?: DefaultAnimationSpeed,
        ),
        proxyGridSegmented = storage.getString(StorageKeys.THEME_PROXY_GRID_SEGMENTED, "false") == "true",
    )
}

fun writeThemeConfig(storage: PlatformStorage, config: ThemeConfig) {
    storage.putString(
        StorageKeys.DARK_MODE,
        when (config.colorMode) {
            1 -> "light"
            2 -> "dark"
            else -> "system"
        },
    )
    storage.putString(StorageKeys.THEME_UI_MODE, config.uiMode.storageValue)
    storage.putString(StorageKeys.THEME_PURE_BLACK, config.pureBlack.toString())
    storage.putString(StorageKeys.THEME_MONET, config.useMonet.toString())
    storage.putString(StorageKeys.THEME_PALETTE_STYLE, config.paletteStyle.name)
    storage.putString(StorageKeys.THEME_COLOR_SPEC, config.colorSpec.name)
    storage.putString(StorageKeys.THEME_ACCENT_COLOR, config.accentColor.storageValue)
    storage.putString(StorageKeys.THEME_BLUR, config.blurEnabled.toString())
    storage.putString(StorageKeys.THEME_BLUR_STYLE, config.topBarBlurStyle.storageValue)
    storage.putString(StorageKeys.THEME_FLOATING_BOTTOM_BAR, config.floatingBottomBar.toString())
    storage.putString(
        StorageKeys.THEME_MATERIAL_FLOATING_BOTTOM_BAR,
        config.materialFloatingBottomBar.toString(),
    )
    storage.putString(StorageKeys.THEME_FLOATING_BOTTOM_BAR_STYLE, config.floatingBottomBarStyle.storageValue)
    storage.putString(StorageKeys.THEME_BOTTOM_BAR_MODE, config.bottomBarMode.storageValue)
    storage.putString(StorageKeys.THEME_PAGER_GESTURE_MODE, config.pagerInterceptionMode.name)
    storage.putString(StorageKeys.THEME_DENSITY_SCALE, normalizeDensityScale(config.densityScale).toString())
    storage.putString(StorageKeys.THEME_ANIMATION_SPEED, normalizeAnimationSpeed(config.animationSpeed).toString())
    storage.putString(StorageKeys.THEME_PROXY_GRID_SEGMENTED, config.proxyGridSegmented.toString())
}

private fun pagerInterceptionModeFromStorage(value: String): PagerInterceptionMode =
    PagerInterceptionMode.entries.firstOrNull { it.name == value } ?: PagerInterceptionMode.CrossAxisInterceptor
