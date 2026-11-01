package com.linx3141.mexpa.ui.screen.overrides

import androidx.compose.runtime.Composable
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel

@Composable
fun ClearOverrideErrorOnExit(viewModel: OverrideProfileViewModel) {
    ClearOverrideErrorOnExit { viewModel.clearError() }
}
