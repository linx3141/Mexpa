package com.linx3141.mexpa.ui.screen.settings

data class SettingsUiState(
    val tunModeIndex: Int = 0,
    val autoStartEnabled: Boolean = false,
    val autoConnectEnabled: Boolean = false,
    val dynamicNotificationEnabled: Boolean = true,
    val updateViaProxyEnabled: Boolean = true,
    val restartAfterUpdateEnabled: Boolean = true,
    val hideTaskCardEnabled: Boolean = false,
    val hasRootPermission: Boolean = false,
    val isProxyRunning: Boolean = false,
    val versionName: String = "",
)

/** Linux 应把 Android 专属能力置 false，从 UI 上隐藏对应入口而不是画假开关。 */
data class SettingsCapabilities(
    val tunMode: Boolean = true,
    val vpnSettings: Boolean = true,
    val rootSettings: Boolean = true,
    val appProxy: Boolean = true,
    val wifiPolicy: Boolean = true,
    val bootStart: Boolean = true,
    val dynamicNotification: Boolean = true,
    val hideTaskCard: Boolean = true,
    val metaSettings: Boolean = true,
    val androidGeneralOptions: Boolean = true,
    val fileManager: Boolean = true,
    val overrides: Boolean = true,
)
