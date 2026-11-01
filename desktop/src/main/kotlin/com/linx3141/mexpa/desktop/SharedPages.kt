package com.linx3141.mexpa.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import com.linx3141.mexpa.ui.screen.settings.aboutOssProjects
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import com.linx3141.mexpa.shared.resources.*
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.SwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.jetbrains.compose.resources.stringResource
import com.linx3141.mexpa.ui.screen.settings.AboutScreenMaterial
import com.linx3141.mexpa.ui.screen.settings.ThemeSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.SettingsScreenContent
import com.linx3141.mexpa.ui.screen.settings.SettingsUiState
import com.linx3141.mexpa.ui.screen.settings.SettingsCapabilities
import com.linx3141.mexpa.ui.screen.settings.AutomationScreenContent
import com.linx3141.mexpa.ui.screen.settings.ExternalControlScreenContent
import com.linx3141.mexpa.ui.screen.settings.FileManagerScreenContent
import com.linx3141.mexpa.ui.screen.settings.NetworkSettingsScreenContent
import com.linx3141.mexpa.ui.screen.settings.BackupRestoreScreenContent
import com.linx3141.mexpa.viewmodel.BackupUiState
import com.linx3141.mexpa.ui.screen.overrides.OverrideListScreenContent
import com.linx3141.mexpa.viewmodel.OverrideProfileUiState
import com.linx3141.mexpa.viewmodel.NetworkSettingsUiState
import com.linx3141.mexpa.domain.model.ConfigurationOverride
import com.linx3141.mexpa.domain.model.DnsOverride
import com.linx3141.mexpa.viewmodel.ExternalControlUiState
import com.linx3141.mexpa.viewmodel.AutomationUiState
import com.linx3141.mexpa.ui.screen.home.HomeContent
import com.linx3141.mexpa.ui.screen.proxy.ProxyScreenContent
import com.linx3141.mexpa.ui.screen.provider.ProviderScreenContent
import com.linx3141.mexpa.ui.screen.log.LogScreenContent
import com.linx3141.mexpa.ui.screen.dns.DnsQueryScreenContent
import com.linx3141.mexpa.ui.screen.connection.ConnectionScreenContent
import com.linx3141.mexpa.viewmodel.ConnectionUiState
import com.linx3141.mexpa.viewmodel.LogUiState
import com.linx3141.mexpa.viewmodel.IndexedLog
import com.linx3141.mexpa.viewmodel.DnsQueryUiState
import com.linx3141.mexpa.domain.model.LogMessage
import com.linx3141.mexpa.domain.model.DnsAnswer
import com.linx3141.mexpa.viewmodel.ProviderUiState
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionAddScreenContent
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionAddUrlScreenContent
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionEditScreenContent
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionScreenContent
import com.linx3141.mexpa.domain.model.Subscription
import com.linx3141.mexpa.domain.model.SubscriptionInfo
import com.linx3141.mexpa.viewmodel.SubscriptionUiState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.ui.component.design.LazyColumn
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TopAppBar
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.theme.MishkaColors
import kotlinx.collections.immutable.toPersistentList
import com.linx3141.mexpa.viewmodel.ProxyUiState
import com.linx3141.mexpa.viewmodel.ProxyGroupUi
import com.linx3141.mexpa.ui.screen.home.HomeFabOverlay
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.viewmodel.SpeedSnapshot
import com.linx3141.mexpa.viewmodel.MemorySnapshot
import com.linx3141.mexpa.viewmodel.SystemInfoSnapshot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import com.linx3141.mexpa.ui.component.design.WindowDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.linx3141.mexpa.shared.resources.Res
import com.linx3141.mexpa.shared.resources.mexpa_icon_foreground
import org.jetbrains.compose.resources.painterResource

@Composable
fun SharedAboutPage(controller: LinuxProxyController, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val homeInfo by controller.data.homeInfo.collectAsState()
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixAboutPage(
            mihomoVersion = homeInfo.version,
            onOpenUrl = { uriHandler.openUri(it) },
            onBack = onBack,
        )
        return
    }
    AboutScreenMaterial(
        appName = "Mexpa",
        versionName = AppVersion.name,
        versionCode = AppVersion.code,
        appIcon = painterResource(Res.drawable.mexpa_icon_foreground),
        onBack = onBack,
        mihomoVersion = homeInfo.version,
        onOpenUrl = { uriHandler.openUri(it) },
    )
}

@Composable
fun SharedThemeSettingsPage(state: DesktopAppState, onBack: () -> Unit) {
    ThemeSettingsScreen(
        themeConfig = state.themeConfig,
        onThemeConfigChange = { next ->
            if (next.uiMode != state.themeConfig.uiMode) state.requestTheme(next) else state.updateTheme(next)
        },
        swipeDismissEnabled = true,
        onBack = onBack,
    )
}

@Composable
fun SharedSettingsPage(state: DesktopAppState) {
    var ui by remember { mutableStateOf(SettingsUiState(versionName = AppVersion.name)) }
    SettingsScreenContent(
        state = ui,
        capabilities = SettingsCapabilities(
            tunMode = false,
            vpnSettings = false,
            rootSettings = false,
            appProxy = false,
            wifiPolicy = false,
            bootStart = false,
            dynamicNotification = false,
            hideTaskCard = false,
            metaSettings = true,
            androidGeneralOptions = false,
            fileManager = true,
            overrides = true,
        ),
        onNavigateNetworkSettings = { state.open(SubPage.Network) },
        onNavigateMetaSettings = { state.open(SubPage.Meta) },
        onNavigateExternalControl = { state.open(SubPage.ExternalControl) },
        onNavigateAutomation = { state.open(SubPage.Automation) },
        onNavigateThemeSettings = { state.open(SubPage.Theme) },
        onNavigateFileManager = { state.open(SubPage.FileManager) },
        onNavigateOverrides = { state.open(SubPage.Overrides) },
        onNavigateBackup = { state.open(SubPage.Backup) },
        onNavigateAbout = { state.open(SubPage.About) },
        onTunModeChange = { ui = ui.copy(tunModeIndex = it) },
    )
}

