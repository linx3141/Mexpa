package com.linx3141.mexpa.ui.screen.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionAddUrlScreen(
    viewModel: SubscriptionViewModel,
    initialUrl: String = "",
    initialName: String = "",
    initialIntervalMinutes: Long = 0,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionAddUrlScreenContent(
        uiState = uiState,
        initialUrl = initialUrl,
        initialName = initialName,
        initialIntervalMinutes = initialIntervalMinutes,
        onBack = onBack,
        onAddSubscription = { name, url, interval, userAgent, ageSecretKey, onComplete ->
            viewModel.addSubscription(name, url, interval, userAgent, ageSecretKey, onComplete)
        },
        onSaved = onSaved,
        onCancelUpdate = { viewModel.cancelCurrentUpdate() },
    )
}
