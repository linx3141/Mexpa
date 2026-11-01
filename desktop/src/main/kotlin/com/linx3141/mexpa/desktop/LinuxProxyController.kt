package com.linx3141.mexpa.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

data class LinuxProxyState(
    val running: Boolean = false,
    val starting: Boolean = false,
    val message: String = "未启动",
    val endpoint: String = "",
    val logFile: String = "",
    val mixedPort: Int = 7890,
    val systemProxy: String = "",
    val tun: String = "",
)

internal fun buildMihomoCommand(
    binaryPath: String,
    workDirPath: String,
    configPath: String,
    endpoint: String,
    secret: String,
    ageSecretKey: String?,
    overrideJsonPath: String?,
): List<String> = buildList {
    add(binaryPath)
    addAll(listOf("-d", workDirPath, "-f", configPath, "-ext-ctl", endpoint))
    if (secret.isNotEmpty()) addAll(listOf("--secret", secret))
    ageSecretKey?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--age-secret-key", it)) }
    overrideJsonPath?.let { addAll(listOf("--override-json", it)) }
}

private data class StartParams(
    val configPath: String,
    val binaryPath: String,
    val workDirPath: String,
    val settings: LinuxSettings,
    val ageSecretKey: String? = null,
    /** 启动后立即退出的重试次数（0 = 首次）。 */
    val attempt: Int = 0,
)

/**
 * Linux 运行层：复用 mihomo 的 mixed-port / HTTP controller。
 *
 * 桌面端两个「网络接管」入口：
 * - 系统代理：写入 KDE kioslaverc 或 GNOME/MATE gsettings，停止/退出时按快照恢复；
 * - TUN：`--override-json` 打开 mihomo 的 tun，需要 CAP_NET_ADMIN（一次性 setcap 授权）。
 */