@Composable
fun SharedHomePage(
    state: DesktopAppState,
    linux: LinuxProxyState,
    controller: LinuxProxyController,
    onStart: () -> Unit,
    onStop: () -> Unit,
    fabBottomPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 1 })
    val data = controller.data
    val homeInfo by data.homeInfo.collectAsState()
    val speed by data.speed.collectAsState()
    val memory by data.memory.collectAsState()
    val systemInfo by data.systemInfo.collectAsState()
    val latency by data.latency.collectAsState()
    val topRates by data.connectionRates.collectAsState()
    val connectionState by data.connectionState.collectAsState()
    val uptime by controller.uptimeSeconds.collectAsState()
    val fallbackMihomoVersion by produceState("", state.binaryPath) {
        value = MihomoCoreVersion.read(state.binaryPath)
    }
    val subscriptionState = state.subscriptionRepository?.state?.collectAsState()?.value
    val activeSubscription = subscriptionState?.subscriptions?.firstOrNull { it.isActive }
    val activeTraffic = activeSubscription?.takeIf { it.total > 0 }?.let {
        SubscriptionInfo(
            Upload = it.upload,
            Download = it.download,
            Total = it.total,
            Expire = it.expire,
        )
    }
    val uiState = HomeUiState(
        isRunning = linux.running,
        isStarting = linux.starting,
        isStopping = false,
        mode = homeInfo.mode.ifBlank { "--" },
        tunStack = homeInfo.config?.tun?.stack.orEmpty(),
        ipv6 = homeInfo.config?.ipv6 ?: false,
        config = homeInfo.config,
        subscription = homeInfo.subscription?.takeIf { it.Total > 0 } ?: activeTraffic,
        providerTraffic = homeInfo.providerTraffic,
        isProviderTrafficLoading = homeInfo.providerTrafficLoading,
        providerTrafficLoadFailed = homeInfo.providerTrafficFailed,
        latencyBaidu = latency.baidu,
        latencyCloudflare = latency.cloudflare,
        latencyGoogle = latency.google,
        isTestingLatency = latency.testing,
        latencyViaRules = latency.viaRules,
        version = homeInfo.version,
        errorMessage = if (!linux.running && !linux.starting && linux.message.isNotBlank()) linux.message else "",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            speed = speed,
            memory = memory,
            systemInfo = systemInfo,
            uptimeSeconds = uptime,
            topConnectionRates = topRates,
            connectionCount = if (linux.running) connectionState.connections.size else null,
            fallbackMihomoVersion = fallbackMihomoVersion.ifBlank { homeInfo.version },
            onStartConnectionRateTracking = { data.startObservingConnections() },
            onStopConnectionRateTracking = { data.stopObservingConnections() },
            onRefreshProviderTraffic = { controller.refreshProviderTraffic() },
            onUpdateAllProviders = { controller.updateAllProviders() },
            onRestart = { controller.restart() },
            onStop = onStop,
            onReload = { controller.reload() },
            onTestLatency = { controller.testLatency() },
            onNavigateLog = { state.open(SubPage.Logs) },
            onNavigateProvider = { state.open(SubPage.Providers) },
            onNavigateConnection = { state.open(SubPage.Connections) },
            onNavigateDnsQuery = { state.open(SubPage.Dns) },
            onStartProxy = onStart,
            onSwitchMode = { controller.switchMode(it) },
            onSwitchTunStack = { stack ->
                state.updateLinuxSettings(state.linuxSettings.copy(tunStack = stack))
            },
            bottomPadding = bottomPadding + if (LocalUiMode.current == UiMode.Material) 80.dp else 0.dp,
        )
        if (LocalUiMode.current == UiMode.Material) {
            HomeFabOverlay(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                isRunning = linux.running,
                isStarting = linux.starting,
                isStopping = false,
                onStart = onStart,
                onRestart = { controller.restart() },
                onStop = onStop,
                onReload = { controller.reload() },
                bottomPadding = fabBottomPadding,
                pagerState = pagerState,
            )
        }
    }
}

@Composable
fun SharedProxyPage(state: DesktopAppState, controller: LinuxProxyController) {
    var sortOption by remember { mutableStateOf(0) }
    var singleColumn by remember { mutableStateOf(false) }
    var showGroupIcons by remember { mutableStateOf(true) }
    val proxyState by controller.data.proxyState.collectAsState()
    ProxyScreenContent(
        uiState = proxyState,
        sortOption = sortOption,
        singleColumn = singleColumn,
        showGroupIcons = showGroupIcons,
        onUpdateSortOption = { sortOption = it },
        onUpdateSingleColumn = { singleColumn = it },
        onUpdateShowGroupIcons = { showGroupIcons = it },
        onSelectProxy = { group, node -> controller.selectProxy(group, node) },
        onTestGroupDelay = { group -> controller.testGroupDelay(group) },
        onTestNodeDelay = { node -> controller.testNodeDelay(node) },
        onUnfixProxy = { group -> controller.unfixProxy(group) },
    )
}

@Composable
private fun subscriptionUiState(state: DesktopAppState): SubscriptionUiState {
    val repository = state.subscriptionRepository ?: return SubscriptionUiState()
    return repository.state.collectAsState().value
}

@Composable
fun SharedSubscriptionPage(state: DesktopAppState) {
    val repository = state.subscriptionRepository
    val uiState = subscriptionUiState(state)
    SubscriptionScreenContent(
        uiState = uiState,
        onNavigateAdd = { state.open(SubPage.SubscriptionAdd) },
        onNavigateEdit = { id ->
            state.editingSubscriptionId = id
            state.open(SubPage.SubscriptionEdit)
        },
        onUpdateAll = { repository?.updateAll() },
        onClearError = { repository?.clearError() },
        onSetActive = { id -> repository?.setActive(id) },
        onRefreshSubscription = { id -> repository?.update(id) },
        onRemoveSubscription = { id -> repository?.delete(id) },
        onCancelUpdate = { repository?.cancelUpdate() },
        canAdd = repository != null,
        canUpdate = repository != null,
        canEdit = repository != null,
        canRefresh = repository != null,
        canDelete = repository != null,
    )
}

/** AWT 文件选择器；Compose 事件处理本来就在 EDT 上，直接弹原生对话框即可。 */
private fun pickConfigFile(): File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "选择配置文件", java.awt.FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json") }
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

@Composable
fun SharedSubscriptionAddPage(state: DesktopAppState, onBack: () -> Unit) {
    val repository = state.subscriptionRepository
    val uiState = subscriptionUiState(state)
    SubscriptionAddScreenContent(
        uiState = uiState,
        onBack = onBack,
        onPickFile = {
            val file = pickConfigFile()
            if (file != null) repository?.importFile(file, onComplete = onBack)
        },
        onNavigateUrl = { state.open(SubPage.SubscriptionAddUrl) },
        onScanQR = null,
        onCancelUpdate = { repository?.cancelUpdate() },
    )
}

