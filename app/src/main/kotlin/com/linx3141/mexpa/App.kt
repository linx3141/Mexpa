package com.linx3141.mexpa

import androidx.compose.runtime.Composable
import com.linx3141.mexpa.platform.BootStartManager
import com.linx3141.mexpa.platform.FilePicker
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.WifiPolicyController
import com.linx3141.mexpa.ui.navigation.AppNavigation
import com.linx3141.mexpa.ui.theme.MishkaTheme
import com.linx3141.mexpa.ui.theme.ThemeConfig
import com.linx3141.mexpa.viewmodel.AppProxyViewModel
import com.linx3141.mexpa.viewmodel.BackupViewModel
import com.linx3141.mexpa.viewmodel.ConnectionViewModel
import com.linx3141.mexpa.viewmodel.DnsQueryViewModel
import com.linx3141.mexpa.viewmodel.AutomationViewModel
import com.linx3141.mexpa.viewmodel.ExternalControlViewModel
import com.linx3141.mexpa.viewmodel.HomeViewModel
import com.linx3141.mexpa.viewmodel.LogViewModel
import com.linx3141.mexpa.viewmodel.MetaSettingsViewModel
import com.linx3141.mexpa.viewmodel.NetworkSettingsViewModel
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel
import com.linx3141.mexpa.viewmodel.ProviderViewModel
import com.linx3141.mexpa.viewmodel.ProxyViewModel
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

@Composable
fun App(
    themeConfig: ThemeConfig = ThemeConfig(),
    onThemeConfigChange: (ThemeConfig) -> Unit = {},
    homeViewModel: HomeViewModel? = null,
    subscriptionViewModel: SubscriptionViewModel? = null,
    overrideProfileViewModel: OverrideProfileViewModel? = null,
    proxyViewModel: ProxyViewModel? = null,
    logViewModel: LogViewModel? = null,
    providerViewModel: ProviderViewModel? = null,
    connectionViewModel: ConnectionViewModel? = null,
    dnsQueryViewModel: DnsQueryViewModel? = null,
    networkSettingsViewModel: NetworkSettingsViewModel? = null,
    metaSettingsViewModel: MetaSettingsViewModel? = null,
    externalControlViewModel: ExternalControlViewModel? = null,
    automationViewModel: AutomationViewModel? = null,
    appProxyViewModel: AppProxyViewModel? = null,
    filePicker: FilePicker? = null,
    storage: PlatformStorage? = null,
    bootStartManager: BootStartManager? = null,
    mihomoVersion: String = "",
    onScanQR: ((callback: (String?) -> Unit) -> Unit)? = null,
    wifiPolicyController: WifiPolicyController? = null,
    onRequestWifiPermission: (((Boolean) -> Unit) -> Unit)? = null,
    onPredictiveBackChange: ((Boolean) -> Unit)? = null,
    onHideTaskCardChange: ((Boolean) -> Unit)? = null,
    hasRootPermission: Boolean = false,
    deepLinkImport: DeepLinkImportRequest? = null,
    onDeepLinkImportConsumed: () -> Unit = {},
    backupViewModel: BackupViewModel? = null,
    onRestartApp: () -> Unit = {},
) {
    MishkaTheme(themeConfig = themeConfig) {
        AppNavigation(
            themeConfig = themeConfig,
            onThemeConfigChange = onThemeConfigChange,
            homeViewModel = homeViewModel,
            subscriptionViewModel = subscriptionViewModel,
            overrideProfileViewModel = overrideProfileViewModel,
            proxyViewModel = proxyViewModel,
            logViewModel = logViewModel,
            providerViewModel = providerViewModel,
            connectionViewModel = connectionViewModel,
            dnsQueryViewModel = dnsQueryViewModel,
            networkSettingsViewModel = networkSettingsViewModel,
            metaSettingsViewModel = metaSettingsViewModel,
            externalControlViewModel = externalControlViewModel,
            automationViewModel = automationViewModel,
            appProxyViewModel = appProxyViewModel,
            filePicker = filePicker,
            storage = storage,
            bootStartManager = bootStartManager,
            mihomoVersion = mihomoVersion,
            onScanQR = onScanQR,
            wifiPolicyController = wifiPolicyController,
            onRequestWifiPermission = onRequestWifiPermission,
            onPredictiveBackChange = onPredictiveBackChange,
            onHideTaskCardChange = onHideTaskCardChange,
            hasRootPermission = hasRootPermission,
            deepLinkImport = deepLinkImport,
            onDeepLinkImportConsumed = onDeepLinkImportConsumed,
            backupViewModel = backupViewModel,
            onRestartApp = onRestartApp,
        )
    }
}
