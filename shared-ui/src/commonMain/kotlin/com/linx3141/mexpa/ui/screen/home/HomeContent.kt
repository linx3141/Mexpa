@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)


package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.shared.resources.*


import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.util.WideContentBox
import com.linx3141.mexpa.util.formatUptime
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.viewmodel.ConnectionRate
import com.linx3141.mexpa.util.formatUptime
import com.linx3141.mexpa.viewmodel.MemorySnapshot
import com.linx3141.mexpa.viewmodel.SpeedSnapshot
import com.linx3141.mexpa.viewmodel.SystemInfoSnapshot
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    uiState: HomeUiState = HomeUiState(),
    speed: SpeedSnapshot = SpeedSnapshot(),
    memory: MemorySnapshot = MemorySnapshot(),
    systemInfo: SystemInfoSnapshot = SystemInfoSnapshot(),
    uptimeSeconds: Long = -1L,
    topConnectionRates: ImmutableList<ConnectionRate>? = null,
    fallbackMihomoVersion: String = "",
    onStartConnectionRateTracking: () -> Unit = {},
    onStopConnectionRateTracking: () -> Unit = {},
    onRefreshProviderTraffic: () -> Unit = {},
    onUpdateAllProviders: () -> Unit = {},
    onRestart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReload: () -> Unit = {},
    onTestLatency: () -> Unit = {},
    onNavigateLog: () -> Unit = {},
    onNavigateProvider: () -> Unit = {},
    onNavigateConnection: () -> Unit = {},
    onNavigateDnsQuery: () -> Unit = {},
    onStartProxy: () -> Unit = {},
    onSwitchMode: (String) -> Unit = {},
    onSwitchTunStack: (String) -> Unit = {},
    connectionCount: Int? = null,
) {

    val uptime = formatUptime(uptimeSeconds)
    val scrollBehavior = rememberMishkaScrollBehavior()
    val isMaterial = LocalUiMode.current == UiMode.Material


    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface
    var showSubscriptionTraffic by rememberSaveable { mutableStateOf(false) }
    var showSpeedDetail by rememberSaveable { mutableStateOf(false) }

    if (showSpeedDetail) {
        androidx.compose.runtime.DisposableEffect(Unit) {
            onStartConnectionRateTracking()
            onDispose { onStopConnectionRateTracking() }
        }
    }
    // 代理停止后不会再有快照可差分，速率被重置回「未就绪」，留着详情只会无限转圈
    androidx.compose.runtime.LaunchedEffect(uiState.isRunning) {
        if (!uiState.isRunning) showSpeedDetail = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.app_name),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .platformScrollEndHaptic()
                    .platformOverScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding,
                ),
            ) {
                statusSection(
                    state = uiState,
                    uptime = uptime,
                    fallbackMihomoVersion = fallbackMihomoVersion,
                    onSwitchMode = onSwitchMode,
                    onSwitchTunStack = onSwitchTunStack,
                )
                // M3E 的状态控制整体收进右下角 FAB；内容流不再有操作行
                if (!isMaterial) {
                    actionButtonsSection(
                        onRestart = onRestart,
                        onStop = onStop,
                        onReload = onReload,
                        onStart = onStartProxy,
                        isRunning = uiState.isRunning,
                        isStarting = uiState.isStarting,
                        isStopping = uiState.isStopping,
                    )
                }
                overviewCardsSection(
                    speed = speed,
                    state = uiState,
                    onSpeedClick = { showSpeedDetail = true },
                    onSubscriptionClick = {
                        showSubscriptionTraffic = true
                        onRefreshProviderTraffic()
                    },
                )
                systemCardsSection(
                    memory = memory,
                    systemInfo = systemInfo,
                )
                latencySection(uiState, onTestLatency)
                quickEntriesSection(
                    isRunning = uiState.isRunning,
                    connectionCount = connectionCount,
                    onNavigateLog = onNavigateLog,
                    onNavigateProvider = onNavigateProvider,
                    onNavigateConnection = onNavigateConnection,
                    onNavigateDnsQuery = onNavigateDnsQuery,
                )
            }
        }
    }

    SpeedDetailSheet(
        show = showSpeedDetail,
        rates = topConnectionRates,
        onDismiss = { showSpeedDetail = false },
    )

    SubscriptionTrafficDialog(
        show = showSubscriptionTraffic,
        providers = uiState.providerTraffic,
        isLoading = uiState.isProviderTrafficLoading,
        loadFailed = uiState.providerTrafficLoadFailed,
        onUpdateAll = { onUpdateAllProviders() },
        onDismiss = { showSubscriptionTraffic = false },
    )
}
