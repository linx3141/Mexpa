package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.R
import com.linx3141.mexpa.domain.model.Subscription
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.util.WideContentBox
import com.linx3141.mexpa.util.FormatUtils
import com.linx3141.mexpa.util.formatEpochMillisAsLocal
import com.linx3141.mexpa.viewmodel.ProfileOperation
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.HorizontalDivider
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.linx3141.mexpa.ui.component.design.WindowDialog

/**
 * 订阅列表页面
 */
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    bottomPadding: Dp = 0.dp,
    onNavigateAdd: () -> Unit = {},
    onNavigateEdit: (uuid: String) -> Unit = {},
    onActiveChanged: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = rememberMishkaScrollBehavior()

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.subscription_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {},
                    actions = {
                        if (uiState.subscriptions.any { it.url.isNotBlank() }) {
                            IconButton(
                                onClick = { viewModel.updateAllSubscriptions() },
                                enabled = !uiState.isLoading,
                            ) {
                                Icon(
                                    imageVector = AppIcons.Refresh,
                                    contentDescription = stringResource(R.string.subscription_update_all),
                                    tint = MishkaColors.onSurface,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.clearError(); onNavigateAdd() }) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = stringResource(R.string.subscription_add),
                                tint = MishkaColors.onSurface,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding,
                ),
            ) {
                if (uiState.subscriptions.isNotEmpty()) {
                    item(key = "top_padding") {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (uiState.error.isNotEmpty()) {
                    item(key = "error") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageHorizontalPadding)
                                .padding(bottom = 12.dp),
                            insideMargin = PaddingValues(16.dp),
                        ) {
                            Text(
                                text = uiState.error,
                                color = StatusColors.danger,
                            )
                        }
                    }
                }

                if (uiState.subscriptions.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier.fillParentMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.subscription_no_config),
                                fontSize = 16.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                            Text(
                                text = stringResource(R.string.subscription_tap_add),
                                modifier = Modifier.padding(top = 6.dp),
                                fontSize = 14.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                        }
                    }
                }

                items(uiState.subscriptions, key = { it.id }) { sub ->
                    SubscriptionItem(
                        subscription = sub,
                        isLoading = uiState.isLoading,
                        onSelect = {
                            viewModel.setActive(sub.id)
                            onActiveChanged?.invoke()
                        },
                        onRefresh = { viewModel.fetchSubscription(sub.id) },
                        onDelete = {
                            viewModel.removeSubscription(sub.id) { onActiveChanged?.invoke() }
                        },
                        onEdit = { onNavigateEdit(sub.id) },
                    )
                }
            }
        }
    }

    // 批量更新优先展示（含 currentName + (done/total) + step）；
    // 单条走 importProgress，标题由 operation 区分（列表页触发的是 Update）。
    val updateAll = uiState.updateAll
    if (updateAll != null) {
        val progressText = stringResource(
            R.string.subscription_updating_progress,
            updateAll.currentName,
            updateAll.completed + 1,
            updateAll.total,
        )
        val step = updateAll.currentStep?.let { "$progressText\n${importStepLabel(it)}" } ?: progressText
        ImportProgressDialog(
            show = true,
            step = step,
            title = stringResource(R.string.subscription_updating_title),
            onCancel = { viewModel.cancelCurrentUpdate() },
        )
    } else {
        val title = when (uiState.operation) {
            ProfileOperation.Update -> stringResource(R.string.subscription_update_config)
            else -> stringResource(R.string.subscription_import_config)
        }
        ImportProgressDialog(
            show = uiState.importProgress != null,
            step = uiState.importProgress?.let { importStepLabel(it) } ?: stringResource(R.string.common_processing),
            title = title,
            onCancel = { viewModel.cancelCurrentUpdate() },
        )
    }
}

@Composable
private fun SubscriptionItem(
    subscription: Subscription,
    isLoading: Boolean,
    onSelect: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val displayName = subscription.name.ifBlank { stringResource(R.string.subscription_config) }
    // M3E 用成对的 container/onContainer；Miuix 保持原来的配色不变
    val isMaterial = LocalUiMode.current == UiMode.Material

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageHorizontalPadding)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
        onClick = onSelect,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = subscription.name.ifBlank { stringResource(R.string.subscription_config) },
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight(550),
                color = MishkaColors.onSurface,
            )
            if (subscription.isActive) {
                val activeColor = StatusColors.healthy
                Text(
                    text = stringResource(R.string.subscription_in_use),
                    style = if (isMaterial) MaterialTheme.typography.labelMedium else androidx.compose.ui.text.TextStyle.Default,
                    fontSize = if (isMaterial) androidx.compose.ui.unit.TextUnit.Unspecified else 12.sp,
                    fontWeight = FontWeight(750),
                    color = activeColor,
                    modifier = Modifier
                        .themeBadgeBackground(activeColor.copy(alpha = 0.15f), 6.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row {

            Column(
                modifier = Modifier.wrapContentSize()
            ) {
                if (subscription.total > 0) {
                    val used = subscription.upload + subscription.download
                    Text(
                        text = stringResource(
                            R.string.subscription_used_traffic,
                            FormatUtils.formatBytes(used),
                            FormatUtils.formatBytes(subscription.total)
                        ),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.subscription_no_traffic),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (subscription.updatedAt > 0) {
                    Text(
                        text = stringResource(R.string.subscription_updated_at, formatEpochMillisAsLocal(subscription.updatedAt)),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = onEdit,
                minHeight = 35.dp,
                minWidth = if (isMaterial) 56.dp else 35.dp,
                backgroundColor = MishkaColors.secondaryContainer,
                capsule = isMaterial,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = AppIcons.MoreCircle,
                    contentDescription = stringResource(R.string.common_edit),
                    tint = if (isMaterial) MishkaColors.onSecondaryContainer else MishkaColors.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading && subscription.url.isNotBlank(),
                minHeight = 35.dp,
                minWidth = if (isMaterial) 56.dp else 35.dp,
                backgroundColor = MishkaColors.secondaryContainer,
                capsule = isMaterial,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = AppIcons.Refresh,
                    contentDescription = stringResource(R.string.common_update),
                    tint = if (isMaterial) MishkaColors.onSecondaryContainer else MishkaColors.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { showDeleteDialog = true },
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = if (isMaterial) MishkaColors.errorContainer else MishkaColors.secondaryContainer,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = AppIcons.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = if (isMaterial) MishkaColors.onErrorContainer else MishkaColors.onSurfaceVariantSummary,
                )
            }
        }
    }

    WindowDialog(
        show = showDeleteDialog,
        title = stringResource(R.string.subscription_delete_title),
        summary = stringResource(R.string.subscription_delete_summary, displayName),
        onDismissRequest = { showDeleteDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showDeleteDialog = false },
            )
            TextButton(
                text = stringResource(R.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    showDeleteDialog = false
                    onDelete()
                },
            )
        }
    }
}