@Composable
fun SharedSubscriptionAddUrlPage(state: DesktopAppState, onBack: () -> Unit) {
    val repository = state.subscriptionRepository
    val uiState = subscriptionUiState(state)
    SubscriptionAddUrlScreenContent(
        uiState = uiState,
        onBack = onBack,
        onAddSubscription = { name, url, intervalMs, userAgent, ageSecretKey, onComplete ->
            repository?.importUrl(name, url, intervalMs / 60_000, userAgent, ageSecretKey, onComplete)
        },
        onSaved = onBack,
        onCancelUpdate = { repository?.cancelUpdate() },
    )
}

@Composable
fun SharedSubscriptionEditPage(state: DesktopAppState, onBack: () -> Unit) {
    val repository = state.subscriptionRepository
    val uiState = subscriptionUiState(state)
    SubscriptionEditScreenContent(
        uuid = state.editingSubscriptionId.orEmpty(),
        uiState = uiState,
        onBack = onBack,
        onSaved = onBack,
        onNavigateOverrides = { state.open(SubPage.Overrides) },
        onEditSubscription = { uuid, name, source, intervalMs, userAgent, ageSecretKey, onComplete ->
            repository?.updateMetadata(uuid, name, source, intervalMs / 60_000, userAgent, ageSecretKey, onComplete)
        },
        onCancelUpdate = { repository?.cancelUpdate() },
    )
}

@Composable
fun SharedProviderPage(state: DesktopAppState, controller: LinuxProxyController) {
    val providerState by controller.data.providerState.collectAsState()
    ProviderScreenContent(
        uiState = providerState,
        onBack = state::back,
        onUpdateAll = { controller.updateAllProviders() },
        onUpdateProvider = { name, isRule -> controller.updateProvider(name, isRule) },
    )
}

@Composable
fun SharedLogsPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val logs by controller.data.logs.collectAsState()
    val logUiState by controller.data.logUiState.collectAsState()
    LogScreenContent(
        uiState = logUiState,
        logs = logs,
        onBack = onBack,
        onConnect = { controller.data.startLogStream() },
        onDisconnect = { controller.data.stopLogStream() },
        onClear = { controller.data.clearLogs() },
    )
}

@Composable
fun SharedDnsPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val dnsState by controller.data.dnsState.collectAsState()
    DnsQueryScreenContent(
        uiState = dnsState,
        onBack = onBack,
        onSetQueryName = { controller.setDnsQueryName(it) },
        onQuery = { controller.queryDns(dnsState.queryName, dnsState.queryType) },
        onSetQueryType = { controller.setDnsQueryType(it) },
        onFlushCache = { controller.flushCache() },
        onFlushFakeIp = { controller.flushFakeIp() },
    )
}

@Composable
fun SharedConnectionsPage(state: DesktopAppState, controller: LinuxProxyController) {
    val connectionState by controller.data.connectionState.collectAsState()
    ConnectionScreenContent(
        uiState = connectionState,
        onBack = state::back,
        onStartObserving = { controller.data.startObservingConnections() },
        onStopObserving = { controller.data.stopObservingConnections() },
        onCloseConnection = { controller.closeConnection(it) },
        onCloseAllConnections = { controller.closeAllConnections() },
    )
}

@Composable
fun SharedAutomationPage(state: DesktopAppState, onBack: () -> Unit) {
    AutomationScreenContent(
        uiState = AutomationUiState(allowed = state.linuxSettings.externalControlAllowed),
        onBack = onBack,
        onAllowedChange = { allowed ->
            state.updateLinuxSettings(state.linuxSettings.copy(externalControlAllowed = allowed))
        },
    )
}

@Composable
fun SharedExternalControlPage(state: DesktopAppState, onBack: () -> Unit) {
    ExternalControlScreenContent(
        uiState = ExternalControlUiState(
            externalController = state.controllerEndpoint.takeIf { it.isNotBlank() },
            secret = state.controllerSecret.takeIf { it.isNotBlank() },
        ),
        onBack = onBack,
        onSetExternalController = { endpoint ->
            state.updateControllerEndpoint(endpoint, state.controllerSecret.takeIf { it.isNotBlank() })
        },
        onSetSecret = { secret ->
            state.updateControllerEndpoint(state.controllerEndpoint.takeIf { it.isNotBlank() }, secret)
        },
    )
}

@Composable
private fun MiuixAboutPage(
    mihomoVersion: String,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val links = aboutOssProjects(mihomoVersion)
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.about_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(Res.string.common_back),
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 24.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.mexpa_icon),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Mexpa",
                        fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MishkaColors.onSurface,
                    )
                    Text(
                        text = "v${AppVersion.name} (${AppVersion.code})",
                        fontSize = 13.sp,
                        color = MishkaColors.onSurfaceVariantSummary,
                    )
                }
            }
            item { SmallTitle(stringResource(Res.string.about_open_source)) }
            groupedCardItems(
                keyPrefix = "miuix_about_links",
                items = links.map { (name, url) ->
                    CardItem(url) {
                        ArrowPreference(
                            title = name,
                            summary = url.removePrefix("https://"),
                            onClick = { onOpenUrl(url) },
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun SharedMetaSettingsPage(state: DesktopAppState, onBack: () -> Unit) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    var keyPair by remember { mutableStateOf<AgeKeyPair?>(null) }
    var copied by remember { mutableStateOf<String?>(null) }

    fun copy(text: String, label: String) {
        runCatching {
            java.awt.Toolkit.getDefaultToolkit().systemClipboard
                .setContents(java.awt.datatransfer.StringSelection(text), null)
            copied = label
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.linux_meta_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(Res.string.common_back),
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            item { SmallTitle(stringResource(Res.string.linux_meta_age_title)) }
            groupedCardItems(
                keyPrefix = "linux_meta_age",
                items = listOf(
                    CardItem("generate") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_meta_age_generate),
                            summary = stringResource(Res.string.linux_meta_age_generate_summary),
                            onClick = { keyPair = AgeKey.generate() },
                        )
                    },
                    CardItem("generateHybrid") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_meta_age_generate_hybrid),
                            summary = stringResource(Res.string.linux_meta_age_generate_hybrid_summary),
                            onClick = {
                                keyPair = AgeKey.hybrid(state.binaryPath)
                                if (keyPair == null) copied = null
                            },
                        )
                    },
                ),
            )
            item {
                Text(
                    text = stringResource(Res.string.linux_meta_age_hint),
                    modifier = Modifier.padding(horizontal = PageHorizontalPadding, vertical = 8.dp),
                    color = MishkaColors.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
            keyPair?.let { pair ->
                item { SmallTitle(stringResource(Res.string.linux_meta_age_result_title)) }
                groupedCardItems(
                    keyPrefix = "linux_meta_age_result",
                    items = listOf(
                        CardItem("secret") {
                            ArrowPreference(
                                title = stringResource(Res.string.linux_meta_age_secret),
                                summary = pair.secretKey,
                                onClick = { copy(pair.secretKey, "secret") },
                            )
                        },
                        CardItem("public") {
                            ArrowPreference(
                                title = stringResource(Res.string.linux_meta_age_public),
                                summary = pair.publicKey,
                                onClick = { copy(pair.publicKey, "public") },
                            )
                        },
                    ),
                )
                item {
                    Text(
                        text = stringResource(Res.string.linux_meta_age_copy_hint),
                        modifier = Modifier.padding(horizontal = PageHorizontalPadding, vertical = 8.dp),
                        color = MishkaColors.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }

    if (copied != null) {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_meta_copied),
            summary = copied,
            onDismissRequest = { copied = null },
        ) {
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { copied = null },
            )
        }
    }
}

