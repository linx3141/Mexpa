package com.linx3141.mexpa.ui.screen.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionEditScreen(
    uuid: String,
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onNavigateOverrides: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionEditScreenContent(
        uuid = uuid,
        uiState = uiState,
        onBack = onBack,
        onSaved = onSaved,
        onNavigateOverrides = onNavigateOverrides,
        onEditSubscription = { id, name, source, interval, userAgent, ageSecretKey, onComplete ->
            viewModel.editSubscription(id, name, source, interval, userAgent, ageSecretKey, onComplete)
        },
        onCancelUpdate = { viewModel.cancelCurrentUpdate() },
    )
}
