package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConnectionInfo
import com.linx3141.mexpa.domain.model.ConnectionsResponse
import com.linx3141.mexpa.domain.model.DnsAnswer
import com.linx3141.mexpa.domain.model.LogMessage
import com.linx3141.mexpa.domain.model.MihomoConfig
import com.linx3141.mexpa.domain.model.ProviderInfo
import com.linx3141.mexpa.domain.model.ProxyNode
import com.linx3141.mexpa.domain.model.SubscriptionInfo
import com.linx3141.mexpa.platform.NetworkAddressType
import com.linx3141.mexpa.viewmodel.ConnectionRate
import com.linx3141.mexpa.viewmodel.ConnectionUiState
import com.linx3141.mexpa.viewmodel.DnsQueryUiState
import com.linx3141.mexpa.viewmodel.IndexedLog
import com.linx3141.mexpa.viewmodel.LatencyProbe
import com.linx3141.mexpa.viewmodel.LogUiState
import com.linx3141.mexpa.viewmodel.MemorySnapshot
import com.linx3141.mexpa.viewmodel.ProviderErrorKey
import com.linx3141.mexpa.viewmodel.ProviderItemUi
import com.linx3141.mexpa.viewmodel.ProviderTrafficInfo
import com.linx3141.mexpa.viewmodel.ProviderUiState
import com.linx3141.mexpa.viewmodel.ProxyUiState
import com.linx3141.mexpa.viewmodel.RefreshProgress
import com.linx3141.mexpa.viewmodel.SpeedSnapshot
import com.linx3141.mexpa.viewmodel.SystemInfoSnapshot
import com.linx3141.mexpa.viewmodel.TRAFFIC_HISTORY_CAPACITY
import com.linx3141.mexpa.viewmodel.TrafficHistory
import com.linx3141.mexpa.viewmodel.VEHICLE_TYPE_COMPATIBLE
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/** 首页需要但不在 HomeUiState 里的聚合信息（来源：/configs、/version、/providers）。 */
data class HomeInfo(
    val mode: String = "rule",
    val version: String = "",
    val config: MihomoConfig? = null,
    val subscription: SubscriptionInfo? = null,
    val providerTraffic: ImmutableList<ProviderTrafficInfo> = persistentListOf(),
    val providerTrafficLoading: Boolean = false,
    val providerTrafficFailed: Boolean = false,
)

data class LatencyState(
    val baidu: Int = -1,
    val cloudflare: Int = -1,
    val google: Int = -1,
    val testing: Boolean = false,
    val viaRules: Boolean = true,
)

private val LEVELS = listOf("debug", "info", "warning", "error")

/**
 * mihomo 运行时数据的适配层：REST 轮询 + WS 推送 → 共享 UI 需要的 StateFlow。
 *
 * 每个上游页面的状态与 Android ViewModel 的语义保持一致：
 * - Proxy 组/节点的 now/fixed/delays 全部来自 /proxies；
 * - Connections 的速率由相邻两次快照差分（mihomo 只给累计字节）；
 * - Logs 按 120ms 合并刷新，避免日志风暴逐行触发重组；
 * - 首页 provider 流量聚合与 Android 的 aggregateProviderInfo 同口径（Total>0 求和，Expire 取最近非零）。
 */
class MihomoDataSource(private val scope: CoroutineScope) {
    private var api: MihomoApi? = null
    private var mixedPort: Int = 7890
    private var logFlushJob: Job? = null
    private var logsDirty = false
    private var logCounter = 0L
    private val logBuffer = ArrayDeque<IndexedLog>()
    private var connectionWs: Job? = null
    private var connectionWatchers = 0
    private var logsWs: Job? = null
    private var logWatchers = 0
    private val pollJobs = mutableListOf<Job>()
    private var lastConnectionSnapshot: Pair<Instant, Map<String, ConnectionInfo>>? = null
    /** /proxies 不含 proxy-provider 节点；这里保存合并来源。 */
    private var providerNodes: Map<String, ProxyNode> = emptyMap()
    private var nodeProviders: Map<String, String> = emptyMap()

