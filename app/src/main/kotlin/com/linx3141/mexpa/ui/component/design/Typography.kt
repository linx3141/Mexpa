@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * 文本与图标的主题适配。
 *
 * 参数面与 miuix 的同名组件一致（调用方只换 import），Miuix 模式逐参数委托 miuix；
 * Material 模式改用 M3 的 Text/Icon——这样文字排版、LocalContentColor 都跟随 M3 主题，
 * 而不是被嵌套的 MiuixTheme 悄悄接管。
 */

/** 把 Material 分支遗留的 12–20sp 字号映射到 M3 type scale，避免继续绕过 Typography token。 */
@Composable
private fun materialTypeStyleFor(fontSize: TextUnit): TextStyle? {
    if (!fontSize.isSpecified) return null
    return when (fontSize.value.roundToInt()) {
        12 -> MaterialTheme.typography.bodySmall
        13, 14 -> MaterialTheme.typography.bodyMedium
        15 -> MaterialTheme.typography.bodyLarge
        20 -> MaterialTheme.typography.titleLarge
        else -> null
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle? = null,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (style != null) {
            MiuixText(
                text = text,
                modifier = modifier,
                color = color,
                autoSize = autoSize,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                style = style,
            )
        } else {
            MiuixText(
                text = text,
                modifier = modifier,
                color = color,
                autoSize = autoSize,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
            )
        }
        return
    }
    val mappedStyle = if (style == null) materialTypeStyleFor(fontSize) else null
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = if (mappedStyle != null) TextUnit.Unspecified else fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        style = style ?: mappedStyle ?: if (fontSize.isSpecified || lineHeight.isSpecified) {
            TextStyle.Default
        } else {
            LocalTextStyle.current
        },
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (tint == Color.Unspecified) {
            MiuixIcon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier)
        } else {
            MiuixIcon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint,
            )
        }
        return
    }
    if (tint == Color.Unspecified) {
        MaterialIcon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier)
    } else {
        MaterialIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
    }
}

@Composable
fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (tint == Color.Unspecified) {
            MiuixIcon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier)
        } else {
            MiuixIcon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, tint = tint)
        }
        return
    }
    if (tint == Color.Unspecified) {
        MaterialIcon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier)
    } else {
        MaterialIcon(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, tint = tint)
    }
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (tint == Color.Unspecified) {
            MiuixIcon(painter = painter, contentDescription = contentDescription, modifier = modifier)
        } else {
            MiuixIcon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
        }
        return
    }
    if (tint == Color.Unspecified) {
        MaterialIcon(painter = painter, contentDescription = contentDescription, modifier = modifier)
    } else {
        MaterialIcon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
    }
}
