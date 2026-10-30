package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.BuildConfig
import com.linx3141.mexpa.R
import com.linx3141.mexpa.platform.TunMode
import com.linx3141.mexpa.ui.theme.RunState
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.WindowDropdownDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

fun LazyListScope.statusSection(
    state: HomeUiState = HomeUiState(),
    uptime: String = "",
    onSwitchMode: (String) -> Unit = {},
    onSwitchTunStack: (String) -> Unit = {},
) {
    item(key = "status") {
        StatusContent(state, uptime, onSwitchMode, onSwitchTunStack)
    }
}

@Composable
private fun StatusContent(
    state: HomeUiState,
    uptime: String,
    onSwitchMode: (String) -> Unit,
    onSwitchTunStack: (String) -> Unit,
) {
    val isRunning = state.isRunning
    val isStarting = state.isStarting
    val isStopping = state.isStopping

    val statusIcon = if (isRunning) {
        Icons.Rounded.CheckCircleOutline
    } else {
        Icons.Rounded.RemoveCircleOutline
    }
    val runState = when {
        isStarting || isStopping -> RunState.Pending
        isRunning -> RunState.Running
        else -> RunState.Stopped
    }
    val statusTint = StatusColors.runState(runState)
    val statusContainer = StatusColors.runStateContainer(runState)
    val statusText = if (isStopping) {
        stringResource(R.string.home_stopping)
    } else if (isStarting) {
        stringResource(R.string.home_starting)
    } else if (isRunning) {
        stringResource(R.string.home_running)
    } else {
        stringResource(R.string.home_stopped)
    }

    var showModeDialog by remember { mutableStateOf(false) }
    var showTunStackDialog by remember { mutableStateOf(false) }

    val isMaterial = LocalUiMode.current == UiMode.Material
    // M3E 停止态用普通卡片色；启动/停止中仍保留语义色
    val materialStopped = isMaterial && runState == RunState.Stopped
    val cardContainer = if (materialStopped) MaterialTheme.colorScheme.surfaceBright else statusContainer
    val cardContent = if (materialStopped) MaterialTheme.colorScheme.onSurface else statusTint

    val statusCard: @Composable (Modifier) -> Unit = { cardModifier ->
        if (isMaterial) {
            // KernelSU Material Home StatusCard：ListItem + 状态图标 + 标题/版本 + 右侧模式标签
            Card(
                modifier = cardModifier,
                colors = CardDefaults.defaultColors(color = cardContainer),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = statusIcon,
                            tint = cardContent,
                            contentDescription = null,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cardContent,
                        )
                    },
                    supportingContent = {
                        // 停止/启动中拿不到运行中的 mihomo version，用编译进核心的静态版本兜底
                        val displayVersion = state.version.ifBlank { BuildConfig.MIHOMO_VERSION }
                        val summary = remember(displayVersion, uptime) {
                            listOf(displayVersion, uptime)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cardContent.copy(alpha = 0.7f),
                        )
                    },
                    trailingContent = {
                        TunnelModeTag(label = tunnelModeLabel(state.tunMode))
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = cardContent,
                        leadingContentColor = cardContent,
                        trailingContentColor = cardContent,
                        supportingContentColor = cardContent.copy(alpha = 0.7f),
                    ),
                )
            }
        } else {
            Card(
                modifier = cardModifier,
                colors = CardDefaults.defaultColors(color = statusContainer),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        Icon(
                            modifier = Modifier.size(170.dp),
                            imageVector = statusIcon,
                            tint = statusTint,
                            contentDescription = null,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp),
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTint,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = state.version,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                        Text(
                            text = uptime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }

    val modeCard: @Composable (Modifier) -> Unit = { cardModifier ->
        Card(
            modifier = cardModifier,
            insideMargin = PaddingValues(16.dp),
            // 停止态也可切：切的是 override，运行中才顺带重启
            onClick = { showModeDialog = true },
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Sink,
        ) {
            Text(
                text = stringResource(R.string.home_mode),
                fontSize = 13.sp,
                color = MishkaColors.onSurfaceVariantSummary,
            )
            Text(
                text = modeLabel(state.mode),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MishkaColors.onSurface,
            )
        }
    }

    val tunCard: @Composable (Modifier) -> Unit = { cardModifier ->
        // RootTproxy 下 tun.enable=false，stack 无意义：展示 Inbound=TPROXY:port 静态信息
        val isTproxy = state.tunMode == TunMode.RootTproxy
        val canOpenTunStack = isRunning && !isTproxy
        val inboundTproxyLabel = stringResource(R.string.home_inbound_tproxy, TPROXY_INBOUND_PORT)
        Card(
            modifier = cardModifier,
            insideMargin = PaddingValues(16.dp),
            onClick = if (isMaterial) {
                if (canOpenTunStack) ({ showTunStackDialog = true }) else null
            } else {
                { if (canOpenTunStack) showTunStackDialog = true }
            },
            showIndication = canOpenTunStack,
            pressFeedbackType = if (canOpenTunStack) PressFeedbackType.Sink else PressFeedbackType.None,
        ) {
            Text(
                text = if (isTproxy) stringResource(R.string.home_inbound) else stringResource(R.string.home_tun),
                fontSize = 13.sp,
                color = MishkaColors.onSurfaceVariantSummary,
            )
            Text(
                text = if (isTproxy) inboundTproxyLabel else tunStackLabel(state.tunStack),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MishkaColors.onSurface,
            )
        }
    }

    if (isMaterial) {
        // KSU Material 首页：状态卡片全宽，代理模式 / TUN Stack 在下一行各占一半
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeCardHorizontalPadding)
                .padding(top = 12.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            statusCard(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                modeCard(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                tunCard(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = 6.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            statusCard(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                modeCard(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(Modifier.height(12.dp))
                tunCard(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }

    OptionSelectDialog(
        show = showModeDialog,
        title = stringResource(R.string.home_switch_mode),
        options = MODE_OPTIONS,
        current = state.mode,
        onSelect = onSwitchMode,
        onDismiss = { showModeDialog = false },
    )

    OptionSelectDialog(
        show = showTunStackDialog,
        title = stringResource(R.string.home_switch_tun_stack),
        options = TUN_STACK_OPTIONS,
        current = state.tunStack,
        onSelect = onSwitchTunStack,
        onDismiss = { showTunStackDialog = false },
    )
}

// ROOT TPROXY 模式 mihomo tproxy-port（与 RootTproxyApplier.TPROXY_PORT 对齐，UI 仅展示用）
private const val TPROXY_INBOUND_PORT: Int = 7895

// value（mihomo 侧小写标识）to label（展示名），同时供卡片取值与选择弹窗列举
private val MODE_OPTIONS = listOf("rule" to "Rule", "global" to "Global", "direct" to "Direct")

private val TUN_STACK_OPTIONS = listOf("mixed" to "Mixed", "gvisor" to "gVisor", "system" to "System", "mips" to "MIPS")

private fun modeLabel(mode: String): String =
    MODE_OPTIONS.firstOrNull { it.first == mode.lowercase() }?.second ?: mode.ifEmpty { "--" }

private fun tunStackLabel(stack: String): String =
    TUN_STACK_OPTIONS.firstOrNull { it.first == stack.lowercase() }?.second ?: stack.ifEmpty { "--" }

/**
 * 单选弹窗：点选即生效并自行关闭（`collapseOnSelection` 默认 true），底部只留取消。
 * 选中项的高亮底色 + 勾选走 [DropdownDefaults.dialogDropdownColors]。
 */
@Composable
private fun OptionSelectDialog(
    show: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    val selected = current.lowercase()
    // onSelect 不进 key：调用点每次重组都可能给出新 lambda，会让整个 entry 白重建
    val entry = remember(options, selected) {
        DropdownEntry(
            options.map { (value, label) ->
                DropdownItem(
                    text = label,
                    selected = value == selected,
                    onClick = { currentOnSelect(value) },
                )
            },
        )
    }

    WindowDropdownDialog(
        entry = entry,
        title = title,
        dialogButtonString = stringResource(R.string.common_cancel),
        show = show,
        onDismiss = onDismiss,
        onDismissFinished = {},
        dropdownColors = DropdownDefaults.dialogDropdownColors(),
    )
}

@Composable
private fun TunnelModeTag(label: String) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp),
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun tunnelModeLabel(mode: TunMode): String = when (mode) {
    TunMode.Vpn -> "VPN"
    TunMode.RootTun -> "ROOT TUN"
    TunMode.RootTproxy -> "ROOT TPROXY"
}
