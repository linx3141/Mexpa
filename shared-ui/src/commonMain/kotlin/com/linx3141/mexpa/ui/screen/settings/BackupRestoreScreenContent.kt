package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.shared.resources.*



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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.HintCard
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.BackupUiState
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Import
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun BackupRestoreScreenContent(
    uiState: BackupUiState = BackupUiState(),
    hasFilePicker: Boolean = false,
    webDavUrl: String = "",
    webDavUsername: String = "",
    webDavPassword: String = "",
    onWebDavUrlChange: (String) -> Unit = {},
    onWebDavUsernameChange: (String) -> Unit = {},
    onWebDavPasswordChange: (String) -> Unit = {},
    onExportLocal: () -> Unit = {},
    onRestoreLocal: () -> Unit = {},
    onTestConnection: () -> Unit = {},
    onBackupWebDav: () -> Unit = {},
    onRestoreWebDav: () -> Unit = {},
    onRestartApp: () -> Unit = {},
    onBack: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    var showLocalDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    // 恢复来源（本地文件 / WebDAV）共用同一个覆盖确认对话框，确认后执行挂起的动作
    var pendingRestoreAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.backup_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(Res.string.common_back),
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
                .platformScrollEndHaptic()
                .platformOverScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            item(key = "top_spacer") {
                Spacer(Modifier.height(12.dp))
            }
            item(key = "hint") {
                HintCard(text = stringResource(Res.string.backup_hint))
            }
            groupedCardItems(
                keyPrefix = "backup_entries",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("local") {
                        ArrowPreference(
                            title = stringResource(Res.string.backup_section_local),
                            summary = stringResource(Res.string.backup_local_row_summary),
                            enabled = hasFilePicker && !uiState.isBusy,
                            holdDownState = showLocalDialog,
                            onClick = { showLocalDialog = true },
                        )
                    },
                    CardItem("webdav") {
                        ArrowPreference(
                            title = stringResource(Res.string.backup_section_webdav),
                            summary = stringResource(Res.string.backup_webdav_row_summary),
                            enabled = !uiState.isBusy,
                            holdDownState = showWebDavDialog,
                            onClick = { showWebDavDialog = true },
                        )
                    },
                ),
            )
            item(key = "bottom_spacer") {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    if (hasFilePicker) {
        LocalBackupDialog(
            show = showLocalDialog,
            isBusy = uiState.isBusy,
            onDismiss = { showLocalDialog = false },
            onExport = {
                showLocalDialog = false
                onExportLocal()
            },
            onRestore = {
                showLocalDialog = false
                pendingRestoreAction = { onRestoreLocal() }
            },
        )
    }

    WebDavDialog(
        show = showWebDavDialog,
        isBusy = uiState.isBusy,
        url = webDavUrl,
        username = webDavUsername,
        password = webDavPassword,
        onUrlChange = onWebDavUrlChange,
        onUsernameChange = onWebDavUsernameChange,
        onPasswordChange = onWebDavPasswordChange,
        onDismiss = { showWebDavDialog = false },
        onTest = onTestConnection,
        onBackup = onBackupWebDav,
        onRestore = {
            showWebDavDialog = false
            pendingRestoreAction = { onRestoreWebDav() }
        },
    )

    WindowDialog(
        show = pendingRestoreAction != null,
        title = stringResource(Res.string.backup_restore_confirm_title),
        summary = stringResource(Res.string.backup_restore_confirm_summary),
        onDismissRequest = { pendingRestoreAction = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { pendingRestoreAction = null },
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val action = pendingRestoreAction
                    pendingRestoreAction = null
                    action?.invoke()
                },
            )
        }
    }

    // 恢复成功后强制重启：OverrideJsonStore / Repository Flow 等内存态不随磁盘恢复刷新，不给取消项
    WindowDialog(
        show = uiState.restoreCompleted,
        title = stringResource(Res.string.backup_restore_done_title),
        summary = stringResource(Res.string.backup_restore_done_summary),
        onDismissRequest = {},
    ) {
        TextButton(
            text = stringResource(Res.string.backup_restart_now),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary(),
            onClick = onRestartApp,
        )
    }
}

@Composable
private fun LocalBackupDialog(
    show: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = stringResource(Res.string.backup_section_local),
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = onDismiss,
    ) {
        ArrowPreference(
            title = stringResource(Res.string.backup_export_local),
            startAction = {
                Icon(
                    imageVector = AppIcons.Backup,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MishkaColors.onSurface,
                )
            },
            enabled = !isBusy,
            onClick = onExport,
            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        )
        ArrowPreference(
            title = stringResource(Res.string.backup_restore_local),
            startAction = {
                Icon(
                    imageVector = AppIcons.Import,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MishkaColors.onSurface,
                )
            },
            enabled = !isBusy,
            onClick = onRestore,
            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        )
        TextButton(
            text = stringResource(Res.string.common_cancel),
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun WebDavDialog(
    show: Boolean,
    isBusy: Boolean,
    url: String,
    username: String,
    password: String,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onTest: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {

    WindowDialog(
        show = show,
        title = stringResource(Res.string.backup_section_webdav),
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = onDismiss,
    ) {
        // 长内容：滚动区收缩 + 底部按钮行固定，防小屏上按钮被顶出可视区
        Column(Modifier.heightIn(max = 500.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                    label = stringResource(Res.string.backup_server_url),
                )
                TextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                    label = stringResource(Res.string.backup_username),
                )
                TextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 12.dp),
                    label = stringResource(Res.string.backup_password),
                )
                ArrowPreference(
                    title = stringResource(Res.string.backup_now),
                    startAction = {
                        Icon(
                            imageVector = AppIcons.Backup,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                            tint = MishkaColors.onSurface,
                        )
                    },
                    enabled = !isBusy,
                    onClick = onBackup,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                )
                ArrowPreference(
                    title = stringResource(Res.string.backup_restore_now),
                    startAction = {
                        Icon(
                            imageVector = AppIcons.Import,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                            tint = MishkaColors.onSurface,
                        )
                    },
                    enabled = !isBusy,
                    onClick = onRestore,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(Res.string.backup_test_connection),
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    onClick = onTest,
                )
                TextButton(
                    text = stringResource(Res.string.common_close),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
            }
        }
    }
}
