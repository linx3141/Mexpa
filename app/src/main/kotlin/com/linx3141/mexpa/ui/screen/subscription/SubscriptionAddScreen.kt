package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.groupedCardItems
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

/**
 * 创建配置 —— 选择添加方式（文件 / URL）
 */
@Composable
fun SubscriptionAddScreen(
    viewModel: SubscriptionViewModel? = null,
    onBack: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onNavigateUrl: () -> Unit = {},
    onScanQR: (() -> Unit)? = null,
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val uiState by viewModel?.uiState?.collectAsStateWithLifecycle()
        ?: return

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.subscription_create),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            groupedCardItems(
                keyPrefix = "subscription_add",
                outerTopPadding = 12.dp,
                outerBottomPadding = 0.dp,
                items = buildList {
                    add(CardItem("file") {
                        ArrowPreference(
                            title = stringResource(R.string.subscription_file),
                            summary = stringResource(R.string.subscription_file_summary),
                            enabled = !uiState.isLoading,
                            onClick = onPickFile,
                        )
                    })
                    add(CardItem("url") {
                        ArrowPreference(
                            title = stringResource(R.string.subscription_url),
                            summary = stringResource(R.string.subscription_url_summary),
                            enabled = !uiState.isLoading,
                            onClick = onNavigateUrl,
                        )
                    })
                    if (onScanQR != null) {
                        add(CardItem("qr") {
                            ArrowPreference(
                                title = stringResource(R.string.subscription_qr),
                                summary = stringResource(R.string.subscription_qr_summary),
                                enabled = !uiState.isLoading,
                                onClick = onScanQR,
                            )
                        })
                    }
                },
            )
            if (uiState.error.isNotEmpty()) {
                item(key = "error") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(top = 12.dp),
                        insideMargin = PaddingValues(16.dp),
                    ) {
                        Text(
                            text = uiState.error,
                            fontSize = 14.sp,
                            color = MishkaColors.primary,
                        )
                    }
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

    ImportProgressDialog(
        show = uiState.isLoading,
        step = uiState.importProgress?.let { importStepLabel(it) } ?: stringResource(R.string.common_processing),
        onCancel = { viewModel.cancelCurrentUpdate() },
    )
}
