package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.R
import com.linx3141.mexpa.platform.showToast
import com.linx3141.mexpa.viewmodel.ExternalControlViewModel

@Composable
fun ExternalControlScreen(
    viewModel: ExternalControlViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val resetDoneMsg = stringResource(R.string.dialog_reset_done)
    ExternalControlScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSetExternalController = { viewModel.setExternalController(it) },
        onSetSecret = { viewModel.setSecret(it) },
        onResetDone = { showToast(resetDoneMsg) },
    )
}
