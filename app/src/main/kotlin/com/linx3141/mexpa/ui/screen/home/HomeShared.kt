package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 卡片背景水印的共享度量，[TrafficSparkline] 与 [SubscriptionUsageBar] 共用；新增水印一律复用 */
internal const val WatermarkHeightFraction = 0.52f

internal const val WatermarkFillAlpha = 0.22f

internal val WatermarkStrokeWidth = 1.5.dp

/** 首页卡片页边距：M3 标准 16dp；Miuix 保持原来的 12dp */
internal val HomeCardHorizontalPadding: Dp
    @Composable get() = PageHorizontalPadding

@Composable
internal fun BadgeLabel(text: String) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = text,
            modifier = Modifier
                .themeBadgeBackground(MishkaColors.secondaryContainer, 6.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MishkaColors.onSecondaryContainer,
        )
    } else {
        Text(
            text = text,
            modifier = Modifier
                .squircleBackground(MishkaColors.secondaryContainer, 6.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MishkaColors.primary,
        )
    }
}

@Composable
internal fun HomeCardTitle(text: String, miuixFontSize: TextUnit = 14.sp) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MishkaColors.onSurface,
        )
    } else {
        Text(
            text = text,
            fontSize = miuixFontSize,
            fontWeight = FontWeight.Medium,
            color = MishkaColors.onSurface,
        )
    }
}

@Composable
internal fun HomeCardSubtitle(text: String, miuixFontSize: TextUnit = 12.sp) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MishkaColors.onSurfaceVariantSummary,
        )
    } else {
        Text(
            text = text,
            fontSize = miuixFontSize,
            color = MishkaColors.onSurfaceVariantSummary,
        )
    }
}

@Composable
internal fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (LocalUiMode.current == UiMode.Material) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MishkaColors.onSurfaceVariantSummary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MishkaColors.onSurface,
            )
        } else {
            Text(text = label, fontSize = 13.sp, color = MishkaColors.onSurfaceVariantSummary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MishkaColors.onSurface)
        }
    }
}