    private val _proxyState = MutableStateFlow(ProxyUiState())
    val proxyState: StateFlow<ProxyUiState> = _proxyState.asStateFlow()

    private val _logUiState = MutableStateFlow(LogUiState())
    val logUiState: StateFlow<LogUiState> = _logUiState.asStateFlow()

    private val _logs = MutableStateFlow<ImmutableList<IndexedLog>>(persistentListOf())
    val logs: StateFlow<ImmutableList<IndexedLog>> = _logs.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionUiState())
    val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()

    private val _connectionRates = MutableStateFlow<ImmutableList<ConnectionRate>>(persistentListOf())
    val connectionRates: StateFlow<ImmutableList<ConnectionRate>> = _connectionRates.asStateFlow()

    private val _providerState = MutableStateFlow(ProviderUiState())
    val providerState: StateFlow<ProviderUiState> = _providerState.asStateFlow()

    private val _dnsState = MutableStateFlow(DnsQueryUiState())
    val dnsState: StateFlow<DnsQueryUiState> = _dnsState.asStateFlow()

    private val _speed = MutableStateFlow(SpeedSnapshot())
    val speed: StateFlow<SpeedSnapshot> = _speed.asStateFlow()

    private val _memory = MutableStateFlow(MemorySnapshot())
    val memory: StateFlow<MemorySnapshot> = _memory.asStateFlow()

    private val _systemInfo = MutableStateFlow(SystemInfoSnapshot())
    val systemInfo: StateFlow<SystemInfoSnapshot> = _systemInfo.asStateFlow()

    private val _latency = MutableStateFlow(LatencyState())
    val latency: StateFlow<LatencyState> = _latency.asStateFlow()

    private val _homeInfo = MutableStateFlow(HomeInfo())
    val homeInfo: StateFlow<HomeInfo> = _homeInfo.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private var trafficHistory = TrafficHistory()

    val started: Boolean get() = api != null

    /** 等 external-controller 就绪并挂上所有采集任务；失败返回 false（控制器据此报错）。 */
    suspend fun start(configFile: File, mixedPort: Int, endpoint: String, secret: String): Boolean {
        stop()
        this.mixedPort = mixedPort
        val client = MihomoApi(endpoint, secret, scope)
        var ready = false
        for (attempt in 0 until 40) {
            if (client.version() != null) {
                ready = true
                break
            }
            delay(250)
        }
        if (!ready) {
            client.close()
            return false
        }
        api = client
        _speed.value = SpeedSnapshot(uploadSpeed = "0 B/s", downloadSpeed = "0 B/s")
        // 页面可能先于 mihomo 就绪打开（日志/连接页进入即订阅），就绪后补上订阅
        if (logWatchers > 0) openLogStream(client)
        if (connectionWatchers > 0) {
            openConnectionWatcher(client)
            launchConnectionsSnapshot()
        }
        refreshConfig()
        refreshProxies()
        refreshProviders()
        startPolling()
        return true
    }

    fun stop() {
        pollJobs.forEach { it.cancel() }
        pollJobs.clear()
        logsWs?.cancel()
        logsWs = null
        logWatchers = 0
        connectionWs?.cancel()
        connectionWs = null
        connectionWatchers = 0
        logFlushJob?.cancel()
        logFlushJob = null
        _logUiState.update { it.copy(isConnected = false) }
        _connectionState.update { it.copy(isConnected = false) }
        api?.close()
        api = null
        lastConnectionSnapshot = null
        trafficHistory = TrafficHistory()
        _memory.value = MemorySnapshot()
    }

    // ---------------------------------------------------------------- 数据刷新

    private fun startPolling() {
        pollJobs += scope.launch {
            while (isActive) {
                refreshProxies()
                delay(2_000)
            }
        }
        pollJobs += scope.launch {
            while (isActive) {
                refreshConfig()
                delay(5_000)
            }
        }
        pollJobs += scope.launch {
            while (isActive) {
                refreshProviders()
                delay(5_000)
            }
        }
        pollJobs += scope.launch {
            while (isActive) {
                sampleSystemInfo()
                delay(2_000)
            }
        }
        api?.stream("/traffic") { text -> onTraffic(text) }?.let { pollJobs += it }
        // 只显示 mihomo 核心内存（/memory WS 的 inuse/oslimit），与 Android 一致
        api?.stream("/memory") { text -> onMemory(text) }?.let { pollJobs += it }
    }

    private suspend fun refreshProxies() {
        val client = api ?: return
        val proxies = client.proxies() ?: return
        val mode = _homeInfo.value.mode
        val merged = providerNodes + proxies
        _proxyState.update { current ->
            MihomoApi.buildProxyState(merged, mode, current)
                .copy(testingGroups = current.testingGroups, testingNodes = current.testingNodes)
        }
    }

    private suspend fun refreshConfig() {
        val config = api?.configs() ?: return
        val version = api?.version()?.version.orEmpty()
        _homeInfo.update {
            it.copy(mode = config.mode.lowercase(), config = config, version = version)
        }
        _proxyState.update { it.copy(mode = config.mode.lowercase()) }
    }

    private suspend fun refreshProviders() {
        val client = api ?: return
        val providers = client.providers() ?: return
        val rules = client.ruleProviders()
        val items = buildList {
            providers.providers.values
                .filter { it.vehicleType != VEHICLE_TYPE_COMPATIBLE }
                .forEach { add(it.toProviderItem(isRule = false)) }
            rules?.providers?.values?.forEach {
                add(ProviderItemUi(name = it.name, type = it.type, vehicleType = it.vehicleType, updatedAt = it.updatedAt, isRuleProvider = true))
            }
        }.sortedBy { it.name }
        _providerState.update { it.copy(providers = items.toPersistentList()) }
        providerNodes = providers.providers.values.flatMap { it.proxies }.associateBy { it.name }
        nodeProviders = providers.providers.values.flatMap { provider ->
            provider.proxies.map { it.name to provider.name }
        }.toMap()
        updateHomeProviderInfo(providers.providers.values)
    }

    private fun updateHomeProviderInfo(providers: Collection<ProviderInfo>) {
        val traffic = providers.mapNotNull { provider ->
            val info = provider.subscriptionInfo ?: return@mapNotNull null
            if (info.Total <= 0) return@mapNotNull null
            ProviderTrafficInfo(
                id = provider.name,
                name = provider.name,
                nodeCount = provider.proxies.size,
                updatedAt = provider.updatedAt,
                hasTraffic = true,
                upload = info.Upload,
                download = info.Download,
                total = info.Total,
                expire = info.Expire,
            )
        }.toPersistentList()
        val withTraffic = providers.mapNotNull { it.subscriptionInfo }.filter { it.Total > 0 }
        val subscription = if (withTraffic.isEmpty()) {
            null
        } else {
            SubscriptionInfo(
                Upload = withTraffic.sumOf { it.Upload },
                Download = withTraffic.sumOf { it.Download },
                Total = withTraffic.sumOf { it.Total },
                Expire = withTraffic.map { it.Expire }.filter { it > 0 }.minOrNull() ?: 0,
            )
        }
        _homeInfo.update {
            it.copy(providerTraffic = traffic, subscription = subscription, providerTrafficLoading = false, providerTrafficFailed = false)
        }
    }

    private fun onMemory(text: String) {
        val obj = runCatching { json.parseToJsonElement(text) as JsonObject }.getOrNull() ?: return
        val inuse = (obj["inuse"] as? JsonPrimitive)?.long ?: return
        val oslimit = (obj["oslimit"] as? JsonPrimitive)?.long ?: 0L
        _memory.value = MemorySnapshot(
            ramUsage = com.linx3141.mexpa.util.FormatUtils.formatBytes(inuse),
            ramTotal = if (oslimit > 0) com.linx3141.mexpa.util.FormatUtils.formatBytes(oslimit) else "-- MB",
        )
    }

    private fun sampleSystemInfo() {
        val (ip, name, addressType) = localAddress()
        _systemInfo.value = SystemInfoSnapshot(
            localIp = ip,
            interfaceName = name,
            addressType = addressType,
            cpuUsage = cpuUsage(),
        )
    }

    // ---------------------------------------------------------------- 流量/日志

    private fun onTraffic(text: String) {
        val obj = runCatching { json.parseToJsonElement(text) as JsonObject }.getOrNull() ?: return
        val up = (obj["up"] as? JsonPrimitive)?.long ?: return
        val down = (obj["down"] as? JsonPrimitive)?.long ?: return
        val history = trafficHistory.let {
            TrafficHistory(
                up = (it.up + up).takeLast(TRAFFIC_HISTORY_CAPACITY).toPersistentList(),
                down = (it.down + down).takeLast(TRAFFIC_HISTORY_CAPACITY).toPersistentList(),
                seq = it.seq + 1,
            )
        }
        trafficHistory = history
        _speed.value = SpeedSnapshot(
            uploadSpeed = formatRate(up),
            downloadSpeed = formatRate(down),
            history = history,
        )
    }

    fun startLogStream() {
        if (logWatchers++ > 0) return
        api?.let { openLogStream(it) }
    }

    private fun openLogStream(client: MihomoApi) {
        logsWs?.cancel()
        _logUiState.update { it.copy(isConnected = true) }
        logsWs = client.stream("/logs?level=info") { text -> onLog(text) }
    }

    fun stopLogStream() {
        if (logWatchers <= 0) return
        logWatchers--
        if (logWatchers == 0) {
            logsWs?.cancel()
            logsWs = null
            _logUiState.update { it.copy(isConnected = false) }
        }
    }

    fun clearLogs() {
        logBuffer.clear()
        _logs.value = persistentListOf()
    }

    private fun onLog(text: String) {
        val message = runCatching { json.decodeFromString(LogMessage.serializer(), text) }.getOrNull() ?: return
        logBuffer.addLast(IndexedLog(logCounter++, message))
        while (logBuffer.size > 500) logBuffer.removeFirst()
        if (logsDirty) return
        logsDirty = true
        logFlushJob = scope.launch {
            delay(120)
            _logs.value = logBuffer.toList().toPersistentList()
            logsDirty = false
        }
    }

    // ---------------------------------------------------------------- 连接

    fun startObservingConnections() {
        if (connectionWatchers++ > 0) return
        api?.let { client ->
            openConnectionWatcher(client)
            launchConnectionsSnapshot()
        }
    }

    private fun openConnectionWatcher(client: MihomoApi) {
        connectionWs?.cancel()
        _connectionState.update { it.copy(isConnected = true) }
        connectionWs = client.stream("/connections") { text -> onConnections(text) }
    }

    private fun launchConnectionsSnapshot() {
        scope.launch { refreshConnectionsSnapshot() }
    }

    fun stopObservingConnections() {
        if (connectionWatchers <= 0) return
        connectionWatchers--
        if (connectionWatchers == 0) {
            connectionWs?.cancel()
            connectionWs = null
            lastConnectionSnapshot = null
            _connectionState.update { it.copy(isConnected = false) }
        }
    }

    private fun onConnections(text: String) {
        val response = runCatching { json.decodeFromString(ConnectionsResponse.serializer(), text) }.getOrNull() ?: return
        updateConnections(response)
    }

    private suspend fun refreshConnectionsSnapshot() {
        val response = api?.connections() ?: return
        updateConnections(response)
    }

    private fun updateConnections(response: ConnectionsResponse) {
        val now = Instant.now()
        val previous = lastConnectionSnapshot
        val rates = if (previous == null) {
            persistentListOf()
        } else {
            val elapsed = max(0.001, (now.toEpochMilli() - previous.first.toEpochMilli()) / 1000.0)
            response.connections.mapNotNull { connection ->
                val before = previous.second[connection.id] ?: return@mapNotNull null
                val upload = (((connection.upload - before.upload) / elapsed).coerceAtLeast(0.0)).toLong()
                val download = (((connection.download - before.download) / elapsed).coerceAtLeast(0.0)).toLong()
                if (upload == 0L && download == 0L) return@mapNotNull null
                ConnectionRate(
                    id = connection.id,
                    host = connection.metadata.host.ifBlank { connection.metadata.destinationIP },
                    node = connection.chains.firstOrNull().orEmpty(),
                    uploadRate = upload,
                    downloadRate = download,
                )
            }.sortedByDescending { it.uploadRate + it.downloadRate }.take(8).toPersistentList()
        }
        lastConnectionSnapshot = now to response.connections.associateBy { it.id }
        _connectionRates.value = rates
        _connectionState.update {
            it.copy(
                connections = response.connections.toPersistentList(),
                downloadTotal = response.downloadTotal,
                uploadTotal = response.uploadTotal,
                isConnected = true,
            )
        }
    }

    // ---------------------------------------------------------------- 操作

    fun selectProxy(group: String, node: String) {
        val client = api ?: return
        scope.launch {
            runCatching { client.selectProxy(group, node) }
            refreshProxies()
        }
    }

    fun unfixProxy(group: String) {
        val client = api ?: return
        scope.launch {
            runCatching { client.selectProxy(group, "") }
            refreshProxies()
        }
    }

    fun testNodeDelay(name: String) {
        val client = api ?: return
        _proxyState.update { it.copy(testingNodes = (it.testingNodes + name).toPersistentSet()) }
        scope.launch {
            val provider = nodeProviders[name]
            val delay = if (provider != null) client.testProviderNodeDelay(provider, name) else client.testNodeDelay(name)
            _proxyState.update { state ->
                val groups = state.groups.map { group ->
                    if (name in group.all) {
                        val delays = if (delay != null && delay > 0) {
                            (group.delays + (name to delay)).toPersistentMap()
                        } else {
                            (group.delays - name).toPersistentMap()
                        }
                        group.copy(delays = delays)
                    } else {
                        group
                    }
                }.toPersistentList()
                state.copy(groups = groups, testingNodes = (state.testingNodes - name).toPersistentSet())
            }
        }
    }

    fun testGroupDelay(group: String) {
        val client = api ?: return
        val target = _proxyState.value.groups.firstOrNull { it.name == group } ?: return
        _proxyState.update { it.copy(testingGroups = (it.testingGroups + group).toPersistentSet()) }
        scope.launch {
            val results = coroutineScope {
                target.all.chunked(8).flatMap { chunk ->
                    chunk.map { node ->
                        async(Dispatchers.IO) {
                            val provider = nodeProviders[node]
                            node to if (provider != null) client.testProviderNodeDelay(provider, node) else client.testNodeDelay(node)
                        }
                    }.awaitAll()
                }
            }
            _proxyState.update { state ->
                val groups = state.groups.map { item ->
                    if (item.name != group) return@map item
                    val delays = item.delays.toMutableMap()
                    results.forEach { (node, delay) ->
                        if (delay != null && delay > 0) delays[node] = delay else delays.remove(node)
                    }
                    item.copy(delays = delays.toPersistentMap())
                }.toPersistentList()
                state.copy(groups = groups, testingGroups = (state.testingGroups - group).toPersistentSet())
            }
        }
    }

    fun closeConnection(id: String) {
        val client = api ?: return
        scope.launch {
            runCatching { client.closeConnection(id) }
            refreshConnectionsSnapshot()
        }
    }

    fun closeAllConnections() {
        val client = api ?: return
        scope.launch {
            runCatching { client.closeAllConnections() }
            refreshConnectionsSnapshot()
        }
    }

    fun updateProvider(name: String, isRuleProvider: Boolean) {
        val client = api ?: return
        scope.launch {
            _providerState.update { it.copy(refresh = RefreshProgress(completed = 0, total = 1, singleName = name)) }
            val result = runCatching { client.updateProvider(name, isRuleProvider) }
            _providerState.update {
                it.copy(
                    refresh = null,
                    providerErrors = if (result.isFailure) {
                        (it.providerErrors + (ProviderErrorKey(name, isRuleProvider) to errorText(result.exceptionOrNull())))
                            .toPersistentMap()
                    } else {
                        (it.providerErrors - ProviderErrorKey(name, isRuleProvider)).toPersistentMap()
                    },
                )
            }
            refreshProviders()
        }
    }

    fun updateAllProviders() {
        val client = api ?: return
        val all = _providerState.value.providers
        if (all.isEmpty()) return
        scope.launch {
            var completed = 0
            val errors = mutableMapOf<ProviderErrorKey, String>()
            _providerState.update { it.copy(refresh = RefreshProgress(0, all.size)) }
            for (provider in all) {
                val result = runCatching { client.updateProvider(provider.name, provider.isRuleProvider) }
                if (result.isFailure) {
                    errors[ProviderErrorKey(provider.name, provider.isRuleProvider)] = errorText(result.exceptionOrNull())
                }
                completed++
                _providerState.update { it.copy(refresh = RefreshProgress(completed, all.size)) }
            }
            _providerState.update {
                it.copy(refresh = null, providerErrors = errors.toPersistentMap())
            }
            refreshProviders()
        }
    }

    fun refreshProviderTraffic() {
        val client = api ?: return
        _homeInfo.update { it.copy(providerTrafficLoading = true) }
        scope.launch {
            val providers = client.providers()
            if (providers == null) {
                _homeInfo.update { it.copy(providerTrafficLoading = false, providerTrafficFailed = true) }
                return@launch
            }
            updateHomeProviderInfo(providers.providers.values)
        }
    }

    fun queryDns(name: String, type: String) {
        val client = api ?: return
        _dnsState.update { it.copy(queryName = name, queryType = type, isQuerying = true, error = "", answers = persistentListOf()) }
        scope.launch {
            val response = client.dnsQuery(name, type)
            if (response == null) {
                _dnsState.update { it.copy(isQuerying = false, error = "DNS 查询失败") }
            } else {
                _dnsState.update {
                    it.copy(
                        isQuerying = false,
                        answers = response.Answer.map { answer -> DnsAnswer(name = answer.name, data = answer.data, type = answer.type, TTL = answer.TTL) }.toPersistentList(),
                        status = response.Status,
                    )
                }
            }
        }
    }

    fun setDnsQueryName(name: String) {
        _dnsState.update { it.copy(queryName = name) }
    }

    fun setDnsQueryType(type: String) {
        _dnsState.update { it.copy(queryType = type) }
    }

    fun flushCache() {
        api?.let { client -> scope.launch { runCatching { client.flushCache() } } }
    }

    fun flushFakeIp() {
        api?.let { client -> scope.launch { runCatching { client.flushFakeIp() } } }
    }

    fun switchMode(mode: String) {
        val client = api ?: return
        scope.launch {
            runCatching { client.patchMode(mode) }
            refreshConfig()
            refreshProxies()
        }
    }

    /** 首页延迟体检：经本机 mixed-port 发真实 HTTP 请求（走规则引擎，和 Android 的 RuleLatencyTester 同口径）。 */
    fun testLatency() {
        if (api == null) return
        _latency.update { it.copy(testing = true) }
        scope.launch {
            // 经本机 mixed-port 发真实 HTTP 请求：出口由 mihomo 规则决定，
            // 不能用 127.0.0.1:port/version 之类的本地探测（那只是到 mixed listener 的本地往返，必然 0–2ms）。
            val results = LatencyProbe.entries.map { probe ->
                async(Dispatchers.IO) { probe to measureLatency(probe.url, mixedPort) }
            }.awaitAll().toMap()
            val baidu = results[LatencyProbe.Baidu] ?: -1
            val cloudflare = results[LatencyProbe.Cloudflare] ?: -1
            val google = results[LatencyProbe.Google] ?: -1
            _latency.value = LatencyState(
                baidu = baidu,
                cloudflare = cloudflare,
                google = google,
                testing = false,
                viaRules = mixedPort > 0,
            )
        }
    }

    /** 每次新建 client：复用连接池会省掉握手，测出的数字逐次偏低。 */
    private suspend fun measureLatency(url: String, port: Int): Int {
        if (port <= 0) return -1
        val client = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 5_000
                socketTimeoutMillis = 5_000
            }
            followRedirects = false
            engine { proxy = ProxyBuilder.http(Url("http://127.0.0.1:$port")) }
        }
        return try {
            val started = System.nanoTime()
            client.get(url)
            ((System.nanoTime() - started) / 1_000_000).toInt().coerceAtLeast(0)
        } catch (_: Throwable) {
            -1
        } finally {
            client.close()
        }
    }

    private fun formatRate(bytesPerSecond: Long): String = when {
        bytesPerSecond >= 1024L * 1024 * 1024 -> String.format("%.2f GB/s", bytesPerSecond / 1024.0 / 1024 / 1024)
        bytesPerSecond >= 1024L * 1024 -> String.format("%.2f MB/s", bytesPerSecond / 1024.0 / 1024)
        bytesPerSecond >= 1024L -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B/s"
    }

    private fun errorText(error: Throwable?): String = error?.message ?: error?.let { it::class.simpleName }.orEmpty()

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    /** /proc/stat 两次采样差分；容器/受限环境里 ManagementFactory 的 cpuLoad 常返回 -1。 */
    private fun cpuUsage(): String {
        val fields = runCatching {
            File("/proc/stat").readLines().firstOrNull { it.startsWith("cpu ") }
                ?.trim()?.split(Regex("\\s+"))?.drop(1)?.mapNotNull { it.toLongOrNull() }
        }.getOrNull() ?: return "--%"
        if (fields.size < 4) return "--%"
        val total = fields.sum()
        val idle = fields[3] + (fields.getOrNull(4) ?: 0L)
        val deltaTotal = total - lastCpuTotal
        val deltaIdle = idle - lastCpuIdle
        lastCpuTotal = total
        lastCpuIdle = idle
        if (deltaTotal <= 0L || deltaIdle < 0L) return "--%"
        return "${((1.0 - deltaIdle.toDouble() / deltaTotal) * 100).roundToInt()}%"
    }

    private fun localAddress(): Triple<String, String, NetworkAddressType> {
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .forEach { network ->
                    val addresses = network.inetAddresses.toList().filter { !it.isLoopbackAddress }
                    val address = addresses.firstOrNull { it.address.size == 4 } ?: addresses.firstOrNull()
                    if (address != null) {
                        val type = when {
                            network.name.contains("tun", ignoreCase = true) || network.name == "Meta" -> NetworkAddressType.Tun
                            address.isSiteLocalAddress -> NetworkAddressType.Lan
                            else -> NetworkAddressType.Wan
                        }
                        return Triple(address.hostAddress.orEmpty(), network.name, type)
                    }
                }
        }
        return Triple("--", "--", NetworkAddressType.Unknown)
    }
}


/** ProviderInfo → ProviderList UI 项。 */
fun ProviderInfo.toProviderItem(isRule: Boolean) = ProviderItemUi(
    name = name,
    type = type,
    vehicleType = vehicleType,
    updatedAt = updatedAt,
    isRuleProvider = isRule,
)
