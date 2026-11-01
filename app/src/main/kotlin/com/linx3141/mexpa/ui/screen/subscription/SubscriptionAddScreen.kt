package com.linx3141.mexpa.ui.screen.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionAddScreen(
    viewModel: SubscriptionViewModel? = null,
    onBack: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onNavigateUrl: () -> Unit = {},
    onScanQR: (() -> Unit)? = null,
) {
    val uiState by viewModel?.uiState?.collectAsStateWithLifecycle() ?: return
    SubscriptionAddScreenContent(
        uiState = uiState,
        onBack = onBack,
        onPickFile = onPickFile,
        onNavigateUrl = onNavigateUrl,
        onScanQR = onScanQR,
        onCancelUpdate = { viewModel?.cancelCurrentUpdate() },
    )
}
