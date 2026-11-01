package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import com.linx3141.mexpa.platform.NetworkAddressType
import com.linx3141.mexpa.ui.util.sheetContentSafePadding
import com.linx3141.mexpa.ui.util.sheetHeightTransition
import com.linx3141.mexpa.util.FormatUtils
import com.linx3141.mexpa.util.formatIsoTimeRelative
import com.linx3141.mexpa.viewmodel.MemorySnapshot
import com.linx3141.mexpa.viewmodel.ProviderTrafficInfo
import com.linx3141.mexpa.viewmodel.SystemInfoSnapshot
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.CardDefaults
import com.linx3141.mexpa.ui.component.design.CircularProgressIndicator
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Refresh
import com.linx3141.mexpa.ui.component.design.BadgeText
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowBottomSheet
import kotlin.math.roundToInt
import kotlin.time.Instant

/** 「概览」分组第二行：本机 IP / 网卡 + 系统占用；标题由 [overviewCardsSection] 的第一行统一给出 */
fun LazyListScope.systemCardsSection(
    memory: MemorySnapshot = MemorySnapshot(),
    systemInfo: SystemInfoSnapshot = SystemInfoSnapshot(),
) {
    item(key = "system_cards") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeCardHorizontalPadding)
                .padding(vertical = 6.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeCardTitle("IP")
                    BadgeLabel(
                        when (systemInfo.addressType) {
                            NetworkAddressType.Unknown -> "--"
                            NetworkAddressType.Tun -> "TUN"
                            NetworkAddressType.Lan -> "LAN"
                            NetworkAddressType.Wan -> "WAN"
                        },
                    )
                }
                InfoRow(stringResource(Res.string.home_address), systemInfo.localIp, Modifier.padding(top = 8.dp))
                InfoRow(stringResource(Res.string.home_interface), systemInfo.interfaceName, Modifier.padding(top = 4.dp))
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeCardTitle(stringResource(Res.string.home_system))
                    BadgeLabel("SYS")
                }
                InfoRow(
                    "CPU",
                    systemInfo.cpuUsage,
                    Modifier.padding(top = 8.dp),
                )
                InfoRow(
                    "RAM",
                    memory.ramUsage,
                    Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun SubscriptionTrafficDialog(
    show: Boolean,
    providers: ImmutableList<ProviderTrafficInfo>,
    isLoading: Boolean,
    loadFailed: Boolean,
    onUpdateAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = stringResource(Res.string.home_subscription_provider_traffic),
        onDismissRequest = onDismiss,
        startAction = {
            // 打开弹窗时已自动 GET 重读快照；此按钮触发真实更新（会消耗订阅服务器配额）
            IconButton(onClick = onUpdateAll, enabled = !isLoading) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = stringResource(Res.string.home_provider_update_all),
                    tint = MishkaColors.onBackground,
                )
            }
        },
        endAction = {
            val dismiss = LocalDismissState.current
            IconButton(onClick = { dismiss?.invoke() }) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = MishkaColors.onBackground,
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .sheetContentSafePadding()
                .heightIn(min = 200.dp)
                .sheetHeightTransition(),
        ) {
            when {
                // 刷新期间隐藏旧数据，只显示加载指示器，成功后再展示新内容
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                loadFailed -> ProviderTrafficMessage(
                    text = stringResource(Res.string.home_subscription_provider_traffic_load_failed),
                    modifier = Modifier.align(Alignment.Center),
                )

                providers.isEmpty() -> ProviderTrafficMessage(
                    text = stringResource(Res.string.home_subscription_no_provider_traffic),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = providers,
                        key = { it.id },
                    ) { provider ->
                        ProviderTrafficCard(provider)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderTrafficMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        color = MishkaColors.onSurfaceVariantSummary,
    )
}

@Composable
private fun ProviderTrafficCard(provider: ProviderTrafficInfo) {
    val upload = provider.upload.coerceAtLeast(0)
    val download = provider.download.coerceAtLeast(0)
    val total = provider.total.coerceAtLeast(0)
    val used = if (Long.MAX_VALUE - upload < download) Long.MAX_VALUE else upload + download
    val remaining = (total - used).coerceAtLeast(0)
    // total<=0 的语义见 HomeViewModel.activeSubscriptionInfo：剩余/总量/进度显示 "--"
    val hasQuota = total > 0
    val progress = if (hasQuota) {
        (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = if (hasQuota) "${(progress * 100).roundToInt()}%" else "--"
    val updatedText = remember(provider.updatedAt) { formatIsoTimeRelative(provider.updatedAt) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = provider.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MishkaColors.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (provider.nodeCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    NodeCountBadge(provider.nodeCount)
                }
            }
            Spacer(Modifier.width(8.dp))
            if (updatedText.isNotEmpty()) {
                Text(
                    text = updatedText,
                    fontSize = 12.sp,
                    color = MishkaColors.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
            if (provider.hasTraffic) {
                Spacer(Modifier.width(6.dp))
                UsageBadge(percent)
            }
        }

        if (provider.hasTraffic) {
            TrafficDetailPanel(
                progress = progress.takeIf { hasQuota },
                used = FormatUtils.formatBytes(used),
                remaining = if (hasQuota) FormatUtils.formatBytes(remaining) else "--",
                total = if (hasQuota) FormatUtils.formatBytes(total) else "--",
                expire = expireText(provider.expire),
            )
        }
    }
}

@Composable
private fun NodeCountBadge(count: Int) {
    BadgeText(
        text = count.toString(),
        modifier = Modifier
            .themeBadgeBackground(MishkaColors.onSurface.copy(alpha = 0.08f), 3.dp)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        color = MishkaColors.onSurfaceVariantSummary,
    )
}

@Composable
private fun TrafficDetailPanel(
    progress: Float?,
    used: String,
    remaining: String,
    total: String,
    expire: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 14.dp,
        ),
        colors = CardDefaults.defaultColors(
            // onSurface 低透明度叠加在 sheet 背景上，深浅色均得到轻微对比的面板底色
            color = MishkaColors.onSurface.copy(alpha = 0.04f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (progress != null) {
                TrafficProgressBar(progress)
            }
            UsedRemainingRow(
                used = used,
                remaining = remaining,
            )
            TotalExpireRow(
                total = total,
                expire = expire,
            )
        }
    }
}

@Composable
private fun UsedRemainingRow(
    used: String,
    remaining: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrafficMetaLabel(stringResource(Res.string.home_used))
            TrafficMetaValue(used)
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(MishkaColors.onSurface.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrafficMetaLabel(stringResource(Res.string.home_remaining))
            TrafficMetaValue(remaining)
        }
    }
}

@Composable
private fun TotalExpireRow(
    total: String,
    expire: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(Res.string.home_total)} $total",
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MishkaColors.onSurfaceVariantSummary,
        )
        Text(
            text = expire,
            modifier = Modifier.weight(0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            fontSize = 12.sp,
            color = MishkaColors.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun TrafficMetaLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        fontSize = 12.sp,
        color = MishkaColors.onSurfaceVariantSummary,
    )
}

