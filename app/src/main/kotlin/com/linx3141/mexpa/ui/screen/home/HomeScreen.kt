package com.linx3141.mexpa.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.BuildConfig
import com.linx3141.mexpa.util.formatUptime
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.viewmodel.HomeViewModel
import com.linx3141.mexpa.viewmodel.MemorySnapshot
import com.linx3141.mexpa.viewmodel.SpeedSnapshot
import com.linx3141.mexpa.viewmodel.SystemInfoSnapshot

/** Android 适配层：收集 HomeViewModel 的高频 Flow，布局本身在 shared-ui 的 HomeContent。 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    uiState: HomeUiState = HomeUiState(),
    viewModel: HomeViewModel? = null,
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
) {
    val speed by (viewModel?.speedState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(SpeedSnapshot()) })
    val memory by (viewModel?.memoryState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(MemorySnapshot()) })
    val systemInfo by (viewModel?.systemInfoState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(SystemInfoSnapshot()) })
    val uptimeSeconds by (viewModel?.uptimeState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(-1L) })
    val topConnectionRates by (
        viewModel?.topConnectionRates?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(null) }
    )
    val uptime = formatUptime(uptimeSeconds)

    HomeContent(
        modifier = modifier,
        bottomPadding = bottomPadding,
        uiState = uiState,
        speed = speed,
        memory = memory,
        systemInfo = systemInfo,
        uptimeSeconds = uptimeSeconds,
        topConnectionRates = topConnectionRates,
        fallbackMihomoVersion = BuildConfig.MIHOMO_VERSION,
        onStartConnectionRateTracking = { viewModel?.startConnectionRateTracking() },
        onStopConnectionRateTracking = { viewModel?.stopConnectionRateTracking() },
        onRefreshProviderTraffic = { viewModel?.refreshProviderTraffic() },
        onUpdateAllProviders = { viewModel?.updateAllProviders() },
        onRestart = onRestart,
        onStop = onStop,
        onReload = onReload,
        onTestLatency = onTestLatency,
        onNavigateLog = onNavigateLog,
        onNavigateProvider = onNavigateProvider,
        onNavigateConnection = onNavigateConnection,
        onNavigateDnsQuery = onNavigateDnsQuery,
        onStartProxy = onStartProxy,
        onSwitchMode = onSwitchMode,
        onSwitchTunStack = onSwitchTunStack,
    )
}