@Composable
fun SharedFileManagerPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val store = state.overridesStore
    val profilesFlow = remember(store) { store?.profiles ?: MutableStateFlow(emptyList<OverrideEntry>()) }
    val profiles by profilesFlow.collectAsState()
    val files = buildList {
        add("配置文件" to File(state.configPath))
        add("mihomo 日志" to File(state.workDir, "mihomo.log"))
        add("运行时覆写" to File(state.workDir, "runtime/override.json"))
        add("运行时有效配置" to File(state.workDir, "runtime/config.effective.yaml"))
        add("设置" to File(DesktopPlatform.configDir, "settings.json"))
        profiles.forEach { entry ->
            add("覆写：${entry.name}" to File(DesktopPlatform.configDir, "overrides/${entry.id}.${entry.format}"))
        }
        val subsDir = File(state.stateDir, "subscriptions")
        subsDir.walkTopDown().filter { it.isFile }.forEach { file ->
            add("订阅文件：${file.relativeTo(subsDir).path}" to file)
        }
    }
    fun open(path: File) {
        state.editorPath = path.path
        state.open(SubPage.FileEditor)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.settings_file_manager),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(onClick = onBack, contentDescription = stringResource(Res.string.common_back))
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 24.dp),
        ) {
            item { SmallTitle(stringResource(Res.string.linux_files_title)) }
            groupedCardItems(
                keyPrefix = "linux_files",
                items = files.map { (title, file) ->
                    CardItem(file.path) {
                        ArrowPreference(
                            title = title,
                            summary = if (file.isFile) {
                                "${file.path} (${file.length()} B)"
                            } else {
                                stringResource(Res.string.linux_file_missing, file.path)
                            },
                            enabled = file.isFile,
                            onClick = { open(file) },
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun SharedFileEditorPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val path = state.editorPath
    if (path == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val file = File(path)
    val store = state.overridesStore
    val overrideEntry = store?.profiles?.value?.firstOrNull { entry ->
        store.contentFile(entry).absolutePath == file.absolutePath
    }
    var text by remember(path) { mutableStateOf(if (file.isFile) file.readText() else "") }
    var dirty by remember(path) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scrollBehavior = rememberMishkaScrollBehavior()

    fun save() {
        if (overrideEntry != null) {
            store?.writeContent(overrideEntry.id, text)
        } else {
            runCatching { file.writeText(text) }.onFailure { message = it.message }
        }
        dirty = false
        controller.reloadConfig()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = file.name,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(onClick = onBack, contentDescription = stringResource(Res.string.common_back))
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = PageHorizontalPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(Res.string.common_save),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { save() },
                )
                TextButton(
                    text = stringResource(Res.string.linux_file_delete),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = true },
                )
            }
            Text(
                text = file.path,
                modifier = Modifier.padding(vertical = 6.dp),
                color = MishkaColors.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it; dirty = true },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MishkaColors.surfaceContainer)
                    .padding(10.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MishkaColors.onSurface,
                ),
                cursorBrush = SolidColor(MishkaColors.primary),
            )
        }
    }

    if (confirmDelete) {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_file_delete_confirm, file.name),
            onDismissRequest = { confirmDelete = false },
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        if (overrideEntry != null) store?.delete(overrideEntry.id) else file.delete()
                        confirmDelete = false
                        controller.reloadConfig()
                        onBack()
                    },
                )
            }
        }
    }
    message?.let {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_file_error),
            summary = it,
            onDismissRequest = { message = null },
        ) {
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { message = null },
            )
        }
    }
}

@Composable
fun SharedOverridesPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val store = state.overridesStore
    val repo = state.subscriptionRepository
    val profilesFlow = remember(store) { store?.profiles ?: MutableStateFlow(emptyList<OverrideEntry>()) }
    val profiles by profilesFlow.collectAsState()
    var appliedIds by remember { mutableStateOf(repo?.activeOverrideIds().orEmpty()) }
    val scrollBehavior = rememberMishkaScrollBehavior()
    var showRemoteDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val remoteName = rememberTextFieldState()
    val remoteUrl = rememberTextFieldState()

    fun applySelection(next: List<String>) {
        appliedIds = next
        repo?.activeId()?.let { repo.setOverrides(it, next) }
        controller.reloadConfig()
    }

    fun openEditor(id: String) {
        state.editingOverrideId = id
        state.open(SubPage.OverrideEdit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.settings_overrides),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(onClick = onBack, contentDescription = stringResource(Res.string.common_back))
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 24.dp),
        ) {
            item { SmallTitle(stringResource(Res.string.linux_override_apply_title)) }
            if (profiles.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.linux_override_empty),
                        modifier = Modifier.padding(horizontal = PageHorizontalPadding, vertical = 8.dp),
                        color = MishkaColors.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                }
            } else {
                groupedCardItems(
                    keyPrefix = "linux_override_list",
                    items = profiles.map { entry ->
                        CardItem("card-${entry.id}") {
                            Column {
                                SwitchPreference(
                                    checked = entry.id in appliedIds,
                                    onCheckedChange = { checked ->
                                        applySelection(
                                            if (checked) (appliedIds + entry.id).distinct() else appliedIds - entry.id,
                                        )
                                    },
                                    title = entry.name,
                                    summary = buildString {
                                        append(entry.format)
                                        append(" · ")
                                        append(if (entry.sourceType == "remote") entry.url else "本地")
                                    },
                                )
                                ArrowPreference(
                                    title = stringResource(Res.string.linux_override_edit),
                                    summary = stringResource(Res.string.linux_override_edit_summary),
                                    onClick = { openEditor(entry.id) },
                                )
                            }
                        }
                    },
                )
            }
            item { SmallTitle(stringResource(Res.string.linux_override_add_title)) }
            groupedCardItems(
                keyPrefix = "linux_override_add",
                items = listOf(
                    CardItem("yaml") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_override_new_yaml),
                            summary = stringResource(Res.string.linux_override_new_yaml_summary),
                            onClick = { store?.addBlank("YAML 覆写", "yaml")?.let { openEditor(it.id) } },
                        )
                    },
                    CardItem("js") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_override_new_js),
                            summary = stringResource(Res.string.linux_override_new_js_summary),
                            onClick = { store?.addBlank("JS 覆写", "js")?.let { openEditor(it.id) } },
                        )
                    },
                    CardItem("file") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_override_import),
                            summary = stringResource(Res.string.linux_override_import_summary),
                            onClick = {
                                pickConfigFile()?.let { file ->
                                    val entry = store?.importFile(file)
                                    if (entry == null) message = "仅支持 .yaml/.yml/.js" else openEditor(entry.id)
                                }
                            },
                        )
                    },
                    CardItem("remote") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_override_remote),
                            summary = stringResource(Res.string.linux_override_remote_summary),
                            onClick = { showRemoteDialog = true },
                        )
                    },
                ),
            )
            item {
                Text(
                    text = stringResource(Res.string.linux_override_hint),
                    modifier = Modifier.padding(horizontal = PageHorizontalPadding, vertical = 8.dp),
                    color = MishkaColors.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
        }
    }

    if (showRemoteDialog) {
        WindowDialog(
            show = showRemoteDialog,
            title = stringResource(Res.string.linux_override_remote),
            onDismissRequest = { showRemoteDialog = false },
        ) {
            TextField(state = remoteName, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(state = remoteUrl, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { showRemoteDialog = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val name = remoteName.text.toString().trim()
                        val url = remoteUrl.text.toString().trim()
                        val format = if (url.endsWith(".js")) "js" else "yaml"
                        val entry = state.overridesStore?.addRemote(name, url, format)
                        showRemoteDialog = false
                        if (entry == null) message = "URL 无效" else openEditor(entry.id)
                    },
                )
            }
        }
    }

    message?.let {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_file_error),
            summary = it,
            onDismissRequest = { message = null },
        ) {
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { message = null },
            )
        }
    }
}