class LinuxProxyController(
    private val stateDir: File = DesktopPlatform.stateDir,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _state = MutableStateFlow(LinuxProxyState())
    val state: StateFlow<LinuxProxyState> = _state.asStateFlow()

    /** external-controller 数据层（Proxy/Log/Connections/Provider/Home/DNS 的真实数据都来自它）。 */
    val data = MihomoDataSource(scope)

    private val _uptimeSeconds = MutableStateFlow(-1L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()
    private var startedAt: java.time.Instant? = null

    private val isWindows = DesktopPlatform.isWindows
    private val isMac = DesktopPlatform.isMac
    private val systemProxy = LinuxSystemProxy(
        configDir = File(System.getProperty("user.home"), ".config"),
        snapshotFile = File(stateDir, "system-proxy.snapshot"),
    )
    private val windowsSystemProxy = WindowsSystemProxy(File(stateDir, "windows-proxy.json"))
    private val macSystemProxy = MacSystemProxy(File(stateDir, "mac-proxy.snapshot"))

    private val selections = SelectionsStore(File(stateDir, "selections.json"))
    private var activeSubscriptionId: String? = null

    /** 由 Main 注入：运行态 age 密钥、当前 active 订阅（selections 归属）。 */
    var ageKeyProvider: (() -> String?)? = null
    var activeSubscriptionProvider: (() -> String?)? = null
    /** 启动前把 active 订阅的覆写合成为运行时 config.yaml（失败返回 null 用原配置）。 */
    var runtimeConfigProvider: ((File) -> File?)? = null

    private var process: Process? = null
    private var watcher: Job? = null
    private var lastStart: StartParams? = null
    private var appliedSettings: LinuxSettings? = null

    init {
        val recovered = when {
            isWindows -> windowsSystemProxy.recoverIfStale()
            isMac -> macSystemProxy.recoverIfStale()
            else -> systemProxy.recoverIfStale()
        }
        recovered?.let { message ->
            _state.update { it.copy(message = message, systemProxy = message) }
        }
        startSleepWatcher()
    }

    /**
     * 桌面休眠/唤醒检测：没有 logind D-Bus 依赖，用墙钟跳变近似。
     * 唤醒后系统代理/TUN 路由可能被网络管理器重置，重放一次接管。
     */
    private fun startSleepWatcher() {
        scope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(30_000)
                val now = System.currentTimeMillis()
                val gap = now - last
                last = now
                if (gap in 90_000..86_400_000) {
                    val settings = lastStart?.settings ?: continue
                    if (_state.value.running) {
                        if (settings.systemProxy) applySystemProxy(_state.value.mixedPort, settings.pacUrl)
                        if (settings.tunEnabled) {
                            restartInternal(lastStart ?: continue)
                        }
                        _state.update {
                            it.copy(message = it.message + "（检测到休眠恢复，已重新应用接管）")
                        }
                    }
                }
            }
        }
    }

    fun systemProxyBackendLabel(): String = when {
        isWindows -> windowsSystemProxy.backendLabel()
        isMac -> macSystemProxy.backendLabel()
        else -> systemProxy.backendLabel()
    }

    fun systemProxySupported(): Boolean = when {
        isWindows -> windowsSystemProxy.available()
        isMac -> macSystemProxy.available()
        else -> systemProxy.detectBackend() != null
    }

    fun tunCapability(binaryPath: String): TunCapability = LinuxTun.capability(resolveBinary(binaryPath))

    fun tunSupportedStacks(binaryPath: String): List<String> = LinuxTun.supportedStacks(resolveBinary(binaryPath))

    /** 阻塞调用（会弹 polkit 授权框），调用方放到 IO 协程。 */
    fun grantTunCapability(binaryPath: String): LinuxTun.ProxyGrantResult =
        LinuxTun.grantCapability(resolveBinary(binaryPath))

    @Synchronized
    fun start(configPath: String, binaryPath: String, workDirPath: String, settings: LinuxSettings) {
        if (process?.isAlive == true || _state.value.starting) return
        scope.launch {
        begin(
            start = StartParams(
                configPath = configPath,
                binaryPath = binaryPath,
                workDirPath = workDirPath,
                settings = settings.normalized(),
                ageSecretKey = ageKeyProvider?.invoke(),
            ),
        )
        }
    }

    /** 运行中切换系统代理/TUN：系统代理即时生效；TUN 需要重启 mihomo。 */
    @Synchronized
    fun update(settings: LinuxSettings) {
        val normalized = settings.normalized()
        val applied = appliedSettings
        if (applied == normalized) return
        val start = lastStart ?: run {
            appliedSettings = normalized
            return
        }
        val tunChanged = applied == null ||
            applied.tunEnabled != normalized.tunEnabled ||
            applied.tunStack != normalized.tunStack ||
            applied.network != normalized.network ||
            applied.perAppMode != normalized.perAppMode ||
            applied.perAppUids != normalized.perAppUids ||
            applied.dnsTakeover != normalized.dnsTakeover
        appliedSettings = normalized
        if (tunChanged) {
            restartInternal(start.copy(settings = normalized))
            return
        }
        if (applied.systemProxy != normalized.systemProxy || applied.pacUrl != normalized.pacUrl) {
            scope.launch {
                if (normalized.systemProxy) {
                    applySystemProxy(_state.value.mixedPort, normalized.pacUrl)
                } else {
                    restoreSystemProxy()
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        val current = process
        val hadProcess = current != null
        _state.update { it.copy(message = "正在停止 mihomo…") }
        data.stop()
        startedAt = null
        _uptimeSeconds.value = -1L
        if (hadProcess) current?.destroy()
        scope.launch {
            var forced = false
            var stuck = false
            withContext(Dispatchers.IO) {
                if (hadProcess && current != null) {
                    if (!current.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        current.destroyForcibly()
                        forced = true
                        stuck = !current.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                    }
                }
                restoreSystemProxy()
            }
            synchronized(this@LinuxProxyController) {
                if (process === current) process = null
                lastStart = null
                appliedSettings = null
                activeSubscriptionId = null
            }
            _state.update {
                it.copy(
                    running = false,
                    starting = false,
                    message = when {
                        stuck -> "mihomo 无法结束（pid=${current?.pid()}），请手动 kill -9"
                        forced -> "已强制停止 mihomo"
                        else -> "已停止"
                    },
                )
            }
            LinuxNotifications.notify("Mexpa", if (stuck) "停止失败：mihomo 未退出" else "代理已停止")
        }
    }

    /** JVM shutdown hook：不启动协程，同步收尾，保证系统代理被恢复。 */
    @Synchronized
    fun shutdownBlocking() {
        process?.let { current ->
            runCatching {
                current.destroy()
                if (!current.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) current.destroyForcibly()
            }
            process = null
        }
        runCatching {
            when {
                isWindows -> windowsSystemProxy.restore()
                isMac -> macSystemProxy.restore()
                else -> systemProxy.restore()
            }
        }
        runCatching { data.stop() }
    }

    private fun begin(start: StartParams) {
        _state.update { it.copy(starting = true, message = "正在准备运行配置…") }
        val originalConfig = resolvePath(start.configPath)
        val workDir = resolvePath(start.workDirPath)
        val config = runtimeConfigProvider?.invoke(originalConfig)?.takeIf { it.isFile } ?: originalConfig
        val log = File(workDir, "mihomo.log")
        if (!config.isFile) {
            fail("配置文件不存在：${config.path}")
            return
        }
        if (!workDir.exists() && !workDir.mkdirs()) {
            fail("无法创建运行目录：${workDir.path}")
            return
        }
        if (!log.parentFile.exists()) log.parentFile.mkdirs()

        val binary = resolveBinary(start.binaryPath)
        if (!binary.isFile) {
            fail("找不到 mihomo 可执行文件：${binary.path}")
            return
        }
        val settings = start.settings
        val network = settings.network
        val tunMessages = mutableListOf<String>()
        if (settings.tunEnabled) {
            when (LinuxTun.capability(binary)) {
                TunCapability.MissingCapability -> {
                    if (isMac) {
                        fail("macOS TUN 需要 root 权限；当前版本请用 sudo 启动 Mexpa，或先使用系统代理模式")
                        return
                    }
                    if (isWindows) {
                        _state.update {
                            it.copy(starting = true, message = "TUN 需要管理员权限，正在请求 Windows UAC…")
                        }
                        val launched = WindowsElevation.relaunchElevated(binary.path)
                        if (!launched) {
                            fail("未获得管理员权限，无法启用 TUN")
                            return
                        }
                        _state.update {
                            it.copy(starting = false, message = "已请求以管理员身份重新启动，旧实例将退出…")
                        }
                        scope.launch {
                            delay(700)
                            kotlin.system.exitProcess(0)
                        }
                        return
                    }
                    // 换包/重装会丢 setcap；直接弹系统 polkit 密码框（图2），授权成功后继续启动
                    _state.update {
                        it.copy(starting = true, message = "TUN 需要 CAP_NET_ADMIN，正在请求系统授权…")
                    }
                    val grant = LinuxTun.grantCapability(binary)
                    if (!grant.ok) {
                        fail("TUN 授权未完成：${grant.message}")
                        return
                    }
                    if (LinuxTun.capability(binary) != TunCapability.Capable) {
                        fail("TUN 授权后仍无法读取 CAP_NET_ADMIN，请检查 setcap 结果")
                        return
                    }
                }

                TunCapability.BinaryMissing -> {
                    fail("找不到 mihomo 可执行文件：${binary.path}")
                    return
                }

                else -> Unit
            }
            val stack = LinuxTun.resolveStack(binary, settings.tunStack)
            if (stack != settings.tunStack) {
                tunMessages += "当前内核不支持 ${settings.tunStack}，已改用 $stack"
            }
            tunMessages += "TUN 已启用（stack=$stack）"
        }
        val configEndpoint = runCatching { MihomoApi.endpointFromConfig(config).first }.getOrDefault("127.0.0.1:9090")
        val endpoint = (network?.externalController?.trim()?.takeIf { it.isNotEmpty() } ?: configEndpoint)
            .replace("0.0.0.0", "127.0.0.1")
        val secret = network?.secret?.trim()?.takeIf { it.isNotEmpty() }
            ?: runCatching { MihomoApi.endpointFromConfig(config).second }.getOrDefault("")
        val overrideFile = runCatching {
            LinuxRuntimeConfig.writeOverrideFile(network, settings, config, binary, workDir)
        }.getOrElse {
            fail("生成运行时覆写失败：${it.message ?: it::class.simpleName}")
            return
        }

        _state.update {
            it.copy(
                starting = true,
                running = false,
                message = "正在启动 mihomo…",
                endpoint = "127.0.0.1:9090",
                logFile = log.path,
                tun = if (settings.tunEnabled) "TUN 启用中…" else "TUN 关闭",
            )
        }

        val mixedPort = network?.mixedPort ?: readMixedPort(config)
        val command = buildMihomoCommand(
            binaryPath = binary.path,
            workDirPath = workDir.path,
            configPath = config.path,
            endpoint = endpoint,
            secret = secret,
            ageSecretKey = start.ageSecretKey,
            overrideJsonPath = overrideFile?.path,
        )

        scope.launch {
            try {
                val started = withContext(Dispatchers.IO) {
                    ProcessBuilder(command)
                        .directory(workDir)
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                        .start()
                }
                synchronized(this@LinuxProxyController) {
                    process = started
                }
                val startedAtInstant = Instant.now()
                watcher = scope.launch {
                    val exit = withContext(Dispatchers.IO) { started.waitFor() }
                    data.stop()
                    startedAt = null
                    _uptimeSeconds.value = -1L
                    restoreSystemProxy()
                    synchronized(this@LinuxProxyController) {
                        if (process === started) {
                            process = null
                            lastStart = null
                            appliedSettings = null
                        }
                    }
                    LinuxNotifications.notify(
                        "Mexpa",
                        if (exit == 0) "mihomo 已退出" else "mihomo 异常退出（code=$exit）",
                    )
                    _state.update {
                        it.copy(
                            running = false,
                            starting = false,
                            tun = "",
                            message = if (exit == 0) {
                                "mihomo 已退出"
                            } else {
                                "mihomo 退出，code=$exit；查看日志 ${log.path}"
                            },
                        )
                    }
                }
                // 进程先短暂存活检查，再交给 data.start 的 /version 轮询做真正的 API ready 判定。
                withContext(Dispatchers.IO) {
                    runCatching { started.waitFor(400, java.util.concurrent.TimeUnit.MILLISECONDS) }
                }
                if (!started.isAlive) {
                    synchronized(this@LinuxProxyController) { if (process === started) process = null }
                    if (start.attempt == 0) {
                        _state.update { it.copy(starting = true, message = "mihomo 启动后立即退出，正在重试一次…") }
                        delay(400)
                        begin(start.copy(attempt = 1))
                        return@launch
                    }
                    fail("mihomo 启动后立即退出；日志位于 ${log.path}")
                    return@launch
                }
                val pid = started.pid()
                synchronized(this@LinuxProxyController) {
                    lastStart = start
                    appliedSettings = settings
                }
                startedAt = startedAtInstant
                _uptimeSeconds.value = 0
                val apiReady = runCatching { data.start(config, mixedPort, endpoint, secret) }.getOrDefault(false)
                if (apiReady) {
                    scope.launch {
                        while (isActive) {
                            val startInstant = startedAt ?: break
                            _uptimeSeconds.value = java.time.Duration.between(startInstant, Instant.now()).seconds
                            delay(1_000)
                        }
                    }
                    activeSubscriptionId = activeSubscriptionProvider?.invoke()
                    applyStoredSelections()
                }
                val systemProxyMessage = if (settings.systemProxy) {
                    applySystemProxy(mixedPort, settings.pacUrl)
                } else {
                    "系统代理未启用"
                }
                _state.update {
                    it.copy(
                        starting = false,
                        running = true,
                        message = buildString {
                            append("mihomo 已启动（pid=").append(pid).append("，启动于 ").append(startedAtInstant).append("）")
                            if (tunMessages.isNotEmpty()) append("；").append(tunMessages.joinToString("；"))
                            if (!apiReady) append("；外部控制器未就绪，页面数据可能为空")
                        },
                        mixedPort = mixedPort,
                        systemProxy = systemProxyMessage,
                        tun = if (settings.tunEnabled) "TUN 已启用" else "TUN 关闭",
                    )
                }
                LinuxNotifications.notify("Mexpa", "mihomo 已启动（mixed-port $mixedPort）")
            } catch (t: Throwable) {
                synchronized(this@LinuxProxyController) {
                    process = null
                    lastStart = null
                    appliedSettings = null
                }
                fail("启动失败：${t.message ?: t::class.simpleName}")
            }
        }
    }

    private fun restartInternal(start: StartParams) {
        scope.launch {
            val current = synchronized(this@LinuxProxyController) { process }
            if (current != null) {
                withContext(Dispatchers.IO) {
                    current.destroy()
                    if (!current.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) current.destroyForcibly()
                }
                synchronized(this@LinuxProxyController) { process = null }
            }
            synchronized(this@LinuxProxyController) {
                _state.update { it.copy(starting = false, running = false) }
                begin(start)
            }
        }
    }

    private fun fail(message: String) {
        data.stop()
        LinuxNotifications.notify("Mexpa", message)
        startedAt = null
        _uptimeSeconds.value = -1L
        _state.update { it.copy(starting = false, running = false, message = message, systemProxy = "", tun = "") }
    }

    private fun applySystemProxy(port: Int, pacUrl: String? = null): String {
        val result = when {
            isWindows -> windowsSystemProxy.apply(port, pacUrl)
            isMac -> macSystemProxy.apply(port, pacUrl)
            else -> systemProxy.apply(port, pacUrl)
        }
        _state.update { it.copy(systemProxy = result.message) }
        return result.message
    }

    private fun restoreSystemProxy() {
        val result = when {
            isWindows -> windowsSystemProxy.restore()
            isMac -> macSystemProxy.restore()
            else -> systemProxy.restore()
        }
        _state.update { it.copy(systemProxy = result.message) }
    }

    /** 订阅切换 active 后把运行中的 mihomo 换到新配置文件。 */
    @Synchronized
    fun applyActiveConfig(configPath: String?) {
        if (configPath.isNullOrBlank()) return
        val start = lastStart ?: return
        val nextAgeKey = ageKeyProvider?.invoke()
        if (start.configPath == configPath && start.ageSecretKey == nextAgeKey) return
        val next = start.copy(configPath = configPath, ageSecretKey = nextAgeKey)
        lastStart = next
        if (process?.isAlive == true) restartInternal(next)
    }

    /** FAB「重启」：按上次启动参数重启 mihomo（重新读 config.yaml）。 */
    @Synchronized
    fun restart() {
        val start = lastStart ?: return
        if (process?.isAlive != true) {
            begin(start)
            return
        }
        restartInternal(start)
    }

    /** FAB「重载」：桌面端直接 restart（整机 mihomo 会重新解析配置）。 */
    fun reload() = restart()

    /** 覆写/选择变更后重解析配置并重启（不要求 configPath 改变）。 */
    @Synchronized
    fun reloadConfig() {
        val start = lastStart
        if (start == null) {
            restart()
            return
        }
        if (process?.isAlive == true) {
            restartInternal(start)
        } else {
            scope.launch { begin(start) }
        }
    }

    fun selectProxy(group: String, node: String) {
        data.selectProxy(group, node)
        activeSubscriptionId?.let { selections.put(it, group, node) }
    }

    fun unfixProxy(group: String) {
        data.unfixProxy(group)
        activeSubscriptionId?.let { selections.remove(it, group) }
    }

    private fun applyStoredSelections() {
        val id = activeSubscriptionId ?: return
        val stored = selections.get(id)
        if (stored.isEmpty()) return
        scope.launch {
            // 等首次 /proxies 拉取完成，避免对尚未加载的组发选择请求
            delay(1_200)
            stored.forEach { (group, node) -> data.selectProxy(group, node) }
        }
    }

    fun testNodeDelay(name: String) = data.testNodeDelay(name)

    fun testGroupDelay(group: String) = data.testGroupDelay(group)

    fun closeConnection(id: String) = data.closeConnection(id)

    fun closeAllConnections() = data.closeAllConnections()

    fun updateProvider(name: String, isRule: Boolean) = data.updateProvider(name, isRule)

    fun updateAllProviders() = data.updateAllProviders()

    fun refreshProviderTraffic() = data.refreshProviderTraffic()

    fun queryDns(name: String, type: String) = data.queryDns(name, type)

    fun setDnsQueryName(name: String) = data.setDnsQueryName(name)

    fun setDnsQueryType(type: String) = data.setDnsQueryType(type)

    fun flushCache() = data.flushCache()

    fun flushFakeIp() = data.flushFakeIp()

    fun testLatency() = data.testLatency()

    fun switchMode(mode: String) = data.switchMode(mode)

    private fun readMixedPort(config: File): Int {
        val match = Regex("(?m)^\\s*mixed-port:\\s*(\\d+)").find(config.readText())
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 7890
    }

    private fun resolvePath(path: String): File {
        val expanded = when {
            path == "~" -> System.getProperty("user.home")
            path.startsWith("~/") -> System.getProperty("user.home") + path.removePrefix("~")
            else -> path
        }
        return File(expanded)
    }

    private fun resolveBinary(path: String): File {
        val resolved = resolvePath(path)
        if (resolved.isAbsolute || resolved.parentFile != null) return resolved
        val executable = System.getenv("PATH")
            ?.split(File.pathSeparator)
            ?.asSequence()
            ?.map { File(it, path) }
            ?.firstOrNull { it.canExecute() }
        return executable ?: resolved
    }
}
