package com.linx3141.mexpa.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.HintHorizontalPadding
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode

/**
 * AOSP Settings 风格的页脚说明：Material 下直接铺在背景上，
 * 上方是圆形感叹号图标，正文使用压暗的 onSurfaceVariant；Miuix 保留原卡片样式。
 */
@Composable
fun FooterHint(
    texts: List<String>,
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Material) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = HintHorizontalPadding)
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            texts.forEachIndexed { index, text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (index != texts.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Column {
                texts.forEach { text ->
                    Text(
                        text = text,
                        color = MishkaColors.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
