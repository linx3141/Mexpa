package com.linx3141.mexpa.ui.screen.overrides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel

@Composable
fun OverrideListScreen(
    viewModel: OverrideProfileViewModel,
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onEditFile: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OverrideListScreenContent(
        state = state,
        onBack = onBack,
        onAdd = onAdd,
        onEdit = onEdit,
        onEditFile = onEditFile,
        onUpdate = { id -> viewModel.update(id) },
        onDelete = { id -> viewModel.delete(id) },
        onClearError = { viewModel.clearError() },
        onUpdateAll = { viewModel.updateAll() },
    )
}