@Composable
fun SharedOverrideEditorPage(state: DesktopAppState, controller: LinuxProxyController, onBack: () -> Unit) {
    val store = state.overridesStore
    val id = state.editingOverrideId
    val entry = store?.profiles?.value?.firstOrNull { it.id == id }
    if (store == null || entry == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var text by remember(entry.id) { mutableStateOf(store.readContent(entry.id)) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scrollBehavior = rememberMishkaScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = entry.name,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(onClick = onBack, contentDescription = stringResource(Res.string.common_back))
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = PageHorizontalPadding),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_save),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        store.writeContent(entry.id, text)
                        controller.reloadConfig()
                        onBack()
                    },
                )
                if (entry.sourceType == "remote") {
                    TextButton(
                        text = stringResource(Res.string.linux_override_update),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            store.updateRemote(entry.id) { error ->
                                message = error ?: "已更新"
                                text = store.readContent(entry.id)
                            }
                        },
                    )
                }
                TextButton(
                    text = stringResource(Res.string.linux_file_delete),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = true },
                )
            }
            Text(
                text = "${entry.format} · ${entry.sourceType} · ${store.contentFile(entry).path}",
                modifier = Modifier.padding(vertical = 6.dp),
                color = MishkaColors.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MishkaColors.surfaceContainer)
                    .padding(10.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MishkaColors.onSurface,
                ),
                cursorBrush = SolidColor(MishkaColors.primary),
            )
        }
    }

    if (confirmDelete) {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_file_delete_confirm, entry.name),
            onDismissRequest = { confirmDelete = false },
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        store.delete(entry.id)
                        controller.reloadConfig()
                        confirmDelete = false
                        onBack()
                    },
                )
            }
        }
    }
    message?.let {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.linux_override_remote),
            summary = it,
            onDismissRequest = { message = null },
        ) {
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { message = null },
            )
        }
    }
}

