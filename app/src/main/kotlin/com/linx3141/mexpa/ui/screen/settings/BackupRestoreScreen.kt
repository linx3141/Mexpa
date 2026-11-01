package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.platform.FilePicker
import com.linx3141.mexpa.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    storage: PlatformStorage,
    filePicker: FilePicker? = null,
    onRestartApp: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf(storage.getString(StorageKeys.WEBDAV_URL, "")) }
    var username by remember { mutableStateOf(storage.getString(StorageKeys.WEBDAV_USERNAME, "")) }
    var password by remember { mutableStateOf(storage.getString(StorageKeys.WEBDAV_PASSWORD, "")) }
    BackupRestoreScreenContent(
        uiState = uiState,
        hasFilePicker = filePicker != null,
        webDavUrl = url,
        webDavUsername = username,
        webDavPassword = password,
        onWebDavUrlChange = { url = it; storage.putString(StorageKeys.WEBDAV_URL, it) },
        onWebDavUsernameChange = { username = it; storage.putString(StorageKeys.WEBDAV_USERNAME, it) },
        onWebDavPasswordChange = { password = it; storage.putString(StorageKeys.WEBDAV_PASSWORD, it) },
        onExportLocal = {
            viewModel.exportBackup { onResult ->
                filePicker?.createZipDocument(defaultBackupFileName(), onResult)
            }
        },
        onRestoreLocal = {
            filePicker?.pickZipDocument { uri ->
                if (uri != null) viewModel.restoreFromDocument(uri)
            }
        },
        onTestConnection = { viewModel.testConnection() },
        onBackupWebDav = { viewModel.backup() },
        onRestoreWebDav = { viewModel.restore() },
        onRestartApp = onRestartApp,
        onBack = onBack,
    )
}

private fun defaultBackupFileName(): String {
    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "mishka-backup-$ts.zip"
}
