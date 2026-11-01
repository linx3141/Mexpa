package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.AutomationViewModel

@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    AutomationScreenContent(
        uiState = uiState,
        onBack = onBack,
        onAllowedChange = { viewModel.setAllowed(it) },
    )
}
