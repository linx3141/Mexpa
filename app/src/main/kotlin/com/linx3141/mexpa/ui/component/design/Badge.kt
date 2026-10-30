package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.squircle.squircleBackground

/**
 * 小标签/徽章的圆角背景：Material 走 M3 shape，Miuix 保留 squircle。
 * 不要把 Miuix 的 squircle 绘制带进 Material 分支。
 */
@Composable
fun Modifier.themeBadgeBackground(
    color: Color,
    miuixCornerRadius: Dp,
    materialShape: Shape = RoundedCornerShape(8.dp),
): Modifier = if (LocalUiMode.current == UiMode.Material) {
    background(color, materialShape)
} else {
    squircleBackground(color, miuixCornerRadius)
}

/** 小徽章文字：Material 走 M3 labelSmall，Miuix 保留原来的 9sp。 */
@Composable
fun BadgeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily? = FontFamily.Monospace,
    textAlign: TextAlign? = null,
) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            color = color,
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            fontSize = 9.sp,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            color = color,
        )
    }
}
