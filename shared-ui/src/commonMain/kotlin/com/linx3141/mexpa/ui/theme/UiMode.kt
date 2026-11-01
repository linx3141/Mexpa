package com.linx3141.mexpa.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局界面风格。默认 [Miuix]，保持既有外观不变；[Material] 为 Material 3 Expressive。
 *
 * 只在组合根（[MishkaTheme]）切换一次，页面/组件通过 [LocalUiMode] 决定使用哪套实现，
 * 与 KernelSU 的 `UiMode`/`LocalUiMode` 同构。
 */
enum class UiMode(val storageValue: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        val Default: UiMode = Miuix

        fun fromStorage(value: String?): UiMode =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Default }
