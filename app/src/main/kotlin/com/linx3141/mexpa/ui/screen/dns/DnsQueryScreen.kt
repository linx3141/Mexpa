package com.linx3141.mexpa.ui.screen.dns

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.DnsQueryViewModel

@Composable
fun DnsQueryScreen(
    viewModel: DnsQueryViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DnsQueryScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSetQueryName = { viewModel.setQueryName(it) },
        onQuery = { viewModel.queryDns() },
        onSetQueryType = { viewModel.setQueryType(it) },
        onFlushCache = { viewModel.flushDnsCache() },
        onFlushFakeIp = { viewModel.flushFakeIp() },
    )
}
