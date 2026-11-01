package com.linx3141.mexpa.ui.theme

import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode

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
