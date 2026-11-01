package com.linx3141.mexpa.ui.theme

import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformMonetSeedColor(isDark: Boolean): Color? {
    val context = LocalContext.current
    return if (isDark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
}
