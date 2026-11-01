package com.linx3141.mexpa.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.linx3141.mexpa.R
import com.linx3141.mexpa.data.backup.BackupManager
import com.linx3141.mexpa.data.backup.WebDavClient
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.ProxyServiceBridge
import com.linx3141.mexpa.platform.ProxyState
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.platform.showToast
import com.linx3141.mexpa.util.describe
import kotlin.coroutines.resume

@Immutable
class BackupViewModel(
    private val backupManager: BackupManager,
    private val storage: PlatformStorage,
    private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private fun client(): WebDavClient? {
        val url = storage.getString(StorageKeys.WEBDAV_URL, "").trim()
        val user = storage.getString(StorageKeys.WEBDAV_USERNAME, "")
        val pass = storage.getString(StorageKeys.WEBDAV_PASSWORD, "")
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showToast(context.getString(R.string.backup_missing_credentials))
            return null
        }
        return WebDavClient(url, user, pass)
    }

    fun testConnection() = runBusy {
        val client = client() ?: return@runBusy
        client.testConnection()
        showToast(context.getString(R.string.backup_connection_ok))
    }

    fun backup() = runBusy {
        val client = client() ?: return@runBusy
        backupManager.withTransferFile { file ->
            backupManager.writeBackupTo(file)
            client.upload(file)
        }
        showToast(context.getString(R.string.backup_done))
    }

    fun restore() = runBusy {
        if (!ensureProxyStopped()) return@runBusy
        val client = client() ?: return@runBusy
        backupManager.withTransferFile { file ->
            if (!client.download(file)) {
                showToast(context.getString(R.string.backup_not_found))
                return@withTransferFile
            }
            backupManager.restoreBackupFrom(file)
            _uiState.update { it.copy(restoreCompleted = true) }
        }
    }

    /**
     * 本地导出：[pickTarget] 弹 SAF CreateDocument 拿目标文档，写入由 BackupManager 流式完成。
     * 用户取消（Uri 为 null）不提示。
     */
    fun exportBackup(pickTarget: (onResult: (Uri?) -> Unit) -> Unit) = runBusy {
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            pickTarget { result -> cont.resume(result) }
        } ?: return@runBusy
        backupManager.exportTo(uri)
        showToast(context.getString(R.string.backup_export_done))
    }

    /** 本地导入：SAF 选中的 zip 文档。前置校验与 WebDAV 恢复一致。 */
    fun restoreFromDocument(uri: Uri) = runBusy {
        if (!ensureProxyStopped()) return@runBusy
        backupManager.importFrom(uri)
        _uiState.update { it.copy(restoreCompleted = true) }
    }

    // 恢复覆盖 imported/ 与 DB，代理运行/过渡态期间禁止（mihomo 正在读这些文件）
    private fun ensureProxyStopped(): Boolean {
        val state = ProxyServiceBridge.state.value.state
        if (state != ProxyState.Stopped && state != ProxyState.Error) {
            showToast(context.getString(R.string.backup_stop_proxy_first))
            return false
        }
        return true
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                showToast(context.getString(R.string.backup_failed, e.describe()), long = true)
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }
}
