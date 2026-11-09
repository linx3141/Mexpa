package com.linx3141.mexpa.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局界面风格。默认 [Material]（Material 3 Expressive）；[Miuix] 为用户可选的替代外观。
 *
 * 只在组合根（[MishkaTheme]）切换一次，页面/组件通过 [LocalUiMode] 决定使用哪套实现，
 * 与 KernelSU 的 `UiMode`/`LocalUiMode` 同构。
 */
enum class UiMode(val storageValue: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        val Default: UiMode = Material

        fun fromStorage(value: String?): UiMode =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Default }