@Composable
private fun TrafficMetaValue(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MishkaColors.onSurface,
    )
}

@Composable
private fun UsageBadge(text: String) {
    val isMaterial = LocalUiMode.current == UiMode.Material
    Text(
        text = text,
        modifier = Modifier
            .themeBadgeBackground(MishkaColors.primary.copy(alpha = 0.12f), 6.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = if (isMaterial) MaterialTheme.typography.labelMedium else androidx.compose.ui.text.TextStyle.Default,
        fontSize = if (isMaterial) androidx.compose.ui.unit.TextUnit.Unspecified else 12.sp,
        fontWeight = FontWeight.Medium,
        color = MishkaColors.primary,
    )
}

@Composable
private fun TrafficProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(50))
            .background(MishkaColors.onSurface.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MishkaColors.primary),
        )
    }
}

/** 完整的到期展示文本：无到期时间时不带「到期」前缀，有日期时为「到期 yyyy-MM-dd HH:mm」 */
@Composable
private fun expireText(expire: Long): String {
    if (expire <= 0) return stringResource(Res.string.home_no_expire)
    val formatted = runCatching {
        Instant.fromEpochSeconds(expire)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .formatDateTime()
    }.getOrNull() ?: stringResource(Res.string.home_expire_unknown)
    return "${stringResource(Res.string.home_expire)} $formatted"
}

private fun LocalDateTime.formatDateTime(): String = buildString {
    append(year.toString().padStart(4, '0'))
    append('-')
    append(month.number.toString().padStart(2, '0'))
    append('-')
    append(day.toString().padStart(2, '0'))
    append(' ')
    append(hour.toString().padStart(2, '0'))
    append(':')
    append(minute.toString().padStart(2, '0'))
}
