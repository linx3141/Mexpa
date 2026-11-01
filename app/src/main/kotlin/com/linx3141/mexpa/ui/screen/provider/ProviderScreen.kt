package com.linx3141.mexpa.ui.screen.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.ProviderViewModel

@Composable
fun ProviderScreen(
    viewModel: ProviderViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProviderScreenContent(
        uiState = uiState,
        onBack = onBack,
        onUpdateAll = { viewModel.updateAll() },
        onUpdateProvider = { name, isRule -> viewModel.updateProvider(name, isRule) },
    )
}
