package com.linx3141.mexpa.ui.screen.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.ConnectionViewModel

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionScreenContent(
        uiState = uiState,
        onBack = onBack,
        onStartObserving = { viewModel.startObserving() },
        onStopObserving = { viewModel.stopObserving() },
        onCloseConnection = { id -> viewModel.closeConnection(id) },
        onCloseAllConnections = { viewModel.closeAllConnections() },
    )
}
