package com.linx3141.mexpa.ui.navigation

import com.linx3141.mexpa.R



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import com.linx3141.mexpa.DeepLinkImportRequest
import com.linx3141.mexpa.platform.BootStartManager
import com.linx3141.mexpa.platform.FilePicker
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.platform.WifiPolicyController
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.design.ExpressiveNavigationBar
import com.linx3141.mexpa.ui.component.design.FloatingExpressiveNavigationBar
import com.linx3141.mexpa.ui.component.design.ExpressiveNavigationRail
import com.linx3141.mexpa.ui.component.design.MishkaNavigationItem
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.liquid.IosLiquidGlassNavigationBar
import com.linx3141.mexpa.ui.screen.connection.ConnectionScreen
import com.linx3141.mexpa.ui.screen.dns.DnsQueryScreen
import com.linx3141.mexpa.ui.screen.home.HomeFabOverlay
import com.linx3141.mexpa.ui.screen.home.HomeScreen
import com.linx3141.mexpa.ui.screen.log.LogScreen
import com.linx3141.mexpa.ui.screen.overrides.OverrideEditScreen
import com.linx3141.mexpa.ui.screen.overrides.OverrideFileEditorScreen
import com.linx3141.mexpa.ui.screen.overrides.OverrideListScreen
import com.linx3141.mexpa.ui.screen.overrides.SubscriptionOverridesScreen
import com.linx3141.mexpa.ui.screen.provider.ProviderScreen
import com.linx3141.mexpa.ui.screen.proxy.ProxyScreen
import com.linx3141.mexpa.ui.screen.settings.AboutScreen
import com.linx3141.mexpa.ui.screen.settings.AppProxyScreen
import com.linx3141.mexpa.ui.screen.settings.BackupRestoreScreen
import com.linx3141.mexpa.ui.screen.settings.AutomationScreen
import com.linx3141.mexpa.ui.screen.settings.ExternalControlScreen
import com.linx3141.mexpa.ui.screen.settings.FileManagerEditorScreen
import com.linx3141.mexpa.ui.screen.settings.FileManagerScreen
import com.linx3141.mexpa.ui.screen.settings.MetaSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.NetworkSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.RootSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.SettingsScreen
import com.linx3141.mexpa.ui.screen.settings.ThemeSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.VpnSettingsScreen
import com.linx3141.mexpa.ui.screen.settings.WifiPolicyScreen
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionAddScreen
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionAddUrlScreen
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionEditScreen
import com.linx3141.mexpa.ui.screen.subscription.SubscriptionScreen
import com.linx3141.mexpa.ui.theme.BottomBarMode
import com.linx3141.mexpa.ui.theme.FloatingBottomBarStyle
import com.linx3141.mexpa.ui.theme.LocalAppDarkMode
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.ThemeConfig
import com.linx3141.mexpa.ui.theme.writeThemeConfig
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.UiMode
import com.linx3141.mexpa.ui.util.rememberIsWideScreen
import com.linx3141.mexpa.viewmodel.AppProxyViewModel
import com.linx3141.mexpa.viewmodel.BackupViewModel
import com.linx3141.mexpa.viewmodel.ConnectionViewModel
import com.linx3141.mexpa.viewmodel.DnsQueryViewModel
import com.linx3141.mexpa.viewmodel.AutomationViewModel
import com.linx3141.mexpa.viewmodel.ExternalControlViewModel
import com.linx3141.mexpa.viewmodel.HomeUiState
import com.linx3141.mexpa.viewmodel.HomeViewModel
import com.linx3141.mexpa.viewmodel.LogViewModel
import com.linx3141.mexpa.viewmodel.MetaSettingsViewModel
import com.linx3141.mexpa.viewmodel.NetworkSettingsViewModel
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel
import com.linx3141.mexpa.viewmodel.ProviderViewModel
import com.linx3141.mexpa.viewmodel.ProxyViewModel
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import com.linx3141.mexpa.ui.component.design.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

// Route 是 @Serializable sealed 层级，直接多态编码整个栈；新增路由自动获得持久化能力，无需手工注册
private val NavBackStackSaver = Saver<SnapshotStateList<NavKey>, List<String>>(
    save = { backStack ->
        backStack.mapNotNull { key ->
            (key as? Route)?.let { Json.encodeToString<Route>(it) }
        }.ifEmpty {
            listOf(Json.encodeToString<Route>(Route.Main))
        }
    },
    restore = { savedRoutes ->
        val restoredRoutes = savedRoutes.mapNotNull { value ->
            runCatching { Json.decodeFromString<Route>(value) }.getOrNull()
        }
        mutableStateListOf<NavKey>().apply {
            if (restoredRoutes.isEmpty()) {
                add(Route.Main)
            } else {
                if (restoredRoutes.first() !is Route.Main) add(Route.Main)
                addAll(restoredRoutes)
            }
        }
    },
)

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

