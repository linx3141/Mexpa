package com.linx3141.mexpa.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalTextApi::class)
actual val AppFontFamily: FontFamily
    get() = if (System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)) {
        FontFamily("Microsoft YaHei UI")
    } else {
        FontFamily.Default
    }
