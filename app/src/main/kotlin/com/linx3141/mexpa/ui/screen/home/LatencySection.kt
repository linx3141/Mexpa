package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.theme.UiMode
import com.linx3141.mexpa.util.FormatUtils
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun LazyListScope.latencySection(
    state: HomeUiState = HomeUiState(),
    onTestLatency: () -> Unit = {},
) {
    item(key = "latency_title") {
        LatencyHeader(state, onTestLatency)
    }
    item(key = "latency") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeCardHorizontalPadding)
                .padding(bottom = 6.dp),
            insideMargin = PaddingValues(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LatencyItem("Baidu", state.latencyBaidu)
                LatencyItem("Cloudflare", state.latencyCloudflare)
                LatencyItem("Google", state.latencyGoogle)
            }
        }
    }
}

@Composable
private fun LatencyHeader(
    state: HomeUiState,
    onTestLatency: () -> Unit,
) {
    val allTested = state.latencyBaidu >= 0 || state.latencyCloudflare >= 0 || state.latencyGoogle >= 0
    val statusColor = when {
        !allTested -> StatusColors.neutral
        state.latencyGoogle >= 0 -> StatusColors.healthy
        state.latencyCloudflare >= 0 -> StatusColors.warning
        else -> StatusColors.danger
    }
    val statusText = when {
        !allTested -> stringResource(R.string.home_latency_untested)
        state.latencyGoogle >= 0 -> stringResource(R.string.home_latency_normal)
        state.latencyCloudflare >= 0 -> stringResource(R.string.home_latency_partial)
        else -> stringResource(R.string.home_latency_abnormal)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallTitle(text = stringResource(R.string.home_latency))
        Row(
            modifier = Modifier.padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 本轮未经规则引擎，明确标注而非静默给数字
            if (state.isRunning && !state.latencyViaRules) {
                Text(
                    text = stringResource(R.string.home_latency_not_via_rules),
                    fontSize = 12.sp,
                    color = StatusColors.warning,
                )
            }
            if (state.isRunning) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = statusColor,
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                IconButton(
                    onClick = onTestLatency,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Refresh,
                        contentDescription = stringResource(R.string.common_refresh),
                        modifier = Modifier.size(16.dp),
                        tint = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LatencyItem(name: String, delay: Int) {
    // -1 视为"未测/不可达"，由 token 映射到 neutral 灰
    val color = StatusColors.delay(if (delay < 0) null else delay)
    val isMaterial = LocalUiMode.current == UiMode.Material

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isMaterial) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MishkaColors.onSurfaceVariantSummary,
                )
            } else {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    color = MishkaColors.onSurfaceVariantSummary,
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        if (isMaterial) {
            Text(
                text = FormatUtils.formatLatency(delay),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MishkaColors.onSurface,
            )
        } else {
            Text(
                text = FormatUtils.formatLatency(delay),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MishkaColors.onSurface,
            )
        }
    }
}
