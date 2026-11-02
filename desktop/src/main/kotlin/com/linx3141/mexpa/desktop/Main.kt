@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.linx3141.mexpa.desktop

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.linx3141.mexpa.ui.component.design.LocalTopBarActions
import com.linx3141.mexpa.ui.component.design.LocalTopBarDragModifier
import com.linx3141.mexpa.ui.component.design.rememberTopBarDragModifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import com.linx3141.mexpa.ui.screen.proxy.LocalProxyIconLoader
import java.awt.Frame
import java.awt.event.ComponentAdapter
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.linx3141.mexpa.ui.component.design.ExpressiveNavigationBar
import com.linx3141.mexpa.ui.component.design.ExpressiveNavigationRail
import com.linx3141.mexpa.ui.component.design.FloatingExpressiveNavigationBar
import com.linx3141.mexpa.ui.component.design.MishkaNavigationItem
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.MishkaTheme
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import com.linx3141.mexpa.shared.resources.Res
import com.linx3141.mexpa.shared.resources.mexpa_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.event.WindowStateListener
import kotlin.math.roundToInt

// 手机比例外壳的最小窗口尺寸（逻辑 dp）。无边框窗口不带尺寸提示，不设置时 KDE 能把它拉到 1px。
private const val MIN_WINDOW_WIDTH_DP = 360f
private const val MIN_WINDOW_HEIGHT_DP = 600f

/** 无边框窗口的圆角半径；最大化时归零，避免屏幕边角露出透明缺口。 */
private val WINDOW_CORNER_RADIUS = 12.dp

private val NavigationItems = listOf(
    MishkaNavigationItem("主页", Icons.Rounded.Home, Icons.Rounded.Home),
    MishkaNavigationItem("代理", Icons.Rounded.Tune, Icons.Rounded.Tune),
    MishkaNavigationItem("订阅", Icons.Rounded.CloudUpload, Icons.Rounded.CloudUpload),
    MishkaNavigationItem("设置", Icons.Rounded.Settings, Icons.Rounded.Settings),
)

private sealed interface DesktopRoute : NavKey {
    data object Main : DesktopRoute
    data class Sub(val page: SubPage) : DesktopRoute
}

private class DesktopRuntimeBridge {
    var trayAvailable = false
    var closeToTray = true
    var hideWindow: () -> Unit = {}
    var showWindow: () -> Unit = {}
}

