package com.linx3141.mexpa.ui.component

import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.HintHorizontalPadding
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页顶部提示：Material 直接铺在背景上，Miuix 保持卡片。
 */
@Composable
fun RestartRequiredHint(
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = stringResource(R.string.settings_override_restart_hint),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = HintHorizontalPadding)
                .padding(top = 12.dp, bottom = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MishkaColors.onSurface,
        )
    } else {
        HintCard(
            text = stringResource(R.string.settings_override_restart_hint),
            modifier = modifier.padding(top = 12.dp),
            bottomPadding = 6.dp,
        )
    }
}

@Composable
fun HintCard(
    text: String,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 12.dp,
) {
    if (LocalUiMode.current == UiMode.Material) {
        Text(
            text = text,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = HintHorizontalPadding)
                .padding(bottom = bottomPadding),
            style = MaterialTheme.typography.bodyMedium,
            color = MishkaColors.onSurface,
        )
        return
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomPadding),
    ) {
        Text(
            text = text,
            color = MishkaColors.onSurfaceVariantSummary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