@Composable
fun SharedNetworkPage(
    state: DesktopAppState,
    linux: LinuxProxyState,
    controller: LinuxProxyController,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var capability by remember(state.binaryPath) { mutableStateOf(controller.tunCapability(state.binaryPath)) }
    var supportedStacks by remember(state.binaryPath) {
        mutableStateOf(controller.tunSupportedStacks(state.binaryPath))
    }
    var granting by remember { mutableStateOf(false) }
    var grantMessage by remember { mutableStateOf<String?>(null) }
    val systemProxySupported = remember { controller.systemProxySupported() }
    val settings = state.linuxSettings
    val networkOverride = state.networkOverride
    val geo = state.geoDataManager
    val geoState = geo?.state?.collectAsState()?.value

    var showPacDialog by remember { mutableStateOf(false) }
    val pacTextState = rememberTextFieldState()
    var showPathDialog by remember { mutableStateOf(false) }
    var pathDialogTitle by remember { mutableStateOf("") }
    var pathDialogSetter by remember { mutableStateOf<(String) -> Unit>({}) }
    val pathTextState = rememberTextFieldState()

    val perAppUsers = remember { PerAppUids.users() }
    var showPerAppDialog by remember { mutableStateOf(false) }
    var perAppSelection by remember { mutableStateOf(settings.perAppUids.toSet()) }
    val perAppModeItems = listOf(
        stringResource(Res.string.linux_per_app_off),
        stringResource(Res.string.linux_per_app_include),
        stringResource(Res.string.linux_per_app_exclude),
    )
    val perAppSummary = when (settings.perAppMode) {
        "include", "exclude" -> stringResource(Res.string.linux_per_app_selected_count, settings.perAppUids.size)
        else -> stringResource(Res.string.linux_per_app_off)
    }
    val conflict = remember(linux.running, state.mixedPort, linux.message) {
        ConflictDetector.detect(state.mixedPort, linux.running)
    }
    var autostartEnabled by remember { mutableStateOf(Autostart.isEnabled()) }
    val autostartAvailable = remember { Autostart.available() }
    val configFileTitle = stringResource(Res.string.linux_config_file)
    val binaryTitle = stringResource(Res.string.linux_mihomo_binary)
    val workDirTitle = stringResource(Res.string.linux_work_dir)

    fun openPathDialog(title: String, current: String, setter: (String) -> Unit) {
        pathDialogTitle = title
        pathDialogSetter = setter
        pathTextState.edit { replace(0, length, current) }
        showPathDialog = true
    }

    fun refreshTunState() {
        capability = controller.tunCapability(state.binaryPath)
        supportedStacks = controller.tunSupportedStacks(state.binaryPath)
    }

    fun requestGrant(enableAfter: Boolean) {
        if (granting) return
        granting = true
        grantMessage = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { controller.grantTunCapability(state.binaryPath) }
            refreshTunState()
            grantMessage = result.message
            if (result.ok && enableAfter) {
                state.updateLinuxSettings(state.linuxSettings.copy(tunEnabled = true))
            }
            granting = false
        }
    }

    val platformContent: LazyListScope.() -> Unit = {
        item { SmallTitle(stringResource(Res.string.linux_runtime_status_title)) }
        groupedCardItems(
            keyPrefix = "linux_runtime_status",
            items = listOf(
                CardItem("message") {
                    ArrowPreference(
                        title = stringResource(Res.string.linux_runtime_message),
                        summary = linux.message.ifBlank { stringResource(Res.string.linux_not_enabled) },
                        onClick = {},
                    )
                },
                CardItem("systemProxy") {
                    ArrowPreference(
                        title = stringResource(Res.string.linux_runtime_system_proxy),
                        summary = linux.systemProxy.ifBlank { stringResource(Res.string.linux_not_enabled) },
                        onClick = {},
                    )
                },
                CardItem("tun") {
                    ArrowPreference(
                        title = stringResource(Res.string.linux_runtime_tun),
                        summary = linux.tun.ifBlank { stringResource(Res.string.linux_not_enabled) },
                        onClick = {},
                    )
                },
                CardItem("dnsTakeover") {
                    ArrowPreference(
                        title = stringResource(Res.string.linux_runtime_dns),
                        summary = if (linux.running && settings.tunEnabled && settings.dnsTakeover) {
                            stringResource(Res.string.linux_runtime_dns_on)
                        } else {
                            stringResource(Res.string.linux_runtime_dns_off)
                        },
                        onClick = {},
                    )
                },
            ),
        )

        if (conflict.hasConflict) {
            item { SmallTitle(stringResource(Res.string.linux_conflict_title)) }
            groupedCardItems(
                keyPrefix = "linux_conflict",
                items = buildList {
                    conflict.foreignTun.forEachIndexed { index, name ->
                        add(
                            CardItem("tun$index") {
                                ArrowPreference(
                                    title = stringResource(Res.string.linux_conflict_foreign_tun, name),
                                    summary = stringResource(Res.string.linux_conflict_foreign_tun_summary),
                                    onClick = {},
                                )
                            },
                        )
                    }
                    conflict.otherSystemProxy?.let { proxy ->
                        add(
                            CardItem("systemProxy") {
                                ArrowPreference(
                                    title = stringResource(Res.string.linux_conflict_system_proxy, proxy),
                                    summary = stringResource(Res.string.linux_conflict_system_proxy_summary),
                                    onClick = {},
                                )
                            },
                        )
                    }
                    if (conflict.mixedPortBusy) {
                        add(
                            CardItem("port") {
                                ArrowPreference(
                                    title = stringResource(Res.string.linux_conflict_port, state.mixedPort),
                                    summary = stringResource(Res.string.linux_conflict_port_summary),
                                    onClick = {},
                                )
                            },
                        )
                    }
                },
            )
        }

        item { SmallTitle(stringResource(Res.string.linux_takeover_title)) }
        groupedCardItems(
            keyPrefix = "linux_takeover",
            items = buildList {
                add(
                    CardItem("systemProxy") {
                        SwitchPreference(
                            checked = settings.systemProxy,
                            onCheckedChange = { value ->
                                state.updateLinuxSettings(state.linuxSettings.copy(systemProxy = value))
                            },
                            title = stringResource(Res.string.linux_system_proxy),
                            summary = when {
                                !systemProxySupported -> stringResource(Res.string.linux_system_proxy_unsupported)
                                !settings.systemProxy -> stringResource(Res.string.common_disabled)
                                linux.running -> stringResource(Res.string.linux_system_proxy_summary_on, linux.mixedPort)
                                else -> stringResource(Res.string.linux_takeover_next_start)
                            },
                            enabled = systemProxySupported,
                        )
                    },
                )
                add(
                    CardItem("pac") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_pac_url),
                            summary = settings.pacUrl.ifBlank { stringResource(Res.string.linux_pac_unset) },
                            enabled = systemProxySupported,
                            onClick = {
                                pacTextState.edit { replace(0, length, settings.pacUrl) }
                                showPacDialog = true
                            },
                        )
                    },
                )
                add(
                    CardItem("tun") {
                        SwitchPreference(
                            checked = settings.tunEnabled,
                            onCheckedChange = { value ->
                                when {
                                    !value -> state.updateLinuxSettings(state.linuxSettings.copy(tunEnabled = false))
                                    capability == TunCapability.Capable ->
                                        state.updateLinuxSettings(state.linuxSettings.copy(tunEnabled = true))
                                    else -> requestGrant(enableAfter = true)
                                }
                            },
                            title = stringResource(Res.string.linux_tun),
                            summary = when {
                                granting -> stringResource(Res.string.linux_tun_granting)
                                !settings.tunEnabled -> stringResource(Res.string.common_disabled)
                                linux.running -> stringResource(Res.string.common_enabled)
                                else -> stringResource(Res.string.linux_takeover_next_start)
                            },
                            enabled = !granting,
                        )
                    },
                )
                if (supportedStacks.size > 1) {
                    add(
                        CardItem("stack") {
                            OverlayDropdownPreference(
                                title = stringResource(Res.string.linux_tun_stack),
                                items = supportedStacks,
                                selectedIndex = supportedStacks.indexOf(settings.tunStack).coerceAtLeast(0),
                                summary = stringResource(Res.string.linux_takeover_next_start),
                                onSelectedIndexChange = { index ->
                                    supportedStacks.getOrNull(index)?.let { stack ->
                                        state.updateLinuxSettings(state.linuxSettings.copy(tunStack = stack))
                                    }
                                },
                            )
                        },
                    )
                }
                if (capability != TunCapability.Capable) {
                    add(
                        CardItem("permission") {
                            ArrowPreference(
                                title = stringResource(Res.string.linux_tun_permission),
                                summary = grantMessage ?: when (capability) {
                                    TunCapability.MissingCapability ->
                                        stringResource(Res.string.linux_tun_capability_missing)
                                    TunCapability.BinaryMissing ->
                                        stringResource(Res.string.linux_tun_binary_missing)
                                    else -> stringResource(Res.string.linux_tun_unknown)
                                },
                                enabled = !granting,
                                onClick = { requestGrant(enableAfter = false) },
                            )
                        },
                    )
                } else if (grantMessage != null) {
                    add(
                        CardItem("permission") {
                            ArrowPreference(
                                title = stringResource(Res.string.linux_tun_capable),
                                summary = grantMessage,
                                onClick = {},
                            )
                        },
                    )
                }
                add(
                    CardItem("dnsTakeover") {
                        SwitchPreference(
                            checked = settings.dnsTakeover,
                            onCheckedChange = { enabled ->
                                state.updateLinuxSettings(state.linuxSettings.copy(dnsTakeover = enabled))
                            },
                            title = stringResource(Res.string.linux_dns_takeover),
                            summary = if (settings.tunEnabled) {
                                stringResource(Res.string.linux_dns_takeover_summary_on)
                            } else {
                                stringResource(Res.string.linux_dns_takeover_summary_tun_only)
                            },
                            enabled = settings.tunEnabled,
                        )
                    },
                )
                add(
                    CardItem("perAppMode") {
                        OverlayDropdownPreference(
                            title = stringResource(Res.string.linux_per_app_title),
                            items = perAppModeItems,
                            selectedIndex = when (settings.perAppMode) {
                                "include" -> 1
                                "exclude" -> 2
                                else -> 0
                            },
                            summary = perAppSummary,
                            onSelectedIndexChange = { index ->
                                val mode = listOf("off", "include", "exclude")[index.coerceIn(0, 2)]
                                state.updateLinuxSettings(state.linuxSettings.copy(perAppMode = mode))
                                if (mode != "off") {
                                    perAppSelection = state.linuxSettings.perAppUids.toSet()
                                    showPerAppDialog = true
                                }
                            },
                        )
                    },
                )
                add(
                    CardItem("perAppUsers") {
                        ArrowPreference(
                            title = stringResource(Res.string.linux_per_app_users),
                            summary = perAppSummary,
                            enabled = settings.perAppMode != "off",
                            onClick = {
                                perAppSelection = state.linuxSettings.perAppUids.toSet()
                                showPerAppDialog = true
                            },
                        )
                    },
                )
            },
        )

        item { SmallTitle(stringResource(Res.string.linux_runtime_files)) }
        groupedCardItems(
            keyPrefix = "linux_runtime_files",
            items = listOf(
                CardItem("config") {
                    ArrowPreference(
                        title = configFileTitle,
                        summary = state.configPath,
                        onClick = {
                            openPathDialog(configFileTitle, state.configPath) { value ->
                                state.updatePathSettings(configPath = value)
                            }
                        },
                    )
                },
                CardItem("binary") {
                    ArrowPreference(
                        title = binaryTitle,
                        summary = state.binaryPath,
                        onClick = {
                            openPathDialog(binaryTitle, state.binaryPath) { value ->
                                state.updatePathSettings(binaryPath = value)
                            }
                        },
                    )
                },
                CardItem("workdir") {
                    ArrowPreference(
                        title = workDirTitle,
                        summary = state.workDir,
                        onClick = {
                            openPathDialog(workDirTitle, state.workDir) { value ->
                                state.updatePathSettings(workDir = value)
                            }
                        },
                    )
                },
            ),
        )

        item { SmallTitle(stringResource(Res.string.linux_data_title)) }
        groupedCardItems(
            keyPrefix = "linux_data",
            items = listOf(
                CardItem("geoip") {
                    ArrowPreference(
                        title = stringResource(Res.string.linux_geoip_data),
                        summary = when {
                            geoState?.downloading == true -> stringResource(Res.string.linux_geoip_downloading)
                            !geoState?.error.isNullOrBlank() -> geoState?.error.orEmpty()
                            geoState != null -> geoState?.message.orEmpty()
                            else -> stringResource(Res.string.linux_geoip_missing)
                        },
                        enabled = geo != null && geoState?.downloading != true,
                        onClick = { geo?.download() },
                    )
                },
                CardItem("autoUpdate") {
                    SwitchPreference(
                        checked = settings.autoUpdateEnabled,
                        onCheckedChange = { state.updateAutoUpdate(it) },
                        title = stringResource(Res.string.linux_subscription_auto_update),
                        summary = stringResource(Res.string.linux_subscription_auto_update_summary),
                    )
                },
                CardItem("viaProxy") {
                    SwitchPreference(
                        checked = settings.updateViaProxy,
                        onCheckedChange = { enabled ->
                            state.updateLinuxSettings(state.linuxSettings.copy(updateViaProxy = enabled))
                        },
                        title = stringResource(Res.string.linux_subscription_via_proxy),
                        summary = stringResource(Res.string.linux_subscription_via_proxy_summary),
                    )
                },
                CardItem("autostart") {
                    SwitchPreference(
                        checked = autostartEnabled,
                        onCheckedChange = { enabled ->
                            autostartEnabled = Autostart.setEnabled(enabled)
                        },
                        title = stringResource(Res.string.linux_autostart),
                        summary = when {
                            !autostartAvailable -> stringResource(Res.string.linux_autostart_unavailable)
                            autostartEnabled -> stringResource(Res.string.linux_autostart_enabled)
                            else -> stringResource(Res.string.common_disabled)
                        },
                        enabled = autostartAvailable,
                    )
                },
                CardItem("closeToTray") {
                    SwitchPreference(
                        checked = settings.closeToTray,
                        onCheckedChange = { enabled ->
                            state.updateLinuxSettings(state.linuxSettings.copy(closeToTray = enabled))
                        },
                        title = stringResource(Res.string.linux_close_to_tray),
                        summary = stringResource(Res.string.linux_close_to_tray_summary),
                    )
                },
                CardItem("autoConnect") {
                    SwitchPreference(
                        checked = settings.autoConnectOnLaunch,
                        onCheckedChange = { enabled ->
                            state.updateLinuxSettings(state.linuxSettings.copy(autoConnectOnLaunch = enabled))
                        },
                        title = stringResource(Res.string.linux_auto_connect),
                        summary = stringResource(Res.string.linux_auto_connect_summary),
                    )
                },
                CardItem("restoreLast") {
                    SwitchPreference(
                        checked = settings.restoreLastState,
                        onCheckedChange = { enabled ->
                            state.updateLinuxSettings(state.linuxSettings.copy(restoreLastState = enabled))
                        },
                        title = stringResource(Res.string.linux_restore_last),
                        summary = stringResource(Res.string.linux_restore_last_summary),
                    )
                },
                CardItem("restartAfterUpdate") {
                    SwitchPreference(
                        checked = settings.restartAfterUpdate,
                        onCheckedChange = { enabled ->
                            state.updateLinuxSettings(state.linuxSettings.copy(restartAfterUpdate = enabled))
                        },
                        title = stringResource(Res.string.linux_restart_after_update),
                        summary = stringResource(Res.string.linux_restart_after_update_summary),
                    )
                },
            ),
        )
    }

    NetworkSettingsScreenContent(
        uiState = NetworkSettingsUiState(
            httpPort = networkOverride.httpPort,
            socksPort = networkOverride.socksPort,
            redirPort = networkOverride.redirPort,
            tproxyPort = networkOverride.tproxyPort,
            mixedPort = networkOverride.mixedPort,
            allowLan = networkOverride.allowLan,
            ipv6 = networkOverride.ipv6,
            bindAddress = networkOverride.bindAddress,
            logLevel = networkOverride.logLevel,
            dns = networkOverride.dns,
        ),
        onBack = onBack,
        onUpdate = { transform ->
            state.updateNetworkOverride(transform(state.networkOverride))
        },
        onUpdateDns = { transform ->
            val dns = state.networkOverride.dns ?: DnsOverride()
            state.updateNetworkOverride(state.networkOverride.copy(dns = transform(dns)))
        },
        platformContent = platformContent,
    )

    if (showPacDialog) {
        WindowDialog(
            show = showPacDialog,
            title = stringResource(Res.string.linux_pac_url),
            summary = stringResource(Res.string.linux_pac_summary),
            onDismissRequest = { showPacDialog = false },
        ) {
            TextField(state = pacTextState, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { showPacDialog = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        state.updateLinuxSettings(
                            state.linuxSettings.copy(pacUrl = pacTextState.text.toString().trim()),
                        )
                        showPacDialog = false
                    },
                )
            }
        }
    }

    if (showPerAppDialog) {
        WindowDialog(
            show = showPerAppDialog,
            title = stringResource(Res.string.linux_per_app_dialog_title),
            summary = stringResource(Res.string.linux_per_app_dialog_summary),
            onDismissRequest = { showPerAppDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                perAppUsers.forEach { user ->
                    SwitchPreference(
                        checked = user.uid in perAppSelection,
                        onCheckedChange = { checked ->
                            perAppSelection = if (checked) perAppSelection + user.uid else perAppSelection - user.uid
                        },
                        title = "${user.name} (uid ${user.uid})",
                        summary = stringResource(Res.string.linux_per_app_process_count, user.processCount),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { showPerAppDialog = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        state.updateLinuxSettings(state.linuxSettings.copy(perAppUids = perAppSelection.sorted()))
                        showPerAppDialog = false
                    },
                )
            }
        }
    }

    if (showPathDialog) {
        WindowDialog(
            show = showPathDialog,
            title = pathDialogTitle,
            onDismissRequest = { showPathDialog = false },
        ) {
            TextField(state = pathTextState, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(Res.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { showPathDialog = false },
                )
                TextButton(
                    text = stringResource(Res.string.common_confirm),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val value = pathTextState.text.toString().trim()
                        if (value.isNotEmpty()) pathDialogSetter(value)
                        showPathDialog = false
                    },
                )
            }
        }
    }
}

