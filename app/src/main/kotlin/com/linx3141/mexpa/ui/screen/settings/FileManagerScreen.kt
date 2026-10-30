package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun FileManagerScreen(
    subscriptionViewModel: SubscriptionViewModel?,
    onBack: () -> Unit = {},
    onOpenFile: (uuid: String, relativePath: String) -> Unit = { _, _ -> },
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val uiState = subscriptionViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val importedSubs = uiState?.subscriptions.orEmpty().filter { it.imported }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.file_manager_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            if (importedSubs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.file_manager_empty),
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalCutoutPadding()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                    ),
                ) {
                    item(key = "top_spacer", contentType = "spacer") {
                        Spacer(Modifier.height(12.dp))
                    }
                    items(importedSubs, key = { it.id }) { sub ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageHorizontalPadding)
                                .padding(bottom = 12.dp),
                        ) {
                            ArrowPreference(
                                title = sub.name.ifBlank { sub.id.take(8) },
                                summary = sub.id,
                                onClick = {
                                    val files = subscriptionViewModel?.fileManager?.listImportedFiles(sub.id).orEmpty()
                                    val firstFile = files.firstOrNull { it == "config.yaml" } ?: files.firstOrNull()
                                    if (firstFile != null) {
                                        onOpenFile(sub.id, firstFile)
                                    }
                                },
                            )
                        }
                    }
                    item {
                        Spacer(
                            Modifier
                                .height(24.dp)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
