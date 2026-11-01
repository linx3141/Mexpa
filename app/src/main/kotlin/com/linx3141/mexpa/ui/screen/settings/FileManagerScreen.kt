package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

@Composable
fun FileManagerScreen(
    subscriptionViewModel: SubscriptionViewModel?,
    onBack: () -> Unit = {},
    onOpenFile: (uuid: String, relativePath: String) -> Unit = { _, _ -> },
) {
    val uiState = subscriptionViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val imported = uiState?.subscriptions.orEmpty().filter { it.imported }
    FileManagerScreenContent(
        subscriptions = imported.toPersistentList(),
        onBack = onBack,
        onOpenSubscription = { sub ->
            val files = subscriptionViewModel?.fileManager?.listImportedFiles(sub.id).orEmpty()
            val firstFile = files.firstOrNull { it == "config.yaml" } ?: files.firstOrNull()
            if (firstFile != null) onOpenFile(sub.id, firstFile)
        },
    )
}
