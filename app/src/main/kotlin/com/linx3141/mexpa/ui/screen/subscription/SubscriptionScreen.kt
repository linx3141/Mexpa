package com.linx3141.mexpa.ui.screen.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

/** Android 适配层：收集 SubscriptionViewModel，布局在 shared-ui 的 SubscriptionScreenContent。 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    bottomPadding: Dp = 0.dp,
    onNavigateAdd: () -> Unit = {},
    onNavigateEdit: (uuid: String) -> Unit = {},
    onActiveChanged: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionScreenContent(
        bottomPadding = bottomPadding,
        uiState = uiState,
        onNavigateAdd = onNavigateAdd,
        onNavigateEdit = onNavigateEdit,
        onUpdateAll = { viewModel.updateAllSubscriptions() },
        onClearError = { viewModel.clearError() },
        onSetActive = { id ->
            viewModel.setActive(id)
            onActiveChanged?.invoke()
        },
        onRefreshSubscription = { id -> viewModel.fetchSubscription(id) },
        onRemoveSubscription = { id ->
            viewModel.removeSubscription(id) { onActiveChanged?.invoke() }
        },
        onCancelUpdate = { viewModel.cancelCurrentUpdate() },
    )
}
