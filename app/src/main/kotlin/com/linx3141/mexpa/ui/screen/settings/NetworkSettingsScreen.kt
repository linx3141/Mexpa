package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.NetworkSettingsViewModel

@Composable
fun NetworkSettingsScreen(
    viewModel: NetworkSettingsViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    NetworkSettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onUpdate = { transform -> viewModel.update(transform) },
        onUpdateDns = { transform -> viewModel.updateDns(transform) },
    )
}
