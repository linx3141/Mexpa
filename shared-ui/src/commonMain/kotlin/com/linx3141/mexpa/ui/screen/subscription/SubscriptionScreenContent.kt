package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.shared.resources.*



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
import com.linx3141.mexpa.ui.component.design.LazyColumn
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.Subscription
import com.linx3141.mexpa.viewmodel.SubscriptionUiState
import com.linx3141.mexpa.viewmodel.ProfileOperation
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.util.WideContentBox
import com.linx3141.mexpa.util.FormatUtils
import com.linx3141.mexpa.util.formatEpochMillisAsLocal
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
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

/**
 * 订阅列表页面
 */
@Composable
fun SubscriptionScreenContent(
    bottomPadding: Dp = 0.dp,
    uiState: SubscriptionUiState = SubscriptionUiState(),
    onNavigateAdd: () -> Unit = {},
    onNavigateEdit: (uuid: String) -> Unit = {},
    onUpdateAll: () -> Unit = {},
    onClearError: () -> Unit = {},
    onSetActive: (String) -> Unit = {},
    onRefreshSubscription: (String) -> Unit = {},
    onRemoveSubscription: (String) -> Unit = {},
    onCancelUpdate: () -> Unit = {},
    canAdd: Boolean = true,
    canUpdate: Boolean = true,
    canEdit: Boolean = true,
    canRefresh: Boolean = true,
    canDelete: Boolean = true,
) {

    val scrollBehavior = rememberMishkaScrollBehavior()

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.subscription_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {},
                    actions = {
                        if (canUpdate && uiState.subscriptions.any { it.url.isNotBlank() }) {
                            IconButton(
                                onClick = { onUpdateAll() },
                                enabled = !uiState.isLoading,
                            ) {
                                Icon(
                                    imageVector = AppIcons.Refresh,
                                    contentDescription = stringResource(Res.string.subscription_update_all),
                                    tint = MishkaColors.onSurface,
                                )
                            }
                        }
                        if (canAdd) IconButton(onClick = { onClearError(); onNavigateAdd() }) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = stringResource(Res.string.subscription_add),
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
                    .platformScrollEndHaptic()
                    .platformOverScrollVertical()
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
                                text = stringResource(Res.string.subscription_no_config),
                                fontSize = 16.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                            Text(
                                text = stringResource(Res.string.subscription_tap_add),
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
                            onSetActive(sub.id)
                        },
                        onRefresh = { onRefreshSubscription(sub.id) },
                        onDelete = { onRemoveSubscription(sub.id) },
                        onEdit = { onNavigateEdit(sub.id) },
                        canEdit = canEdit,
                        canRefresh = canRefresh,
                        canDelete = canDelete,
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
            Res.string.subscription_updating_progress,
            updateAll.currentName,
            updateAll.completed + 1,
            updateAll.total,
        )
        val step = updateAll.currentStep?.let { "$progressText\n${importStepLabel(it)}" } ?: progressText
        ImportProgressDialog(
            show = true,
            step = step,
            title = stringResource(Res.string.subscription_updating_title),
            onCancel = { onCancelUpdate() },
        )
    } else {
        val title = when (uiState.operation) {
            ProfileOperation.Update -> stringResource(Res.string.subscription_update_config)
            else -> stringResource(Res.string.subscription_import_config)
        }
        ImportProgressDialog(
            show = uiState.importProgress != null,
            step = uiState.importProgress?.let { importStepLabel(it) } ?: stringResource(Res.string.common_processing),
            title = title,
            onCancel = { onCancelUpdate() },
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
    canEdit: Boolean = true,
    canRefresh: Boolean = true,
    canDelete: Boolean = true,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val displayName = subscription.name.ifBlank { stringResource(Res.string.subscription_config) }
    // M3E 用成对的 container/onContainer；Miuix 保持原来的配色不变
    val isMaterial = LocalUiMode.current == UiMode.Material
    // 操作按钮固定尺寸：文字区域收缩，动作按钮保持 Material 的胶囊/圆形形态。
    val actionButtonWidth = if (isMaterial) 56.dp else 35.dp

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
                text = subscription.name.ifBlank { stringResource(Res.string.subscription_config) },
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight(550),
                color = MishkaColors.onSurface,
            )
            if (subscription.isActive) {
                val activeColor = StatusColors.healthy
                Text(
                    text = stringResource(Res.string.subscription_in_use),
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (subscription.total > 0) {
                    val used = subscription.upload + subscription.download
                    Text(
                        text = stringResource(
                            Res.string.subscription_used_traffic,
                            FormatUtils.formatBytes(used),
                            FormatUtils.formatBytes(subscription.total)
                        ),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.subscription_no_traffic),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }

                if (subscription.updatedAt > 0) {
                    Text(
                        text = stringResource(Res.string.subscription_updated_at, formatEpochMillisAsLocal(subscription.updatedAt)),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canEdit) IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(actionButtonWidth, 35.dp),
                    minHeight = 35.dp,
                    minWidth = actionButtonWidth,
                    backgroundColor = MishkaColors.secondaryContainer,
                    capsule = isMaterial,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = AppIcons.MoreCircle,
                        contentDescription = stringResource(Res.string.common_edit),
                        tint = if (isMaterial) MishkaColors.onSecondaryContainer else MishkaColors.onSurfaceVariantSummary,
                    )
                }
                if (canRefresh) IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(actionButtonWidth, 35.dp),
                    enabled = !isLoading && subscription.url.isNotBlank(),
                    minHeight = 35.dp,
                    minWidth = actionButtonWidth,
                    backgroundColor = MishkaColors.secondaryContainer,
                    capsule = isMaterial,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = AppIcons.Refresh,
                        contentDescription = stringResource(Res.string.common_update),
                        tint = if (isMaterial) MishkaColors.onSecondaryContainer else MishkaColors.onSurfaceVariantSummary,
                    )
                }
                if (canDelete) IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(35.dp),
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = if (isMaterial) MishkaColors.errorContainer else MishkaColors.secondaryContainer,
                    capsule = isMaterial,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = AppIcons.Delete,
                        contentDescription = stringResource(Res.string.common_delete),
                        tint = if (isMaterial) MishkaColors.onErrorContainer else MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }

    WindowDialog(
        show = showDeleteDialog,
        title = stringResource(Res.string.subscription_delete_title),
        summary = stringResource(Res.string.subscription_delete_summary, displayName),
        onDismissRequest = { showDeleteDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showDeleteDialog = false },
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
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
