package com.linx3141.mexpa.ui.screen.overrides

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.OverrideProfile
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.HintCard
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.util.WideContentBox
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.util.formatEpochMillisAsLocal
import com.linx3141.mexpa.viewmodel.OverrideProfileUiState
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

@Composable
fun OverrideListScreenContent(
    state: OverrideProfileUiState = OverrideProfileUiState(),
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onEditFile: (String) -> Unit = {},
    onUpdate: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    onUpdateAll: () -> Unit = {},
    canAdd: Boolean = true,
    canEdit: Boolean = true,
    canUpdate: Boolean = true,
    canDelete: Boolean = true,
    canUpdateAll: Boolean = true,
) {

    ClearOverrideErrorOnExit(onClearError)
    val scrollBehavior = rememberMishkaScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.settings_overrides),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { OverrideBackButton(onBack) },
                    actions = {
                        if (canUpdateAll && state.profiles.any { it.isRemote }) {
                            IconButton(onClick = onUpdateAll, enabled = !state.isLoading) {
                                Icon(
                                    imageVector = AppIcons.Refresh,
                                    contentDescription = stringResource(Res.string.subscription_update_all),
                                    tint = MishkaColors.onSurface,
                                )
                            }
                        }
                        if (canAdd) IconButton(
                            onClick = { onClearError(); onAdd() },
                            enabled = !state.isLoading,
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = stringResource(Res.string.override_add),
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
                    .horizontalCutoutPadding()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .platformScrollEndHaptic()
                    .platformOverScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = sidePadding,
                    end = sidePadding,
                ),
            ) {
                item(key = "top_spacer") { Spacer(Modifier.height(12.dp)) }

                if (state.error.isNotEmpty()) {
                    item(key = "error") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageHorizontalPadding)
                                .padding(bottom = 12.dp),
                            insideMargin = PaddingValues(16.dp),
                        ) {
                            Text(text = state.error, color = StatusColors.danger)
                        }
                    }
                }

                if (state.profiles.isEmpty()) {
                    item(key = "empty") {
                        HintCard(text = stringResource(Res.string.override_empty))
                    }
                }

                items(state.profiles, key = { it.id }) { profile ->
                    OverrideItem(
                        profile = profile,
                        isLoading = state.isLoading,
                        onOpen = { onClearError(); onEditFile(profile.id) },
                        onEdit = { onClearError(); onEdit(profile.id) },
                        onUpdate = { onUpdate(profile.id) },
                        onDelete = { onDelete(profile.id) },
                        canEdit = canEdit,
                        canUpdate = canUpdate,
                        canDelete = canDelete,
                        modifier = Modifier.animateItem(),
                    )
                }

                item(key = "bottom_spacer") {
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

@Composable
private fun OverrideItem(
    profile: OverrideProfile,
    isLoading: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    canEdit: Boolean = true,
    canUpdate: Boolean = true,
    canDelete: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageHorizontalPadding)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
        onClick = { if (!isLoading) onOpen() },
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Text(
            text = profile.name,
            fontSize = 17.sp,
            fontWeight = FontWeight(550),
            color = MishkaColors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = overrideSummary(profile),
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 12.sp,
            color = MishkaColors.onSurfaceVariantSummary,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (profile.sourceLocation.isNotBlank()) {
                    Text(
                        text = profile.sourceLocation,
                        fontSize = 12.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                profile.lastUpdatedAt?.let {
                    Text(
                        text = stringResource(
                            Res.string.subscription_updated_at,
                            formatEpochMillisAsLocal(it),
                        ),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (canEdit) ItemAction(
                icon = AppIcons.MoreCircle,
                contentDescription = stringResource(Res.string.common_edit),
                enabled = !isLoading,
                onClick = onEdit,
            )
            if (canUpdate && profile.isRemote) {
                Spacer(Modifier.width(8.dp))
                ItemAction(
                    icon = AppIcons.Refresh,
                    contentDescription = stringResource(Res.string.common_update),
                    enabled = !isLoading,
                    onClick = onUpdate,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (canDelete) ItemAction(
                icon = AppIcons.Delete,
                contentDescription = stringResource(Res.string.common_delete),
                enabled = !isLoading,
                onClick = { showDeleteDialog = true },
            )
        }
    }

    WindowDialog(
        show = showDeleteDialog,
        title = stringResource(Res.string.override_delete_title),
        summary = stringResource(Res.string.override_delete_summary, profile.name),
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

@Composable
private fun ItemAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        minHeight = 35.dp,
        minWidth = 35.dp,
        backgroundColor = MishkaColors.secondaryContainer,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MishkaColors.onSurfaceVariantSummary
            else MishkaColors.disabledOnSecondaryVariant,
        )
    }
}
