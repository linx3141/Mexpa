package com.linx3141.mexpa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive 的排版。默认只覆盖 bodyLarge，其余沿用 Material 3 默认值；
 * 集中在此处便于后续按需调整，避免散落在页面里。
 */
val MishkaMaterialTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
