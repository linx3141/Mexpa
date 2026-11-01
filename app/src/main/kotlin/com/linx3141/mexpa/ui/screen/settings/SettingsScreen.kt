package com.linx3141.mexpa.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.BuildConfig
import com.linx3141.mexpa.platform.BootStartManager
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.ProxyServiceBridge
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.ui.screen.settings.SettingsCapabilities
import com.linx3141.mexpa.ui.screen.settings.SettingsUiState

/**
 * Android 适配层：只负责把 PlatformStorage / BootStartManager / ProxyServiceBridge
 * 映射成共享 Settings 页的 state 与 callbacks；布局本身在 shared-ui。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onNavigateVpnSettings: () -> Unit = {},
    onNavigateRootSettings: () -> Unit = {},
    onNavigateNetworkSettings: () -> Unit = {},
    onNavigateMetaSettings: () -> Unit = {},
    onNavigateExternalControl: () -> Unit = {},
    onNavigateAutomation: () -> Unit = {},
    onNavigateAppProxy: () -> Unit = {},
    onNavigateWifiPolicy: () -> Unit = {},
    onNavigateThemeSettings: () -> Unit = {},
    onNavigateFileManager: () -> Unit = {},
    onNavigateOverrides: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    bootStartManager: BootStartManager? = null,
    storage: PlatformStorage? = null,
    onHideTaskCardChange: ((Boolean) -> Unit)? = null,
    hasRootPermission: Boolean = false,
    isProxyRunning: Boolean = false,
) {
    val initial = remember {
        SettingsUiState(
            tunModeIndex = when (storage?.getString(StorageKeys.TUN_MODE, "vpn")) {
                "root_tun" -> 1
                "root_tproxy" -> 2
                else -> 0
            },
            autoStartEnabled = bootStartManager?.isEnabled() ?: false,
            autoConnectEnabled = storage?.getString(StorageKeys.AUTO_CONNECT_ON_LAUNCH, "false") == "true",
            dynamicNotificationEnabled = storage?.getString(StorageKeys.DYNAMIC_NOTIFICATION, "true") != "false",
            updateViaProxyEnabled = storage?.getString(StorageKeys.SUBSCRIPTION_UPDATE_VIA_PROXY, "true") != "false",
            restartAfterUpdateEnabled = storage?.getString(StorageKeys.RESTART_AFTER_PROFILE_UPDATE, "true") != "false",
            hideTaskCardEnabled = storage?.getString(StorageKeys.HIDE_TASK_CARD, "false") == "true",
            hasRootPermission = hasRootPermission,
            isProxyRunning = isProxyRunning,
            versionName = BuildConfig.VERSION_NAME,
        )
    }
    var state by remember { mutableStateOf(initial) }
    LaunchedEffect(hasRootPermission, isProxyRunning) {
        state = state.copy(hasRootPermission = hasRootPermission, isProxyRunning = isProxyRunning)
    }

    SettingsScreenContent(
        modifier = modifier,
        bottomPadding = bottomPadding,
        state = state,
        capabilities = SettingsCapabilities(
            bootStart = bootStartManager != null,
            hideTaskCard = onHideTaskCardChange != null,
        ),
        onNavigateVpnSettings = onNavigateVpnSettings,
        onNavigateRootSettings = onNavigateRootSettings,
        onNavigateNetworkSettings = onNavigateNetworkSettings,
        onNavigateMetaSettings = onNavigateMetaSettings,
        onNavigateExternalControl = onNavigateExternalControl,
        onNavigateAutomation = onNavigateAutomation,
        onNavigateAppProxy = onNavigateAppProxy,
        onNavigateWifiPolicy = onNavigateWifiPolicy,
        onNavigateThemeSettings = onNavigateThemeSettings,
        onNavigateFileManager = onNavigateFileManager,
        onNavigateOverrides = onNavigateOverrides,
        onNavigateBackup = onNavigateBackup,
        onNavigateAbout = onNavigateAbout,
        onTunModeChange = { index ->
            val mode = when (index) {
                1 -> "root_tun"
                2 -> "root_tproxy"
                else -> "vpn"
            }
            storage?.putString(StorageKeys.TUN_MODE, mode)
            state = state.copy(tunModeIndex = index)
        },
        onAutoStartChange = { enabled ->
            bootStartManager?.setEnabled(enabled)
            state = state.copy(autoStartEnabled = enabled)
        },
        onAutoConnectChange = { enabled ->
            storage?.putString(StorageKeys.AUTO_CONNECT_ON_LAUNCH, enabled.toString())
            state = state.copy(autoConnectEnabled = enabled)
        },
        onDynamicNotificationChange = { enabled ->
            storage?.putString(StorageKeys.DYNAMIC_NOTIFICATION, enabled.toString())
            ProxyServiceBridge.requestNotificationRefresh()
            state = state.copy(dynamicNotificationEnabled = enabled)
        },
        onUpdateViaProxyChange = { enabled ->
            storage?.putString(StorageKeys.SUBSCRIPTION_UPDATE_VIA_PROXY, enabled.toString())
            state = state.copy(updateViaProxyEnabled = enabled)
        },
        onRestartAfterUpdateChange = { enabled ->
            storage?.putString(StorageKeys.RESTART_AFTER_PROFILE_UPDATE, enabled.toString())
            state = state.copy(restartAfterUpdateEnabled = enabled)
        },
        onHideTaskCardChange = { enabled ->
            storage?.putString(StorageKeys.HIDE_TASK_CARD, enabled.toString())
            onHideTaskCardChange?.invoke(enabled)
            state = state.copy(hideTaskCardEnabled = enabled)
        },
    )
}