@Composable
fun AppNavigation(
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
    val backStack = rememberSaveable(saver = NavBackStackSaver) { mutableStateListOf(Route.Main) }
    val navigator = remember { Navigator(backStack) }
    val pagerState = rememberPagerState(pageCount = { 4 })
    val useNavigationRail = rememberIsWideScreen()
    val mainPagerState = rememberMainPagerState(
        pagerState = pagerState,
        animatePageChanges = !useNavigationRail,
        animationSpeed = themeConfig.animationSpeed,
    )

    // 深链导入：回 Main 并把 pager 切到订阅 Tab 后直接压预填添加页（订阅管理是 pager Tab，无二级路由）
    LaunchedEffect(deepLinkImport) {
        if (deepLinkImport != null) {
            navigator.popUntil { key -> key is Route.Main }
            mainPagerState.animateToPage(2)
            navigator.push(
                Route.SubscriptionAddUrl(
                    initialUrl = deepLinkImport.url,
                    initialName = deepLinkImport.name,
                    initialIntervalMinutes = deepLinkImport.intervalMinutes,
                )
            )
            onDeepLinkImportConsumed()
        }
    }

    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
        // 切到代理 Tab 时刷新 mihomo 缓存的 history.delay；URL-Test 组的周期测试结果靠这里同步到 UI
        if (mainPagerState.pagerState.currentPage == 1) proxyViewModel?.loadProxies()
    }

    MainScreenBackHandler(mainPagerState, navigator)

    // 横移返回默认启用；设置页开关实时生效（storage 只持久化，本状态驱动 entry 的 swipeDismiss）
    var swipeDismissEnabled by remember {
        mutableStateOf(storage?.getString(StorageKeys.SWIPE_DISMISS, "true") == "true")
    }

    // 横移返回方向是物理方向（不随布局方向镜像），RTL 下要反过来
    val swipeBackDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        NavSwipeDirection.RightToLeft
    } else {
        NavSwipeDirection.LeftToRight
    }
    val swipeDismiss = if (swipeDismissEnabled) swipeBackDirection else null
    val navTransition = remember(themeConfig.animationSpeed) {
        val base = NavTransitions.MiuixDefault
        if (themeConfig.animationSpeed == 1f) base else ScaledNavTransition(base, themeConfig.animationSpeed)
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalMainPagerState provides mainPagerState,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.pop() },
            transition = navTransition,
            effects = NavDisplayEffects(
                cornerClipRadius = rememberNavSystemCornerRadius(),
            ),
        ) {
            entry<Route.Main>(swipeDismiss = swipeDismiss) {
                MainPage(
                    homeViewModel,
                    proxyViewModel,
                    subscriptionViewModel,
                    navigator,
                    mainPagerState,
                    bootStartManager,
                    themeConfig,
                    storage,
                    onHideTaskCardChange,
                    hasRootPermission,
                    useNavigationRail,
                )
            }
            entry<Route.SubscriptionAdd>(swipeDismiss = swipeDismiss) {
                SubscriptionAddScreen(
                    viewModel = subscriptionViewModel,
                    onBack = { navigator.pop() },
                    onPickFile = {
                        filePicker?.pickYamlFile { result ->
                            if (result != null && subscriptionViewModel != null) {
                                subscriptionViewModel.addFromFile(
                                    fileName = result.fileName,
                                    content = result.content,
                                    onComplete = {
                                        navigator.popUntil { key -> key is Route.Main }
                                    },
                                )
                            }
                        }
                    },
                    onNavigateUrl = { navigator.push(Route.SubscriptionAddUrl()) },
                    onScanQR = if (onScanQR != null) {
                        {
                            onScanQR { url ->
                                if (url != null) {
                                    navigator.push(Route.SubscriptionAddUrl(initialUrl = url))
                                }
                            }
                        }
                    } else null,
                )
            }
            entry<Route.SubscriptionAddUrl>(swipeDismiss = swipeDismiss) { route ->
                subscriptionViewModel?.let {
                    SubscriptionAddUrlScreen(
                        viewModel = it,
                        initialUrl = route.initialUrl,
                        initialName = route.initialName,
                        initialIntervalMinutes = route.initialIntervalMinutes,
                        onBack = { navigator.pop() },
                        onSaved = { navigator.popUntil { key -> key is Route.Main } },
                    )
                }
            }
            entry<Route.SubscriptionEdit>(swipeDismiss = swipeDismiss) { route ->
                subscriptionViewModel?.let {
                    SubscriptionEditScreen(
                        uuid = route.uuid,
                        viewModel = it,
                        onBack = { navigator.pop() },
                        onSaved = { navigator.pop() },
                        onNavigateOverrides = { navigator.push(Route.SubscriptionOverrides(route.uuid)) },
                    )
                }
            }
            entry<Route.SubscriptionOverrides>(swipeDismiss = swipeDismiss) { route ->
                if (subscriptionViewModel != null && overrideProfileViewModel != null) {
                    SubscriptionOverridesScreen(
                        subscriptionId = route.uuid,
                        subscriptionViewModel = subscriptionViewModel,
                        overrideViewModel = overrideProfileViewModel,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.OverrideList>(swipeDismiss = swipeDismiss) {
                overrideProfileViewModel?.let { vm ->
                    OverrideListScreen(
                        viewModel = vm,
                        onBack = { navigator.pop() },
                        onAdd = { navigator.push(Route.OverrideEdit()) },
                        onEdit = { id -> navigator.push(Route.OverrideEdit(id)) },
                        onEditFile = { id -> navigator.push(Route.OverrideFileEditor(id)) },
                    )
                }
            }
            entry<Route.OverrideEdit>(swipeDismiss = swipeDismiss) { route ->
                overrideProfileViewModel?.let { vm ->
                    OverrideEditScreen(
                        overrideId = route.id,
                        viewModel = vm,
                        onBack = { navigator.pop() },
                        onSaved = { navigator.pop() },
                        onPickFile = { callback -> filePicker?.pickYamlFile(callback) },
                    )
                }
            }
            entry<Route.OverrideFileEditor>(swipeDismiss = swipeDismiss) { route ->
                overrideProfileViewModel?.let { vm ->
                    OverrideFileEditorScreen(route.id, vm, onBack = { navigator.pop() })
                }
            }
            entry<Route.Log>(swipeDismiss = swipeDismiss) {
                logViewModel?.let {
                    LogScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.Provider>(swipeDismiss = swipeDismiss) {
                providerViewModel?.let {
                    ProviderScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.Connection>(swipeDismiss = swipeDismiss) {
                connectionViewModel?.let {
                    ConnectionScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.DnsQuery>(swipeDismiss = swipeDismiss) {
                dnsQueryViewModel?.let {
                    DnsQueryScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.VpnSettings>(swipeDismiss = swipeDismiss) {
                storage?.let {
                    VpnSettingsScreen(
                        storage = it,
                        isSystemProxySupported = true,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.RootSettings>(swipeDismiss = swipeDismiss) {
                storage?.let {
                    val homeState = homeViewModel?.uiState?.collectAsStateWithLifecycle()?.value
                    RootSettingsScreen(
                        storage = it,
                        isProxyRunning = homeState?.isRunning == true || homeState?.isStarting == true,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.NetworkSettings>(swipeDismiss = swipeDismiss) {
                networkSettingsViewModel?.let {
                    NetworkSettingsScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.MetaSettings>(swipeDismiss = swipeDismiss) {
                metaSettingsViewModel?.let {
                    MetaSettingsScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.AppProxy>(swipeDismiss = swipeDismiss) {
                appProxyViewModel?.let {
                    AppProxyScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.WifiPolicy>(swipeDismiss = swipeDismiss) {
                storage?.let {
                    WifiPolicyScreen(
                        storage = it,
                        controller = wifiPolicyController,
                        onRequestPermission = onRequestWifiPermission,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.ThemeSettings>(swipeDismiss = swipeDismiss) {
                storage?.let {
                    ThemeSettingsScreen(
                        themeConfig = themeConfig,
                        predictiveBackEnabled = it.getString(StorageKeys.PREDICTIVE_BACK, "false") == "true",
                        onThemeConfigChange = { next ->
                            writeThemeConfig(it, next)
                            onThemeConfigChange(next)
                        },
                        onPredictiveBackChange = { checked ->
                            it.putString(StorageKeys.PREDICTIVE_BACK, checked.toString())
                            onPredictiveBackChange?.invoke(checked)
                        },
                        swipeDismissEnabled = swipeDismissEnabled,
                        onSwipeDismissChange = { enabled ->
                            swipeDismissEnabled = enabled
                            it.putString(StorageKeys.SWIPE_DISMISS, enabled.toString())
                        },
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.ExternalControl>(swipeDismiss = swipeDismiss) {
                externalControlViewModel?.let {
                    ExternalControlScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.Automation>(swipeDismiss = swipeDismiss) {
                automationViewModel?.let {
                    AutomationScreen(
                        viewModel = it,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.BackupRestore>(swipeDismiss = swipeDismiss) {
                if (backupViewModel != null && storage != null) {
                    BackupRestoreScreen(
                        viewModel = backupViewModel,
                        storage = storage,
                        filePicker = filePicker,
                        onRestartApp = onRestartApp,
                        onBack = { navigator.pop() },
                    )
                }
            }
            entry<Route.FileManager>(swipeDismiss = swipeDismiss) {
                FileManagerScreen(
                    subscriptionViewModel = subscriptionViewModel,
                    onBack = { navigator.pop() },
                    onOpenFile = { uuid, relPath ->
                        navigator.push(Route.FileManagerEditor(uuid, relPath))
                    },
                )
            }
            entry<Route.FileManagerEditor>(swipeDismiss = swipeDismiss) { route ->
                FileManagerEditorScreen(
                    uuid = route.uuid,
                    relativePath = route.relativePath,
                    subscriptionViewModel = subscriptionViewModel,
                    onBack = { navigator.pop() },
                )
            }
            entry<Route.About>(swipeDismiss = swipeDismiss) {
                val uriHandler = LocalUriHandler.current
                AboutScreen(
                    onBack = { navigator.pop() },
                    mihomoVersion = mihomoVersion,
                    onOpenUrl = { url -> uriHandler.openUri(url) },
                )
            }
        }
    }
}

@Composable
private fun MainPage(
    homeViewModel: HomeViewModel?,
    proxyViewModel: ProxyViewModel?,
    subscriptionViewModel: SubscriptionViewModel?,
    navigator: Navigator,
    mainPagerState: MainPagerState,
    bootStartManager: BootStartManager? = null,
    themeConfig: ThemeConfig = ThemeConfig(),
    storage: PlatformStorage? = null,
    onHideTaskCardChange: ((Boolean) -> Unit)? = null,
    hasRootPermission: Boolean = false,
    useNavigationRail: Boolean = false,
) {
    val homeUiState = homeViewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: HomeUiState()
    SideEffect { mainPagerState.animationSpeed = themeConfig.animationSpeed }
    val selectedPage = mainPagerState.selectedPage
    val interceptPagerGestures =
        themeConfig.pagerInterceptionMode == PagerInterceptionMode.CrossAxisInterceptor
    val nativePagerNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        state = mainPagerState.pagerState,
        orientation = Orientation.Horizontal,
    )
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = mainPagerState.pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec.scaledForSpeed(themeConfig.animationSpeed),
    )

    // 页面主体：手机与宽屏两套外壳共用，仅传入不同的容器 modifier 与底部留白
    val pagerContent: @Composable (Modifier, Dp) -> Unit = { pagerModifier, bottomPadding ->
        HorizontalPager(
            modifier = pagerModifier.pagerGestureOverride(
                pagerState = mainPagerState.pagerState,
                mode = themeConfig.pagerInterceptionMode,
            ),
            state = mainPagerState.pagerState,
            userScrollEnabled = !interceptPagerGestures,
            pageNestedScrollConnection = if (interceptPagerGestures) {
                PagerGestureNestedScrollConnection
            } else {
                nativePagerNestedScrollConnection
            },
            verticalAlignment = Alignment.Top,
            flingBehavior = pagerFlingBehavior,
            overscrollEffect = null,
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    // FAB 悬浮在底栏上方，末尾再留 80dp 避免最后一张卡被它盖住
                    bottomPadding = bottomPadding +
                        if (LocalUiMode.current == UiMode.Material) 80.dp else 0.dp,
                    uiState = homeUiState,
                    viewModel = homeViewModel,
                    onRestart = { homeViewModel?.restartProxy() },
                    onStop = { homeViewModel?.stopProxy() },
                    onReload = { homeViewModel?.reloadConfig() },
                    onTestLatency = { homeViewModel?.testLatency() },
                    onNavigateLog = { navigator.push(Route.Log) },
                    onNavigateProvider = { navigator.push(Route.Provider) },
                    onNavigateConnection = { navigator.push(Route.Connection) },
                    onNavigateDnsQuery = { navigator.push(Route.DnsQuery) },
                    onStartProxy = { homeViewModel?.startProxy() },
                    onSwitchMode = { homeViewModel?.switchMode(it) },
                    onSwitchTunStack = { homeViewModel?.switchTunStack(it) },
                )

                1 -> ProxyScreen(bottomPadding = bottomPadding, viewModel = proxyViewModel)
                2 -> subscriptionViewModel?.let {
                    SubscriptionScreen(
                        viewModel = it,
                        bottomPadding = bottomPadding,
                        onNavigateAdd = { navigator.push(Route.SubscriptionAdd) },
                        onNavigateEdit = { uuid -> navigator.push(Route.SubscriptionEdit(uuid)) },
                        onActiveChanged = { homeViewModel?.onActiveSubscriptionChanged() },
                    )
                }

                3 -> SettingsScreen(
                    bottomPadding = bottomPadding,
                    onNavigateVpnSettings = { navigator.push(Route.VpnSettings) },
                    onNavigateRootSettings = { navigator.push(Route.RootSettings) },
                    onNavigateNetworkSettings = { navigator.push(Route.NetworkSettings) },
                    onNavigateMetaSettings = { navigator.push(Route.MetaSettings) },
                    onNavigateExternalControl = { navigator.push(Route.ExternalControl) },
                    onNavigateAutomation = { navigator.push(Route.Automation) },
                    onNavigateAppProxy = { navigator.push(Route.AppProxy) },
                    onNavigateWifiPolicy = { navigator.push(Route.WifiPolicy) },
                    onNavigateThemeSettings = { navigator.push(Route.ThemeSettings) },
                    onNavigateFileManager = { navigator.push(Route.FileManager) },
                    onNavigateOverrides = { navigator.push(Route.OverrideList) },
                    onNavigateBackup = { navigator.push(Route.BackupRestore) },
                    onNavigateAbout = { navigator.push(Route.About) },
                    bootStartManager = bootStartManager,
                    storage = storage,
                    onHideTaskCardChange = onHideTaskCardChange,
                    hasRootPermission = hasRootPermission,
                    isProxyRunning = homeUiState.isRunning || homeUiState.isStarting,
                )
            }
        }
    }

    val uiMode = LocalUiMode.current
    if (uiMode == UiMode.Material) {
        var homeFabExpanded by remember { mutableStateOf(false) }
        LaunchedEffect(selectedPage, homeUiState.isRunning) {
            if (selectedPage != 0 || !homeUiState.isRunning) homeFabExpanded = false
        }
        // 设置页切换隧道模式只写 storage；回主页时刷新停止态标签
        LaunchedEffect(selectedPage) {
            if (selectedPage == 0) homeViewModel?.refreshTunMode()
        }
        // Home 页仍在视野内时保留 overlay，让它跟随 pager 平移；完全切走后移除
        val homeFabVisible by remember {
            derivedStateOf {
                val pager = mainPagerState.pagerState
                val page = pager.currentPage
                val offset = pager.currentPageOffsetFraction
                page == 0 || (page == 1 && offset < 0f)
            }
        }
        val navHome = stringResource(R.string.nav_home)
        val navProxy = stringResource(R.string.nav_proxy)
        val navSubscription = stringResource(R.string.nav_subscription)
        val navSettings = stringResource(R.string.nav_settings)
        val mishkaNavigationItems = remember(navHome, navProxy, navSubscription, navSettings) {
            listOf(
                MishkaNavigationItem(
                    label = navHome,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                ),
                MishkaNavigationItem(
                    label = navProxy,
                    selectedIcon = Icons.Filled.Tune,
                    unselectedIcon = Icons.Outlined.Tune,
                ),
                MishkaNavigationItem(
                    label = navSubscription,
                    selectedIcon = Icons.Filled.CloudUpload,
                    unselectedIcon = Icons.Outlined.CloudUpload,
                ),
                MishkaNavigationItem(
                    label = navSettings,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                ),
            )
        }

        MaterialMainShell(
            useNavigationRail = useNavigationRail,
            items = mishkaNavigationItems,
            selectedPage = selectedPage,
            onSelect = { index -> mainPagerState.animateToPage(index) },
            pagerContent = pagerContent,
            bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            navRailExpanded = storage?.getString(StorageKeys.NAV_RAIL_EXPANDED, "false") == "true",
            onNavRailExpandedChange = { expanded ->
                storage?.putString(StorageKeys.NAV_RAIL_EXPANDED, expanded.toString())
            },
            floatingBottomBar = themeConfig.materialFloatingBottomBar,
            hideContentSemantics = homeFabVisible && homeFabExpanded,
            overlay = { bottomBarHeight ->
                if (homeFabVisible) {
                    HomeFabOverlay(
                        expanded = homeFabExpanded,
                        onExpandedChange = { homeFabExpanded = it },
                        isRunning = homeUiState.isRunning,
                        isStarting = homeUiState.isStarting,
                        isStopping = homeUiState.isStopping,
                        onStart = { homeViewModel?.startProxy() },
                        onRestart = { homeViewModel?.restartProxy() },
                        onStop = { homeViewModel?.stopProxy() },
                        onReload = { homeViewModel?.reloadConfig() },
                        bottomPadding = bottomBarHeight,
                        pagerState = mainPagerState.pagerState,
                    )
                }
            },
        )
    } else if (useNavigationRail) {
        // 宽屏：侧边 NavigationRail 取代底部 NavigationBar，纵向空间全部让给内容
        val railState = rememberNavigationRailState(
            initialValue = if (storage?.getString(StorageKeys.NAV_RAIL_EXPANDED, "false") == "true") {
                NavigationRailValue.Expanded
            } else {
                NavigationRailValue.Collapsed
            },
        )
        LaunchedEffect(railState.currentValue, storage) {
            storage?.putString(StorageKeys.NAV_RAIL_EXPANDED, railState.isExpanded.toString())
        }
        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
            Row(Modifier.fillMaxSize()) {
                NavigationRail(state = railState) {
                    NavigationRailItem(
                        selected = selectedPage == 0,
                        onClick = { mainPagerState.animateToPage(0) },
                        icon = MiuixIcons.Home,
                        label = stringResource(R.string.nav_home),
                    )
                    NavigationRailItem(
                        selected = selectedPage == 1,
                        onClick = { mainPagerState.animateToPage(1) },
                        icon = MiuixIcons.Tune,
                        label = stringResource(R.string.nav_proxy),
                    )
                    NavigationRailItem(
                        selected = selectedPage == 2,
                        onClick = { mainPagerState.animateToPage(2) },
                        icon = MiuixIcons.UploadCloud,
                        label = stringResource(R.string.nav_subscription),
                    )
                    NavigationRailItem(
                        selected = selectedPage == 3,
                        onClick = { mainPagerState.animateToPage(3) },
                        icon = MiuixIcons.Settings,
                        label = stringResource(R.string.nav_settings),
                    )
                }
                pagerContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // rail 已吸收起始侧的刘海/导航栏 inset；末尾侧无 rail，在此补齐并对后代标记为已消费
                        .consumeWindowInsets(
                            WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                                .only(WindowInsetsSides.Start),
                        )
                        .windowInsetsPadding(
                            WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.End),
                        ),
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                )
            }
        }
    } else {
        val bottomBarBackdrop = rememberBlurBackdrop(themeConfig.blurEnabled)
        val bottomBarBlurActive = bottomBarBackdrop != null
        val barColor = if (bottomBarBlurActive) Color.Transparent else MishkaColors.surface
        val floatingBarColor = if (bottomBarBlurActive) Color.Transparent else MishkaColors.surfaceContainer
        val floatingPillRadius = 50.dp
        val floatingBarShape = RoundedCornerShape(floatingPillRadius)
        val isDark = LocalAppDarkMode.current
        val floatingHighlight = remember(isDark) {
            if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
        }
        val floatingBarModifier = if (bottomBarBackdrop != null) {
            Modifier.textureBlur(
                backdrop = bottomBarBackdrop,
                shape = floatingBarShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(
                            color = MishkaColors.surfaceContainer.copy(alpha = 0.6f),
                        ),
                    ),
                ),
                highlight = floatingHighlight,
            )
        } else {
            Modifier
        }
        val bottomBarDisplayMode = when (themeConfig.bottomBarMode) {
            BottomBarMode.IconAndText -> NavigationBarDisplayMode.IconAndText
            BottomBarMode.IconOnly -> NavigationBarDisplayMode.IconOnly
        }
        val showBottomBarLabels = themeConfig.bottomBarMode == BottomBarMode.IconAndText
        val navigationItems = listOf(
            NavigationItem(label = stringResource(R.string.nav_home), icon = MiuixIcons.Home),
            NavigationItem(label = stringResource(R.string.nav_proxy), icon = MiuixIcons.Tune),
            NavigationItem(label = stringResource(R.string.nav_subscription), icon = MiuixIcons.UploadCloud),
            NavigationItem(label = stringResource(R.string.nav_settings), icon = MiuixIcons.Settings),
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (themeConfig.floatingBottomBar) {
                    if (themeConfig.floatingBottomBarStyle == FloatingBottomBarStyle.IosLike) {
                        IosLiquidGlassNavigationBar(
                            items = navigationItems,
                            selectedIndex = selectedPage,
                            onItemClick = { index -> mainPagerState.animateToPage(index) },
                            backdrop = bottomBarBackdrop,
                            isBlurActive = bottomBarBlurActive,
                            isDark = isDark,
                            showLabels = showBottomBarLabels,
                        )
                    } else {
                        FloatingNavigationBar(
                            modifier = floatingBarModifier,
                            color = floatingBarColor,
                            cornerRadius = floatingPillRadius,
                        ) {
                            navigationItems.forEachIndexed { index, item ->
                                MiuixFloatingNavigationBarItem(
                                    item = item,
                                    selected = selectedPage == index,
                                    onClick = { mainPagerState.animateToPage(index) },
                                    showLabel = showBottomBarLabels,
                                )
                            }
                        }
                    }
                } else {
                    // 渐进模糊仅作用于顶栏；底栏保持高斯
                    BlurredBar(
                        backdrop = bottomBarBackdrop,
                        blurActive = bottomBarBlurActive,
                        blurStyle = TopBarBlurStyle.Gaussian,
                    ) {
                        NavigationBar(
                            color = barColor,
                            mode = bottomBarDisplayMode,
                        ) {
                            navigationItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedPage == index,
                                    onClick = { mainPagerState.animateToPage(index) },
                                    icon = item.icon,
                                    label = item.label,
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            pagerContent(
                if (bottomBarBackdrop != null) {
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(bottomBarBackdrop)
                } else {
                    Modifier.fillMaxSize()
                },
                padding.calculateBottomPadding(),
            )
        }
    }
}

/**
 * Material 模式的主页外壳：手机用 M3E [ExpressiveNavigationBar] 底栏，宽屏用 [ExpressiveNavigationRail]。
 * Miuix 模式的毛玻璃/浮动底栏/可展开侧栏全部保留在下方原分支里，两套外壳互不影响。
 */
@Composable
private fun MaterialMainShell(
    useNavigationRail: Boolean,
    items: List<MishkaNavigationItem>,
    selectedPage: Int,
    onSelect: (Int) -> Unit,
    pagerContent: @Composable (Modifier, Dp) -> Unit,
    bottomInset: Dp,
    navRailExpanded: Boolean,
    onNavRailExpandedChange: (Boolean) -> Unit,
    floatingBottomBar: Boolean = false,
    hideContentSemantics: Boolean = false,
    overlay: @Composable BoxScope.(Dp) -> Unit = {},
) {
    val hiddenSemantics = if (hideContentSemantics) {
        Modifier.semantics { invisibleToUser() }
    } else {
        Modifier
    }
    if (useNavigationRail) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                Row(Modifier.fillMaxSize()) {
                ExpressiveNavigationRail(
                    items = items,
                    selectedIndex = selectedPage,
                    onSelect = onSelect,
                    expanded = navRailExpanded,
                    onExpandedChange = onNavRailExpandedChange,
                    modifier = hiddenSemantics,
                    expandContentDescription = stringResource(R.string.nav_rail_expand),
                    collapseContentDescription = stringResource(R.string.nav_rail_collapse),
                )
                pagerContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .consumeWindowInsets(
                            WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                                .only(WindowInsetsSides.Start),
                        )
                        .windowInsetsPadding(
                            WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.End),
                        )
                        .then(hiddenSemantics),
                    bottomInset,
                )
                }
            }
            overlay(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        }
    } else {
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        if (floatingBottomBar) {
            val floatingBarBackdrop = rememberBlurBackdrop()
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    pagerContent(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (floatingBarBackdrop != null) {
                                    Modifier.layerBackdrop(floatingBarBackdrop)
                                } else {
                                    Modifier
                                },
                            )
                            .then(hiddenSemantics),
                        bottomBarHeight,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size ->
                            bottomBarHeight = with(density) { size.height.toDp() }
                        },
                ) {
                    FloatingExpressiveNavigationBar(
                        items = items,
                        selectedIndex = selectedPage,
                        onSelect = onSelect,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .then(hiddenSemantics),
                        backdrop = floatingBarBackdrop,
                    )
                }
                overlay(bottomBarHeight)
            }
        } else {
            val bottomBarBackdrop = rememberBlurBackdrop()
            val bottomBarBlurActive = bottomBarBackdrop != null
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BlurredBar(
                            backdrop = bottomBarBackdrop,
                            blurActive = bottomBarBlurActive,
                            blurStyle = TopBarBlurStyle.Gaussian,
                        ) {
                            ExpressiveNavigationBar(
                                modifier = Modifier
                                    .onSizeChanged { size ->
                                        bottomBarHeight = with(density) { size.height.toDp() }
                                    }
                                    .then(hiddenSemantics),
                                items = items,
                                selectedIndex = selectedPage,
                                onSelect = onSelect,
                                containerColor = if (bottomBarBlurActive) {
                                    Color.Transparent
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                            )
                        }
                    },
                ) { padding ->
                    pagerContent(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (bottomBarBackdrop != null) {
                                    Modifier.layerBackdrop(bottomBarBackdrop)
                                } else {
                                    Modifier
                                },
                            )
                            .then(hiddenSemantics),
                        padding.calculateBottomPadding(),
                    )
                }
                overlay(bottomBarHeight)
            }
        }
    }
}

@Composable
private fun MiuixFloatingNavigationBarItem(
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val onSurfaceContainerColor = MishkaColors.onSurfaceContainer
    val tint = when {
        isPressed -> onSurfaceContainerColor.copy(alpha = if (selected) 0.7f else 0.5f)
        selected -> onSurfaceContainerColor
        else -> onSurfaceContainerColor.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = if (showLabel) 56.dp else 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = item.icon,
            contentDescription = if (showLabel) null else item.label,
            tint = tint,
        )
        if (showLabel) {
            Text(
                text = item.label,
                color = tint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun SpringSpec<Float>.scaledForSpeed(speed: Float): SpringSpec<Float> =
    if (speed == 1f) {
        this
    } else {
        SpringSpec(
            dampingRatio = dampingRatio,
            stiffness = (stiffness * speed * speed).coerceAtLeast(1f),
            visibilityThreshold = visibilityThreshold,
        )
    }


private class ScaledNavTransition(
    private val base: NavTransition,
    speed: Float,
) : NavTransition by base {
    override val motion: NavMotion = NavMotion(
        commit = base.motion.commit.scaled(speed),
        cancel = base.motion.cancel.scaled(speed),
        programmatic = base.motion.programmatic.scaled(speed),
    )
}

private fun NavSettleSpec.scaled(speed: Float): NavSettleSpec = when (this) {
    is NavSettleSpec.Spring -> copy(stiffness = (stiffness * speed * speed).coerceAtLeast(1f))
    is NavSettleSpec.Tween -> copy(durationMillis = (durationMillis / speed).roundToInt().coerceAtLeast(1))
    else -> this
}

// === MainPagerState ===

@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
    initialAnimationSpeed: Float = 1f,
) {
    var animationSpeed by mutableFloatStateOf(initialAnimationSpeed)

    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null
    // 串行化 pager 动画：上一段被取消的 animateScrollToPage 必须真正释放后，
    // 下一段才能接管；否则连续点 Tab 时后一次动画会被前一次的取消过程吞掉。
    private val navigationMutex = Mutex()

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            navigationMutex.withLock {
                try {
                    if (animatePageChanges) {
                        pagerState.animateScrollToPage(
                            page = targetIndex,
                            animationSpec = PagerNavigationSpringSpec.scaledForSpeed(animationSpeed),
                        )
                    } else {
                        pagerState.scrollToPage(targetIndex)
                    }
                } finally {
                    if (navJob == myJob) {
                        isNavigating = false
                        if (pagerState.currentPage != targetIndex) {
                            selectedPage = pagerState.currentPage
                        }
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    animatePageChanges: Boolean = true,
    animationSpeed: Float = 1f,
): MainPagerState = remember(pagerState, coroutineScope, animatePageChanges) {
    MainPagerState(pagerState, coroutineScope, animatePageChanges, animationSpeed)
}

// === 返回键处理 ===

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navigator: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navigator.current() is Route.Main &&
                    navigator.backStackSize() == 1 &&
                    mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        },
    )
}