@Composable
fun SharedBackupPage(state: DesktopAppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var restoreCompleted by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val configDir = DesktopPlatform.configDir

    fun pickBackupFile(save: Boolean): File? {
        val dialog = java.awt.FileDialog(
            null as java.awt.Frame?,
            if (save) "导出备份" else "选择备份文件",
            if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD,
        )
        if (save) dialog.file = "mexpa-backup.zip"
        dialog.setFilenameFilter { _, name -> name.endsWith(".zip") }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        return File(directory, file)
    }

    fun webDavClient(): WebDavClient = WebDavClient(
        baseUrl = state.linuxSettings.webDavUrl,
        username = state.linuxSettings.webDavUser,
        password = state.linuxSettings.webDavPassword,
    )

    BackupRestoreScreenContent(
        uiState = BackupUiState(isBusy = busy, restoreCompleted = restoreCompleted),
        hasFilePicker = true,
        webDavUrl = state.linuxSettings.webDavUrl,
        webDavUsername = state.linuxSettings.webDavUser,
        webDavPassword = state.linuxSettings.webDavPassword,
        onWebDavUrlChange = { state.updateLinuxSettings(state.linuxSettings.copy(webDavUrl = it)) },
        onWebDavUsernameChange = { state.updateLinuxSettings(state.linuxSettings.copy(webDavUser = it)) },
        onWebDavPasswordChange = { state.updateLinuxSettings(state.linuxSettings.copy(webDavPassword = it)) },
        onTestConnection = {
            if (busy) return@BackupRestoreScreenContent
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { webDavClient().test() }
                busy = false
                message = result.message
            }
        },
        onBackupWebDav = {
            if (busy) return@BackupRestoreScreenContent
            busy = true
            scope.launch {
                val temp = File.createTempFile("mexpa-backup", ".zip")
                val exported = withContext(Dispatchers.IO) {
                    LinuxBackup.export(temp, configDir, state.stateDir, state.subscriptionRepository?.subscriptions().orEmpty())
                }
                if (!exported.ok) {
                    busy = false
                    message = exported.message
                    return@launch
                }
                val uploaded = withContext(Dispatchers.IO) {
                    webDavClient().put("mexpa-backup.zip", temp.readBytes())
                }
                temp.delete()
                busy = false
                message = uploaded.message
            }
        },
        onRestoreWebDav = {
            if (busy) return@BackupRestoreScreenContent
            busy = true
            scope.launch {
                val downloaded = withContext(Dispatchers.IO) { webDavClient().get("mexpa-backup.zip") }
                val bytes = downloaded.bytes
                if (!downloaded.ok || bytes == null) {
                    busy = false
                    message = downloaded.message
                    return@launch
                }
                val temp = File.createTempFile("mexpa-restore", ".zip")
                temp.writeBytes(bytes)
                val restored = withContext(Dispatchers.IO) {
                    LinuxBackup.restore(temp, configDir, state.stateDir)
                }
                temp.delete()
                busy = false
                message = restored.message
                if (restored.ok) restoreCompleted = true
            }
        },
        onExportLocal = {
            val target = pickBackupFile(save = true) ?: return@BackupRestoreScreenContent
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { LinuxBackup.export(target, configDir, state.stateDir, state.subscriptionRepository?.subscriptions().orEmpty()) }
                busy = false
                message = result.message
            }
        },
        onRestoreLocal = {
            val source = pickBackupFile(save = false) ?: return@BackupRestoreScreenContent
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { LinuxBackup.restore(source, configDir, state.stateDir) }
                busy = false
                message = result.message
                if (result.ok) restoreCompleted = true
            }
        },
        onRestartApp = { kotlin.system.exitProcess(0) },
        onBack = onBack,
    )

    if (message != null && !restoreCompleted) {
        WindowDialog(
            show = true,
            title = stringResource(Res.string.backup_title),
            summary = message,
            onDismissRequest = { message = null },
        ) {
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { message = null },
            )
        }
    }
}