fun main(args: Array<String>) {
    // 无窗口自检模式：不进入 Compose application，验证完直接退出（见 LinuxDiagnostics）
    LinuxDiagnostics.runIfRequested()
    val deepLinkUrl = parseDeepLink(args)
    if (deepLinkUrl != null) {
        val response = ControlClient.send("import $deepLinkUrl")
        if (response != null) {
            println(response)
            return
        }
        // 没有运行中的实例：继续正常启动，等仓库就绪后导入
    }
    parseAutomationCommand(args)?.let { command ->
        val response = ControlClient.send(command)
        if (response == null) {
            System.err.println("Mexpa 未在运行（control.sock 不可达）")
            kotlin.system.exitProcess(1)
        }
        println(response)
        return
    }
    val elevatedStart = args.contains("--elevated-start")
    // 单实例：多开会让两个 mihomo 抢同一端口/TUN；MEXPA_ALLOW_MULTI=1 供开发/测试绕过
    var instanceLock = SingleInstance.tryAcquire(DesktopPlatform.stateDir)
    if (instanceLock == null && elevatedStart) {
        // 旧实例在拿到 UAC 成功返回后才退出，给锁释放留出窗口。
        repeat(50) {
            if (instanceLock != null) return@repeat
            Thread.sleep(100)
            instanceLock = SingleInstance.tryAcquire(DesktopPlatform.stateDir)
        }
    }
    if (instanceLock == null && System.getenv("MEXPA_ALLOW_MULTI") != "1") {
        val shown = ControlClient.send("show")
        if (shown != null) {
            println("Mexpa 已在运行，已唤出窗口")
        } else {
            System.err.println("Mexpa 已在运行（单实例锁），退出")
        }
        return
    }
    val minimized = args.contains("--minimized")
    application {

    // XWayland 不会把 Wayland 的分数缩放传给 AWT：工具包仍报 96dpi，
    // 因此显式读 Xft.dpi，把 Compose density 和初始窗口尺寸一起放大。
    val uiScale = remember { detectUiScale() }
    val initialPage = System.getenv("MEXPA_START_PAGE")?.let { name ->
        runCatching { SubPage.valueOf(name) }.getOrNull()
    }
    val initialTab = System.getenv("MEXPA_START_TAB")?.let { name ->
        runCatching { MainTab.valueOf(name) }.getOrNull()
    } ?: MainTab.Home
    val state = remember { DesktopAppState(initialPage, initialTab) }
    val runtimeBridge = remember { DesktopRuntimeBridge() }
    var windowVisible by remember { mutableStateOf(!minimized) }
    // 保存的是「基准 dp」：px / uiScale，恢复时再乘回当前 uiScale
    val screens = remember {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration.bounds }
    }
    val maxScreenWidth = screens.maxOfOrNull { it.width } ?: 0
    val maxScreenHeight = screens.maxOfOrNull { it.height } ?: 0
    val savedWidthPx = ((state.linuxSettings.windowWidth ?: 420) * uiScale).toInt()
    val savedHeightPx = ((state.linuxSettings.windowHeight ?: 900) * uiScale).toInt()
    val restoredWidthPx = if (maxScreenWidth > 0) savedWidthPx.coerceAtMost(maxScreenWidth) else savedWidthPx
    val restoredHeightPx = if (maxScreenHeight > 0) savedHeightPx.coerceAtMost(maxScreenHeight) else savedHeightPx
    val clampedPosition = state.linuxSettings.windowX?.let { x ->
        clampWindowPosition(x, state.linuxSettings.windowY ?: 0, restoredWidthPx, restoredHeightPx, screens)
    }
    val savedPosition = clampedPosition?.let { (x, y) -> WindowPosition.Absolute(x.dp, y.dp) }
        ?: WindowPosition.PlatformDefault
    val windowState = rememberWindowState(
        width = (restoredWidthPx / uiScale).dp,
        height = (restoredHeightPx / uiScale).dp,
        position = savedPosition,
    )
    Window(
        onCloseRequest = {
            if (runtimeBridge.trayAvailable && runtimeBridge.closeToTray) {
                runtimeBridge.hideWindow()
            } else {
                exitApplication()
            }
        },
        title = DesktopPlatform.windowTitle,
        icon = painterResource(Res.drawable.mexpa_icon),
        decoration = WindowDecoration.Undecorated(),
        // 透明窗口 + 内容圆角裁剪：XWayland 下无边框窗口没有系统圆角，只能自己画。
        // Windows 也保留透明窗口，避免圆角外被不透明窗口底色填成白色；若遇到个别
        // 显卡/Wine 的 X_CopyArea 问题，可用 MEXPA_OPAQUE_WINDOW=1 临时退回不透明。
        transparent = System.getenv("MEXPA_OPAQUE_WINDOW") != "1",
        visible = windowVisible,
        state = windowState,
    ) {
        LaunchedEffect(window, uiScale) {
            // 透明窗口的 AWT 底色必须是全透明，否则圆角外会露出默认白色。
            window.background = java.awt.Color(0, 0, 0, 0)
            window.minimumSize = Dimension(
                (MIN_WINDOW_WIDTH_DP * uiScale).roundToInt(),
                (MIN_WINDOW_HEIGHT_DP * uiScale).roundToInt(),
            )
        }
        var maximized by remember { mutableStateOf((window.extendedState and Frame.MAXIMIZED_BOTH) != 0) }
        DisposableEffect(window) {
            val listener = WindowStateListener { event ->
                maximized = (event.newState and Frame.MAXIMIZED_BOTH) != 0
            }
            window.addWindowStateListener(listener)
            onDispose { window.removeWindowStateListener(listener) }
        }
        System.getenv("MEXPA_UI_MODE")?.let { mode ->
            runCatching { UiMode.valueOf(mode) }.getOrNull()?.let { state.themeConfig = state.themeConfig.copy(uiMode = it) }
        }
        if (System.getenv("MEXPA_FLOATING_BOTTOM") == "true") {
            state.themeConfig = state.themeConfig.copy(floatingBottomBar = true, materialFloatingBottomBar = true)
        }
        val controller = remember { LinuxProxyController(stateDir = state.stateDir) }
        val linux by controller.state.collectAsState()
        val boundsScope = rememberCoroutineScope()

        // 窗口位置/尺寸记忆：最大化时不覆盖普通尺寸。
        LaunchedEffect(Unit) {
            clampedPosition?.let { (x, y) ->
                delay(250)
                runCatching { window.setLocation(x, y) }
            }
        }
        DisposableEffect(window) {
            var saveJob: Job? = null
            fun saveBounds() {
                if (window.extendedState and Frame.MAXIMIZED_BOTH != 0) return
                state.updateWindowBounds(
                    window.x,
                    window.y,
                    (window.width / uiScale).roundToInt(),
                    (window.height / uiScale).roundToInt(),
                )
            }
            val listener = object : ComponentAdapter() {
                override fun componentMoved(e: java.awt.event.ComponentEvent) {
                    saveJob?.cancel()
                    saveJob = boundsScope.launch { delay(800); saveBounds() }
                }
                override fun componentResized(e: java.awt.event.ComponentEvent) {
                    saveJob?.cancel()
                    saveJob = boundsScope.launch { delay(800); saveBounds() }
                }
            }
            window.addComponentListener(listener)
            onDispose {
                saveJob?.cancel()
                saveBounds()
                window.removeComponentListener(listener)
            }
        }
        val indicatorProcess = remember { if (DesktopPlatform.isWindows || DesktopPlatform.isMac) null else LinuxAppIndicator.install(state.stateDir) }
        LaunchedEffect(Unit) {
            if (indicatorProcess == null) {
                val desktop = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().uppercase()
                val reason = LinuxAppIndicator.lastFailure ?: "未知原因"
                if ("KDE" in desktop) {
                    LinuxNotifications.notify("Mexpa 托盘", "系统托盘依赖缺失（$reason），已回退 AWT；KDE 下菜单可能不可用")
                }
            }
        }
        val trayIcon = if (indicatorProcess == null) {
            remember {
                LinuxTray.install(
                    window = window,
                    onShow = { runtimeBridge.showWindow() },
                    onStart = {
                        controller.start(
                            configPath = state.configPath,
                            binaryPath = state.binaryPath,
                            workDirPath = state.workDir,
                            settings = state.linuxSettings,
                        )
                    },
                    onStop = { controller.stop() },
                    onExit = ::exitApplication,
                )
            }
        } else {
            null
        }
        DisposableEffect(trayIcon, indicatorProcess) {
            LinuxNotifications.attach(trayIcon)
            runtimeBridge.trayAvailable = trayIcon != null || indicatorProcess != null
            runtimeBridge.hideWindow = {
                windowVisible = false
                window.isVisible = false
                LinuxNotifications.notify("Mexpa", "已最小化到托盘，代理继续运行")
            }
            runtimeBridge.showWindow = {
                windowVisible = true
                window.isVisible = true
                window.toFront()
                window.requestFocus()
            }
            state.showWindowProvider = runtimeBridge.showWindow
            onDispose {
                state.showWindowProvider = null
                LinuxNotifications.attach(null)
                LinuxTray.uninstall(trayIcon)
                LinuxAppIndicator.uninstall(indicatorProcess)
            }
        }
        LaunchedEffect(state.linuxSettings.closeToTray) {
            runtimeBridge.closeToTray = state.linuxSettings.closeToTray
        }
        // 登录自启 --minimized：恢复上次运行；普通启动则按「打开应用自动连接」开关
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(600)
            val settings = state.linuxSettings
            val shouldStart = elevatedStart || settings.autoConnectOnLaunch ||
                (minimized && settings.restoreLastState && settings.lastRunActive)
            if (shouldStart && !controller.state.value.running && !controller.state.value.starting) {
                controller.start(
                    configPath = state.configPath,
                    binaryPath = state.binaryPath,
                    workDirPath = state.workDir,
                    settings = state.linuxSettings,
                )
            }
        }
        LaunchedEffect(state.pendingTheme) {
            if (state.pendingTheme != null) {
                kotlinx.coroutines.delay(400)
                state.commitPendingTheme()
            }
        }
        // 记录上次运行状态，供登录恢复
        LaunchedEffect(linux.running) {
            if (state.linuxSettings.lastRunActive != linux.running) {
                state.updateLinuxSettings(state.linuxSettings.copy(lastRunActive = linux.running))
            }
        }

        // 订阅仓库与 GeoIP 管理：IO scope 跟随窗口生命周期
        val linuxScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
        val subscriptionRepository = remember {
            SubscriptionRepository(
                baseDir = state.stateDir,
                binaryPath = { state.binaryPath },
                scope = linuxScope,
                mixedPortProvider = { controller.state.value.takeIf { it.running }?.mixedPort },
                viaProxyProvider = { state.linuxSettings.updateViaProxy },
                geoDataDirProvider = { File(state.workDir) },
                onActiveChanged = { path ->
                    if (!path.isNullOrBlank()) {
                        state.setActiveConfigPath(path)
                        controller.applyActiveConfig(path)
                    }
                },
            ).also { it.load() }
        }
        val geoDataManager = remember(state.workDir) { GeoDataManager(File(state.workDir), linuxScope) }
        val overridesStore = remember {
            OverridesStore(
                dir = File(DesktopPlatform.configDir, "overrides"),
                scope = linuxScope,
                onChanged = { controller.reloadConfig() },
                proxyPortProvider = { controller.state.value.takeIf { it.running }?.mixedPort },
            ).also { it.load() }
        }
        // 覆写 → 运行时配置：active 订阅勾选的覆写按顺序套用到 config.yaml，再交给 mihomo。
        DisposableEffect(controller, overridesStore, subscriptionRepository) {
            state.overridesStore = overridesStore
            controller.runtimeConfigProvider = { base ->
                val ids = subscriptionRepository.activeOverrideIds()
                if (ids.isEmpty()) {
                    null
                } else {
                    val output = File(state.workDir, "runtime/config.effective.yaml")
                    OverrideRuntime.apply(
                        base = base,
                        overrideIds = ids,
                        store = overridesStore,
                        helper = OverrideRuntime.resolveHelper(state.binaryPath),
                        output = output,
                    ).file
                }
            }
            onDispose {
                controller.runtimeConfigProvider = null
                state.overridesStore = null
            }
        }

        // 控制器不持有仓库：age 密钥 / active 订阅 id 用 lambda 注入，避免反向依赖。
        DisposableEffect(controller, subscriptionRepository) {
            controller.ageKeyProvider = { subscriptionRepository.activeAgeKey() }
            controller.activeSubscriptionProvider = { subscriptionRepository.activeId() }
            onDispose {
                controller.ageKeyProvider = null
                controller.activeSubscriptionProvider = null
            }
        }
        // 外部自动化：Unix socket 控制口（CLI: --status/--start/--stop/--restart/--import URL/--quit）
        DisposableEffect(controller, subscriptionRepository, state.linuxSettings.externalControlAllowed) {
            if (!state.linuxSettings.externalControlAllowed) {
                return@DisposableEffect onDispose { }
            }
            val server = ControlServer.start { command ->
                val parts = command.trim().split(Regex("\\s+"), limit = 2)
                when (parts.firstOrNull()) {
                    "status" -> buildString {
                        val current = controller.state.value
                        append("running=").append(current.running)
                        append(" starting=").append(current.starting)
                        append(" port=").append(current.mixedPort)
                        append(" subscriptions=").append(subscriptionRepository.state.value.subscriptions.size)
                        append(" active=").append(subscriptionRepository.subscriptions().firstOrNull { it.active }?.name.orEmpty())
                        append(" message=").append(current.message)
                    }
                    "start" -> {
                        controller.start(
                            configPath = state.configPath,
                            binaryPath = state.binaryPath,
                            workDirPath = state.workDir,
                            settings = state.linuxSettings,
                        )
                        "ok: start requested"
                    }
                    "stop" -> { controller.stop(); "ok: stop requested" }
                    "restart" -> { controller.restart(); "ok: restart requested" }
                    "import" -> {
                        val url = parts.getOrNull(1)?.trim().orEmpty()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            subscriptionRepository.importUrl("", url, 0, "", "")
                            "ok: import started"
                        } else {
                            "error: usage import <url>"
                        }
                    }
                    "show" -> {
                        javax.swing.SwingUtilities.invokeLater { state.showWindowProvider?.invoke() }
                        "ok: shown"
                    }
                    "hide" -> {
                        javax.swing.SwingUtilities.invokeLater { runtimeBridge.hideWindow() }
                        "ok: hidden"
                    }
                    "quit" -> {
                        kotlin.system.exitProcess(0)
                    }
                    else -> "error: unknown command"
                }
            }
            onDispose { ControlServer.stop(server) }
        }
        LaunchedEffect(Unit) { DesktopIntegration.registerClashScheme() }
        LaunchedEffect(Unit) {
            if (deepLinkUrl != null) {
                kotlinx.coroutines.delay(900)
                subscriptionRepository.importUrl("", deepLinkUrl, 0, "", "")
            }
        }
        DisposableEffect(subscriptionRepository, geoDataManager) {
            state.subscriptionRepository = subscriptionRepository
            state.geoDataManager = geoDataManager
            subscriptionRepository.onActiveConfigUpdated = {
                if (state.linuxSettings.restartAfterUpdate) controller.reloadConfig()
            }
            subscriptionRepository.activeConfigPath()?.let { state.setActiveConfigPath(it) }
            subscriptionRepository.startAutoUpdate { state.linuxSettings.autoUpdateEnabled }
            onDispose {
                subscriptionRepository.onActiveConfigUpdated = null
                state.subscriptionRepository = null
                state.geoDataManager = null
            }
        }
        DisposableEffect(linuxScope) {
            onDispose { linuxScope.cancel() }
        }

        // 接管设置（系统代理 / TUN）落盘在 DesktopAppState；运行层只负责应用与恢复。
        // shutdown hook 兜住 SIGTERM/异常退出，避免系统代理停在本进程已经消失的端口上。
        DisposableEffect(controller) {
            state.settingsListener = { controller.update(it) }
            val shutdownHook = Thread { controller.shutdownBlocking() }
            Runtime.getRuntime().addShutdownHook(shutdownHook)
            onDispose {
                state.settingsListener = null
                runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
                controller.stop()
            }
        }

        val currentDensity = LocalDensity.current
        val scaledDensity = if (currentDensity.density < 1.01f && uiScale > 1.01f) {
            Density(density = currentDensity.density * uiScale, fontScale = currentDensity.fontScale)
        } else {
            currentDensity
        }
        val frame = window
        val proxyIconLoader = remember { DesktopProxyIconLoader.loader() }
        // 桌面鼠标点击容错：Compose 默认 touchSlop 偏小（约 2dp），手一抖就被当成拖动。
        // 对齐桌面惯例（KDE startDragDistance ≈ 10px），统一放大到 6dp，clickable/拖动共用同一阈值。
        val baseViewConfiguration = LocalViewConfiguration.current
        val desktopViewConfiguration = remember(baseViewConfiguration, scaledDensity) {
            object : ViewConfiguration by baseViewConfiguration {
                override val touchSlop: Float = with(scaledDensity) { 6.dp.toPx() }
            }
        }
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalViewConfiguration provides desktopViewConfiguration,
            LocalProxyIconLoader provides proxyIconLoader,
            LocalTopBarActions provides {
                WindowControlActions(
                    frame = frame,
                    onClose = {
                        if (runtimeBridge.trayAvailable && runtimeBridge.closeToTray) {
                            runtimeBridge.hideWindow()
                        } else {
                            exitApplication()
                        }
                    },
                )
            },
            LocalTopBarDragModifier provides { largeTitleBar, collapsedHeight ->
                rememberTopBarDragModifier(frame, largeTitleBar, collapsedHeight)
            },
        ) {
            MishkaTheme(themeConfig = state.themeConfig) {
                val cornerRadius by animateDpAsState(
                    targetValue = if (maximized) 0.dp else WINDOW_CORNER_RADIUS,
                    animationSpec = tween(durationMillis = 150),
                    label = "windowCornerRadius",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(MishkaColors.surface),
                ) {
                    DesktopShell(
                        state = state,
                        linux = linux,
                        controller = controller,
                        onStart = {
                            controller.start(
                                configPath = state.configPath,
                                binaryPath = state.binaryPath,
                                workDirPath = state.workDir,
                                settings = state.linuxSettings,
                            )
                        },
                        onStop = controller::stop,
                    )
                    WindowResizeOverlay(window = frame, enabled = !maximized)
                }
            }
        }
    }
}
}

