package com.linx3141.mexpa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive 的排版。字体家族走平台入口（Windows 用 Microsoft YaHei UI），
 * 其余字号/字重集中在这里，避免散落到页面。
 */
private fun Typography.withAppFont(): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = labelSmall.copy(fontFamily = AppFontFamily),
)

val MishkaMaterialTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
).withAppFont()
