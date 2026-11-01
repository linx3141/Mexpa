package com.linx3141.mexpa.ui.screen.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.LogViewModel

@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    LogScreenContent(
        uiState = uiState,
        logs = logs,
        onBack = onBack,
        onConnect = { viewModel.connect() },
        onDisconnect = { viewModel.disconnect() },
        onClear = { viewModel.clearLogs() },
    )
}