@Composable
private fun DesktopShell(
    state: DesktopAppState,
    linux: LinuxProxyState,
    controller: LinuxProxyController,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.tab.ordinal,
        pageCount = { MainTab.entries.size },
    )
    val isMiuix = state.themeConfig.uiMode == UiMode.Miuix
    val coroutineScope = rememberCoroutineScope()
    var tabAnimationJob by remember { mutableStateOf<Job?>(null) }
    var tabNavigationGeneration by remember { mutableIntStateOf(0) }
    var tabNavigationInFlight by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val onSelect: (Int) -> Unit = { index ->
            state.selectTab(MainTab.entries[index])
            // 用户在上一段 pager 动画途中再次点底栏时，取消旧动画并开到新目标；
            // 用 generation 防止旧 job 的 finally 把新动画的 in-flight 标记清掉。
            tabNavigationGeneration++
            val generation = tabNavigationGeneration
            tabNavigationInFlight = true
            tabAnimationJob?.cancel()
            tabAnimationJob = coroutineScope.launch {
                try {
                    val spec = spring<Float>(
                        stiffness = Spring.StiffnessMediumLow *
                            state.themeConfig.animationSpeed * state.themeConfig.animationSpeed,
                    )
                    if (isWide) {
                        pagerState.scrollToPage(index)
                    } else {
                        pagerState.animateScrollToPage(page = index, animationSpec = spec)
                    }
                } finally {
                    if (generation == tabNavigationGeneration) {
                        tabNavigationInFlight = false
                    }
                }
            }
        }
        LaunchedEffect(pagerState.settledPage, tabNavigationInFlight) {
            // 程序化动画期间不能让中间页（例如 A→C 途中的 B）回写 tab，
            // 否则底栏会先显示 B、等 pager 停稳后才跳到 C。
            if (!tabNavigationInFlight && state.subPage == null) {
                val pageTab = MainTab.entries[pagerState.settledPage]
                if (state.tab != pageTab) state.selectTab(pageTab)
            }
        }

        if (isWide) {
            var railExpanded by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxSize()) {
                if (state.subPage == null && isMiuix) {
                    MiuixNavigationRail(
                        items = NavigationItems,
                        selectedIndex = state.tab.ordinal,
                        onSelect = onSelect,
                    )
                } else if (state.subPage == null) {
                    ExpressiveNavigationRail(
                        items = NavigationItems,
                        selectedIndex = state.tab.ordinal,
                        onSelect = onSelect,
                        expanded = railExpanded,
                        onExpandedChange = { railExpanded = it },
                    )
                }
                DesktopNavHost(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    state = state,
                    linux = linux,
                    controller = controller,
                    pagerState = pagerState,
                    onStart = onStart,
                    onStop = onStop,
                )
            }
        } else {
            val floatingBottomBar = state.subPage == null && if (isMiuix) {
                state.themeConfig.floatingBottomBar
            } else {
                state.themeConfig.materialFloatingBottomBar
            }
            if (floatingBottomBar) {
                var bottomBarHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                val floatingBackdrop = rememberBlurBackdrop()
                Box(modifier = Modifier.fillMaxSize()) {
                    DesktopNavHost(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (floatingBackdrop != null) {
                                    Modifier.layerBackdrop(floatingBackdrop)
                                } else {
                                    Modifier
                                },
                            ),
                        state = state,
                        linux = linux,
                        controller = controller,
                        pagerState = pagerState,
                        onStart = onStart,
                        onStop = onStop,
                        fabBottomPadding = bottomBarHeight,
                        homeBottomPadding = bottomBarHeight,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { size ->
                                bottomBarHeight = with(density) { size.height.toDp() }
                            },
                    ) {
                        if (isMiuix) {
                            MiuixFloatingBottomNavigation(
                                items = NavigationItems,
                                selectedIndex = state.tab.ordinal,
                                onSelect = onSelect,
                                backdrop = floatingBackdrop,
                                showLabels = state.themeConfig.bottomBarMode == com.linx3141.mexpa.ui.theme.BottomBarMode.IconAndText,
                                liquidGlass = state.themeConfig.floatingBottomBarStyle ==
                                    com.linx3141.mexpa.ui.theme.FloatingBottomBarStyle.IosLike,
                            )
                        } else {
                            FloatingExpressiveNavigationBar(
                                items = NavigationItems,
                                selectedIndex = state.tab.ordinal,
                                onSelect = onSelect,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 10.dp),
                                containerColor = MishkaColors.surfaceContainer,
                                backdrop = floatingBackdrop,
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DesktopNavHost(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        state = state,
                        linux = linux,
                        controller = controller,
                        pagerState = pagerState,
                        onStart = onStart,
                        onStop = onStop,
                    )
                    if (state.subPage == null) {
                        if (isMiuix) {
                            MiuixBottomNavigation(
                                items = NavigationItems,
                                selectedIndex = state.tab.ordinal,
                                onSelect = onSelect,
                            )
                        } else {
                            ExpressiveNavigationBar(
                                items = NavigationItems,
                                selectedIndex = state.tab.ordinal,
                                onSelect = onSelect,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopNavHost(
    modifier: Modifier,
    state: DesktopAppState,
    linux: LinuxProxyState,
    controller: LinuxProxyController,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    fabBottomPadding: Dp = 0.dp,
    homeBottomPadding: Dp = 0.dp,
) {
    val backStack = remember { mutableStateListOf<NavKey>(DesktopRoute.Main) }
    val navController = remember(backStack) { NavController(backStack) }
    LaunchedEffect(state.subPage) {
        when (val page = state.subPage) {
            null -> while (navController.backStack.size > 1) navController.pop()
            else -> {
                val current = navController.backStack.lastOrNull() as? DesktopRoute.Sub
                if (current?.page != page) {
                    if (navController.backStack.size > 1) navController.pop()
                    navController.push(DesktopRoute.Sub(page))
                }
            }
        }
    }
    val baseTransition = NavTransitions.MiuixDefault
    val navTransition = remember(state.themeConfig.animationSpeed) {
        if (state.themeConfig.animationSpeed == 1f) {
            baseTransition
        } else {
            ScaledNavTransition(baseTransition, state.themeConfig.animationSpeed)
        }
    }
    // 只 pop NavController，让 NavDisplay 先播完 pop transition；页面状态延迟清空，
    // 避免同一帧改 state 导致 NavDisplay 直接换页、看不出返回动画。
    val popToMain: () -> Unit = {
        if (navController.backStack.size > 1) navController.pop()
    }
    LaunchedEffect(navController.backStack.size, state.subPage) {
        if (navController.backStack.size == 1 && state.subPage != null) {
            delay(320)
            if (navController.backStack.size == 1) state.back()
        }
    }
    NavDisplay(
        navController = navController,
        onBack = popToMain,
        transition = navTransition,
        modifier = modifier.fillMaxSize(),
    ) {
        entry<DesktopRoute.Main> {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (MainTab.entries[page]) {
                    MainTab.Home -> SharedHomePage(
                        state = state,
                        linux = linux,
                        controller = controller,
                        onStart = onStart,
                        onStop = onStop,
                        fabBottomPadding = fabBottomPadding,
                        bottomPadding = homeBottomPadding,
                    )
                    MainTab.Proxy -> SharedProxyPage(state, controller, homeBottomPadding)
                    MainTab.Subscription -> SharedSubscriptionPage(state, homeBottomPadding)
                    MainTab.Settings -> SharedSettingsPage(state, homeBottomPadding)
                }
            }
        }
        entry<DesktopRoute.Sub> { route ->
            when (route.page) {
                SubPage.Logs -> SharedLogsPage(state, controller, onBack = popToMain)
                SubPage.Connections -> SharedConnectionsPage(state, controller)
                SubPage.Providers -> SharedProviderPage(state, controller)
                SubPage.Dns -> SharedDnsPage(state, controller, onBack = popToMain)
                SubPage.Theme -> SharedThemeSettingsPage(state, onBack = popToMain)
                SubPage.Network -> SharedNetworkPage(state, linux, controller, onBack = popToMain)
                SubPage.ExternalControl -> SharedExternalControlPage(state, onBack = popToMain)
                SubPage.Automation -> SharedAutomationPage(state, onBack = popToMain)
                SubPage.Backup -> SharedBackupPage(state, onBack = popToMain)
                SubPage.FileManager -> SharedFileManagerPage(state, controller, onBack = popToMain)
                SubPage.Overrides -> SharedOverridesPage(state, controller, onBack = popToMain)
                SubPage.About -> SharedAboutPage(controller, onBack = popToMain)
                SubPage.Meta -> SharedMetaSettingsPage(state, onBack = popToMain)
                SubPage.OverrideEdit -> SharedOverrideEditorPage(state, controller, onBack = popToMain)
                SubPage.FileEditor -> SharedFileEditorPage(state, controller, onBack = popToMain)
                SubPage.SubscriptionAdd -> SharedSubscriptionAddPage(state, onBack = popToMain)
                SubPage.SubscriptionAddUrl -> SharedSubscriptionAddUrlPage(state, onBack = popToMain)
                SubPage.SubscriptionEdit -> SharedSubscriptionEditPage(state, onBack = popToMain)
            }
        }
    }
}


// 与 Android AppNavigation 的 ScaledNavTransition 相同，只按用户动画速度缩放 Miuix NavMotion。
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

/** XWayland 下 AWT 默认 96dpi；优先用户覆盖，其次读 Xft.dpi（Wayland 通常设 144/192）。 */
private fun detectUiScale(): Float {
    if (DesktopPlatform.isWindows || DesktopPlatform.isMac) return 1f
    val override = System.getenv("MEXPA_UI_SCALE")?.toFloatOrNull()
    if (override != null && override.isFinite() && override > 0f) {
        return override.coerceIn(1f, 3f)
    }
    val dpi = runCatching {
        val process = ProcessBuilder("xrdb", "-query").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
        Regex("(?m)^Xft\\.dpi:\\s*([0-9]+)").find(output)?.groupValues?.get(1)?.toIntOrNull()
    }.getOrNull()
    return (dpi?.let { it / 96f } ?: 1f).coerceIn(1f, 3f)
}


/** CLI 自动化参数：--status / --start / --stop / --restart / --import URL / --quit。 */
private fun parseAutomationCommand(args: Array<String>): String? {
    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--status", "--start", "--stop", "--restart", "--quit", "--show", "--hide" -> return arg.removePrefix("--")
            "--import" -> {
                val url = args.getOrNull(index + 1) ?: return null
                return "import $url"
            }
        }
        index++
    }
    return null
}


/** 多屏拔插后保存的位置可能落在屏外；返回夹取后的坐标，完全不在任何屏幕则 null。 */
private fun clampWindowPosition(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    screens: List<java.awt.Rectangle>,
): Pair<Int, Int>? {
    if (screens.isEmpty()) return null
    val window = java.awt.Rectangle(x, y, width, height)
    val screen = screens.maxByOrNull { candidate ->
        candidate.intersection(window).let { if (it.isEmpty) 0L else it.width.toLong() * it.height }
    } ?: return null
    if (screen.intersection(window).isEmpty) return null
    val clampedX = x.coerceIn(screen.x - width + 120, screen.x + screen.width - 120)
    val clampedY = y.coerceIn(screen.y - height + 120, screen.y + screen.height - 120)
    return clampedX to clampedY
}

/** `clash://install-config?url=...` / `clashmeta://...`；只支持 URL 导入，不做静默安装。 */
private fun parseDeepLink(args: Array<String>): String? {
    val link = args.firstOrNull { it.startsWith("clash://") || it.startsWith("clashmeta://") } ?: return null
    return runCatching {
        val uri = java.net.URI(link)
        val query = uri.rawQuery.orEmpty()
        query.split('&').firstNotNullOfOrNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2 && parts[0] == "url") {
                java.net.URLDecoder.decode(parts[1], Charsets.UTF_8)
            } else null
        }
    }.getOrNull()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}
