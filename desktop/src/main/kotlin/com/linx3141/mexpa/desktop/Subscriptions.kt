package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.data.repository.ImportProgress
import com.linx3141.mexpa.data.repository.ImportStep
import com.linx3141.mexpa.domain.model.ProfileType
import com.linx3141.mexpa.domain.model.Subscription
import com.linx3141.mexpa.ui.theme.ThemeConfig
import com.linx3141.mexpa.viewmodel.ProfileOperation
import com.linx3141.mexpa.viewmodel.SubscriptionUiState
import com.linx3141.mexpa.viewmodel.UpdateAllProgress
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
data class LocalSubscription(
    val id: String,
    val name: String,
    val url: String = "",
    /** subscriptions/{id}/config.yaml 的绝对路径。 */
    val path: String,
    /** "url" | "file" */
    val type: String = TYPE_URL,
    val active: Boolean = false,
    val intervalMinutes: Long = 0,
    val userAgent: String = "",
    val ageSecretKey: String = "",
    /** 机场 subscription-userinfo 头解析出的流量信息（导入/更新时刷新）。 */
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    /** 应用到该订阅的 OverrideEntry.id（按顺序执行）。 */
    val overrideIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0,
) {
    val isUrl: Boolean get() = type == TYPE_URL

    companion object {
        const val TYPE_URL = "url"
        const val TYPE_FILE = "file"
    }
}

@Serializable
private data class SubscriptionsFile(val subscriptions: List<LocalSubscription> = emptyList())

/**
 * Linux 订阅仓库：JSON 落盘 + mihomo `-t` 校验 + 60s 自动更新调度。
 *
 * 与 Android 的 Room 三阶段管线相比这里刻意简化（P1 范围）：
 * - 下载后先写临时文件，`mihomo -t -f tmp -d tmpdir` 校验通过才原子换入 config.yaml；
 * - 不做 provider prefetch（桌面版内核构建 tags 不含 mishka，没有 `-prefetch`）；
 * - 自动更新是进程内调度，App 退出后不更新（P3 的托盘/后台常驻之后再扩展）。
 */
class SubscriptionRepository(
    private val baseDir: File,
    private val binaryPath: () -> String,
    private val scope: CoroutineScope,
    private val mixedPortProvider: () -> Int? = { null },
    private val viaProxyProvider: () -> Boolean = { true },
    private val geoDataDirProvider: () -> File? = { null },
    private val onActiveChanged: (configPath: String?) -> Unit = {},
    /** active 订阅配置更新后调用（用于自动重启代理）。 */
    var onActiveConfigUpdated: (() -> Unit)? = null,
) {
    private val file = File(baseDir, "subscriptions.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val directClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }
    private val proxiedClients = mutableMapOf<Int, HttpClient>()

    /** 机场常按 UA 返回不同内容（无 UA 时 404/空 body/base64），默认对齐 Android 客户端。 */
    private fun effectiveUserAgent(configured: String): String =
        configured.trim().ifEmpty { DEFAULT_USER_AGENT }

    private fun clientFor(proxyPort: Int?): HttpClient {
        if (proxyPort == null) return directClient
        return proxiedClients.getOrPut(proxyPort) {
            HttpClient(OkHttp) {
                engine {
                    config {
                        proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", proxyPort)))
                    }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 60_000
                }
            }
        }
    }

    private fun proxyPortOrNull(): Int? {
        val runtimePort = if (viaProxyProvider()) mixedPortProvider()?.takeIf { it > 0 } else null
        if (runtimePort != null) return runtimePort
        // 环境变量代理（http_proxy/https_proxy/all_proxy）作为直连失败后的备选
        val envProxy = listOf("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy", "ALL_PROXY", "all_proxy")
            .firstNotNullOfOrNull { System.getenv(it)?.takeIf { url -> url.isNotBlank() } }
            ?: return null
        return Regex(":(\\d+)/?$").find(envProxy)?.groupValues?.get(1)?.toIntOrNull()
    }

    companion object {
        /** 与 Android 客户端一致；机场会按 UA 分流，空 UA 时常见 404/空 body/base64。 */
        const val DEFAULT_USER_AGENT = "ClashMetaForAndroid/2.11.12"
    }

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    private var updateJob: Job? = null
    private var autoUpdateJob: Job? = null

    fun load() {
        val entries = runCatching {
            if (file.isFile) json.decodeFromString(SubscriptionsFile.serializer(), file.readText()).subscriptions else emptyList()
        }.getOrDefault(emptyList())
        publish(entries)
        backfillTrafficHeaders()
    }

    /**
     * 老版本导入的订阅没有保存 subscription-userinfo；启动时用 HEAD 补一次，
     * 避免用户必须先手动刷新每张卡片才能看到流量。
     */
    private fun backfillTrafficHeaders() {
        val missing = currentEntries.filter { it.isUrl && it.total <= 0 && it.url.isNotBlank() }
        if (missing.isEmpty()) return
        scope.launch {
            for (entry in missing) {
                val traffic = runCatching {
                    val response = clientFor(proxyPortOrNull()).head(entry.url) {
                        header("User-Agent", effectiveUserAgent(entry.userAgent))
                    }
                    if (response.status.value !in 200..299) return@runCatching null
                    parseSubscriptionUserInfo(response.headers["Subscription-Userinfo"].orEmpty())
                }.getOrNull() ?: continue
                val updated = currentEntries.map {
                    if (it.id == entry.id && it.total <= 0) it.copy(
                        upload = traffic.upload,
                        download = traffic.download,
                        total = traffic.total,
                        expire = traffic.expire,
                    ) else it
                }
                if (updated != currentEntries) save(updated)
            }
        }
    }

    fun subscriptions(): List<LocalSubscription> = currentEntries

    fun activeConfigPath(): String? = currentEntries.firstOrNull { it.active }?.path

    fun activeId(): String? = currentEntries.firstOrNull { it.active }?.id

    /** 运行态 age 解密密钥：启动/重启 mihomo 时传给 --age-secret-key。 */
    fun activeOverrideIds(): List<String> =
        currentEntries.firstOrNull { it.active }?.overrideIds.orEmpty()

    fun setOverrides(subscriptionId: String, overrideIds: List<String>) {
        val entries = currentEntries.map {
            if (it.id == subscriptionId) it.copy(overrideIds = overrideIds.distinct()) else it
        }
        save(entries)
    }

    fun activeAgeKey(): String? =
        currentEntries.firstOrNull { it.active }?.ageSecretKey?.takeIf { it.isNotEmpty() }

    private var currentEntries: List<LocalSubscription> = emptyList()

    private fun publish(entries: List<LocalSubscription>) {
        currentEntries = entries
        _state.update { it.copy(subscriptions = entries.map { entry -> entry.toUiModel() }.toPersistentList()) }
    }

    private fun save(entries: List<LocalSubscription>) {
        publish(entries)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(SubscriptionsFile.serializer(), SubscriptionsFile(entries)))
        }
    }

    // ---------------------------------------------------------------- 导入 / 更新

    fun importUrl(
        name: String,
        url: String,
        intervalMinutes: Long,
        userAgent: String,
        ageSecretKey: String,
        onComplete: () -> Unit = {},
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        updateJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = "", operation = ProfileOperation.Import, importProgress = ImportProgress(ImportStep.Downloading)) }
            try {
                val id = UUID.randomUUID().toString()
                val target = File(File(baseDir, "subscriptions/$id"), "config.yaml")
                target.parentFile.mkdirs()
                val result = downloadAndValidate(
                    source = Source.Url(cleanUrl, userAgent),
                    target = target,
                    ageSecretKey = ageSecretKey,
                )
                when (result) {
                    is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.message, operation = null, importProgress = null) }
                    is Result.Success -> {
                        val entry = LocalSubscription(
                            id = id,
                            name = name.trim().ifEmpty { result.displayName.ifBlank { "远程订阅" } },
                            url = cleanUrl,
                            path = target.path,
                            type = LocalSubscription.TYPE_URL,
                            intervalMinutes = intervalMinutes,
                            userAgent = userAgent,
                            ageSecretKey = ageSecretKey,
                            upload = (result as? Result.Success)?.traffic?.upload ?: 0,
                            download = (result as? Result.Success)?.traffic?.download ?: 0,
                            total = (result as? Result.Success)?.traffic?.total ?: 0,
                            expire = (result as? Result.Success)?.traffic?.expire ?: 0,
                            updatedAt = System.currentTimeMillis(),
                        )
                        val entries = currentEntries + entry
                        val activated = if (entries.none { it.active }) activateFirst(entries) else entries
                        save(activated)
                        _state.update { it.copy(isLoading = false, operation = null, importProgress = null) }
                        activated.firstOrNull { it.active && it.id == entry.id }?.let { onActiveChanged(it.path) }
                        onComplete()
                    }
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isLoading = false, operation = null, importProgress = null) }
                throw cancelled
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: t::class.simpleName.orEmpty(), operation = null, importProgress = null) }
            }
        }
    }

    fun importFile(file: File, onComplete: () -> Unit = {}) {
        if (!file.isFile) return
        updateJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = "", operation = ProfileOperation.Import, importProgress = ImportProgress(ImportStep.Validating)) }
            try {
                val id = UUID.randomUUID().toString()
                val target = File(File(baseDir, "subscriptions/$id"), "config.yaml")
                target.parentFile.mkdirs()
                val result = runCatching {
                    target.writeBytes(file.readBytes())
                    validate(target, ageSecretKey = "")
                }.getOrElse { errorResult(it) }
                if (result is Result.Failure) {
                    target.parentFile.deleteRecursively()
                    _state.update { it.copy(isLoading = false, error = result.message, operation = null, importProgress = null) }
                    return@launch
                }
                val entry = LocalSubscription(
                    id = id,
                    name = file.nameWithoutExtension.ifBlank { "本地配置" },
                    path = target.path,
                    type = LocalSubscription.TYPE_FILE,
                    updatedAt = System.currentTimeMillis(),
                )
                val entries = currentEntries + entry
                val activated = if (entries.none { it.active }) activateFirst(entries) else entries
                save(activated)
                _state.update { it.copy(isLoading = false, operation = null, importProgress = null) }
                activated.firstOrNull { it.active && it.id == entry.id }?.let { onActiveChanged(it.path) }
                onComplete()
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isLoading = false, operation = null, importProgress = null) }
                throw cancelled
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: t::class.simpleName.orEmpty(), operation = null, importProgress = null) }
            }
        }
    }

    fun update(id: String) {
        val entry = currentEntries.firstOrNull { it.id == id } ?: return
        updateJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = "", operation = ProfileOperation.Update, importProgress = ImportProgress(ImportStep.Downloading)) }
            try {
                val result = refreshEntry(entry)
                _state.update {
                    it.copy(
                        isLoading = false,
                        operation = null,
                        importProgress = null,
                        error = if (result is Result.Failure) result.message else "",
                    )
                }
                if (result is Result.Success && entry.active) {
                    onActiveChanged(entry.path)
                    onActiveConfigUpdated?.invoke()
                }
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isLoading = false, operation = null, importProgress = null) }
                throw cancelled
            }
        }
    }

    fun updateAll() {
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            val total = currentEntries.size
            var completed = 0
            _state.update {
                it.copy(
                    isLoading = true,
                    error = "",
                    operation = ProfileOperation.Update,
                    updateAll = UpdateAllProgress(completed = 0, total = total, currentName = ""),
                )
            }
            var activeUpdated = false
            for (entry in currentEntries) {
                _state.update {
                    it.copy(updateAll = UpdateAllProgress(completed = completed, total = total, currentName = entry.name))
                }
                val result = refreshEntry(entry)
                if (result is Result.Failure) {
                    _state.update { it.copy(error = "${entry.name}: ${result.message}") }
                } else if (entry.active) {
                    onActiveChanged(entry.path)
                    activeUpdated = true
                }
                completed++
                _state.update { it.copy(updateAll = UpdateAllProgress(completed = completed, total = total, currentName = entry.name)) }
            }
            if (activeUpdated) onActiveConfigUpdated?.invoke()
            _state.update { it.copy(isLoading = false, operation = null, updateAll = null) }
        }
    }

    private suspend fun refreshEntry(entry: LocalSubscription): Result {
        return try {
            _state.update { it.copy(importProgress = ImportProgress(ImportStep.Downloading)) }
            val target = File(entry.path)
            val source = if (entry.isUrl) Source.Url(entry.url, entry.userAgent) else Source.Local(target)
            val result = downloadAndValidate(source, target, entry.ageSecretKey, temporaryRename = !entry.isUrl)
            if (result is Result.Success) {
                val traffic = (result as? Result.Success)?.traffic
                val entries = currentEntries.map {
                    if (it.id == entry.id) entry.copy(
                        updatedAt = System.currentTimeMillis(),
                        name = it.name,
                        upload = traffic?.upload ?: entry.upload,
                        download = traffic?.download ?: entry.download,
                        total = traffic?.total ?: entry.total,
                        expire = traffic?.expire ?: entry.expire,
                    ) else it
                }
                save(entries)
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            errorResult(t)
        }
    }

    fun cancelUpdate() {
        updateJob?.cancel()
        updateJob = null
        _state.update { it.copy(isLoading = false, operation = null, importProgress = null, updateAll = null) }
    }

    // ---------------------------------------------------------------- 列表操作

    fun setActive(id: String) {
        val entries = currentEntries.map { it.copy(active = it.id == id) }
        save(entries)
        onActiveChanged(entries.firstOrNull { it.id == id }?.path)
    }

    fun delete(id: String) {
        val removed = currentEntries.firstOrNull { it.id == id } ?: return
        val remaining = currentEntries.filterNot { it.id == id }
        val wasActive = removed.active
        val activated = if (wasActive) activateFirst(remaining) else remaining
        save(activated)
        runCatching { File(removed.path).parentFile?.deleteRecursively() }
        if (wasActive) onActiveChanged(activated.firstOrNull { it.active }?.path)
    }

    fun clearError() {
        _state.update { it.copy(error = "") }
    }

    /** 编辑页保存：只改元数据；URL 变了就顺手重新下载校验（与 Android 的 PATCH 语义近似）。 */
    fun updateMetadata(
        id: String,
        name: String,
        source: String,
        intervalMinutes: Long,
        userAgent: String,
        ageSecretKey: String,
        onComplete: () -> Unit = {},
    ) {
        val entry = currentEntries.firstOrNull { it.id == id } ?: return
        val urlChanged = entry.isUrl && source.trim().isNotEmpty() && source.trim() != entry.url
        val next = entry.copy(
            name = name.trim().ifEmpty { entry.name },
            url = if (entry.isUrl) source.trim() else entry.url,
            intervalMinutes = intervalMinutes,
            userAgent = userAgent,
            ageSecretKey = ageSecretKey,
        )
        updateJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = "", operation = ProfileOperation.Edit, importProgress = null) }
            try {
                if (urlChanged) {
                    val target = File(next.path)
                    val result = downloadAndValidate(Source.Url(next.url, next.userAgent), target, ageSecretKey)
                    if (result is Result.Failure) {
                        _state.update { it.copy(isLoading = false, operation = null, error = result.message) }
                        return@launch
                    }
                }
                val entries = currentEntries.map { if (it.id == id) next.copy(updatedAt = System.currentTimeMillis()) else it }
                save(entries)
                _state.update { it.copy(isLoading = false, operation = null) }
                if (next.active) onActiveChanged(next.path)
                onComplete()
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(isLoading = false, operation = null) }
                throw cancelled
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, operation = null, error = t.message ?: t::class.simpleName.orEmpty()) }
            }
        }
    }

    private fun activateFirst(entries: List<LocalSubscription>): List<LocalSubscription> {
        val first = entries.firstOrNull() ?: return entries
        return entries.map { it.copy(active = it.id == first.id) }
    }

    // ---------------------------------------------------------------- 自动更新

    fun startAutoUpdate(enabled: () -> Boolean) {
        autoUpdateJob?.cancel()
        autoUpdateJob = scope.launch {
            delay(10_000)
            while (true) {
                if (enabled() && updateJob?.isActive != true) {
                    val now = System.currentTimeMillis()
                    val due = currentEntries.filter {
                        it.intervalMinutes > 0 && now - maxOf(it.updatedAt, it.createdAt) >= it.intervalMinutes * 60_000
                    }
                    var activeUpdated = false
                    val failed = mutableListOf<String>()
                    due.forEach { entry ->
                        val result = refreshEntry(entry)
                        if (result is Result.Failure) {
                            failed += entry.name
                            _state.update { it.copy(error = "自动更新 ${entry.name} 失败：${result.message}") }
                        } else if (entry.active) {
                            onActiveChanged(entry.path)
                            activeUpdated = true
                        }
                    }
                    if (due.isNotEmpty()) {
                        val success = due.size - failed.size
                        val message = if (failed.isEmpty()) {
                            "订阅自动更新完成：$success 个全部成功"
                        } else {
                            "订阅自动更新：成功 $success 个，失败 ${failed.size} 个 —— ${failed.joinToString("、")}"
                        }
                        LinuxNotifications.notify("Mexpa 订阅自动更新", message)
                        if (activeUpdated) onActiveConfigUpdated?.invoke()
                    }
                }
                delay(60_000)
            }
        }
    }

    // ---------------------------------------------------------------- 下载 / 校验

    private sealed interface Source {
        data class Url(val url: String, val userAgent: String) : Source
        data class Local(val file: File) : Source
    }

    private sealed interface Result {
        data class Success(val displayName: String = "", val traffic: SubscriptionTraffic? = null) : Result
        data class Failure(val message: String) : Result
    }

    private suspend fun downloadAndValidate(
        source: Source,
        target: File,
        ageSecretKey: String,
        temporaryRename: Boolean = false,
    ): Result = withContext(Dispatchers.IO) {
        // 三阶段：pending 沙箱写入并校验 → 提交时整目录换入；失败只清 pending，旧配置不动。
        val id = target.parentFile.name
        val pendingDir = File(baseDir, "subscriptions/.pending/$id").apply { mkdirs() }
        val pending = File(pendingDir, "config.yaml")
        val oldDir = File(baseDir, "subscriptions/.old/$id")
        var traffic: SubscriptionTraffic? = null
        try {
            val displayName = when (source) {
                is Source.Local -> {
                    pending.writeBytes(source.file.readBytes())
                    ""
                }

                is Source.Url -> {
                    _state.update { it.copy(importProgress = ImportProgress(ImportStep.Downloading)) }
                    val userAgent = effectiveUserAgent(source.userAgent)
                    val proxiedPort = proxyPortOrNull()
                    val fetched = fetchSubscription(source.url, userAgent, proxiedPort)
                        ?: throw java.io.IOException(
                            "下载失败：直连不可用${if (proxiedPort == null) "（可先启动代理后重试，或检查网络）" else ""}",
                        )
                    pending.writeBytes(fetched.bytes)
                    traffic = fetched.traffic
                    fetched.displayName
                }
            }
            _state.update { it.copy(importProgress = ImportProgress(ImportStep.Prefetching)) }
            prefetchProviders(pending, ageSecretKey)
            _state.update { it.copy(importProgress = ImportProgress(ImportStep.Validating)) }
            val validation = validate(pending, ageSecretKey)
            if (validation is Result.Failure) return@withContext validation

            // commit：旧目录先挪到 .old，pending 换入；换入失败回滚
            val targetDir = target.parentFile
            targetDir.parentFile?.mkdirs()
            oldDir.parentFile?.mkdirs()
            oldDir.deleteRecursively()
            if (targetDir.exists() && !targetDir.renameTo(oldDir)) {
                return@withContext Result.Failure("提交失败：无法移走旧配置 ${targetDir.path}")
            }
            if (!pendingDir.renameTo(targetDir)) {
                // 跨设备/权限异常时退回复制，回滚旧目录
                val copied = runCatching {
                    targetDir.mkdirs()
                    pending.copyTo(File(targetDir, "config.yaml"), overwrite = true)
                }.isSuccess
                pendingDir.deleteRecursively()
                if (!copied) {
                    if (oldDir.exists()) oldDir.renameTo(targetDir)
                    return@withContext Result.Failure("提交失败：无法写入 ${targetDir.path}")
                }
            }
            oldDir.deleteRecursively()
            pendingDir.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
            Result.Success(displayName, traffic)
        } catch (cancelled: CancellationException) {
            pendingDir.deleteRecursively()
            throw cancelled
        } catch (t: Throwable) {
            pendingDir.deleteRecursively()
            errorResult(t)
        }
    }

    /** provider prefetch：需要 mishka build tag；失败不影响导入（首次启动会再取）。 */
    private fun prefetchProviders(candidate: File, ageSecretKey: String) {
        val text = runCatching { candidate.readText().take(200_000) }.getOrDefault("")
        if ("proxy-providers" !in text) return
        val binary = File(binaryPath())
        if (!binary.isFile) return
        val home = File(baseDir, "validate").apply { mkdirs() }
        seedGeoData(home)
        val command = buildList {
            add(binary.path)
            addAll(listOf("-prefetch", "-f", candidate.path, "-d", home.path))
            if (ageSecretKey.isNotBlank()) addAll(listOf("-age-secret-key", ageSecretKey))
        }
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        proxyPortOrNull()?.let { port ->
            val proxyUrl = "http://127.0.0.1:$port"
            builder.environment()["HTTP_PROXY"] = proxyUrl
            builder.environment()["HTTPS_PROXY"] = proxyUrl
            builder.environment()["ALL_PROXY"] = proxyUrl
        }
        runCatching {
            val process = builder.start()
            process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(90, TimeUnit.SECONDS)) process.destroyForcibly()
        }
    }

    private data class SubscriptionTraffic(
        val upload: Long,
        val download: Long,
        val total: Long,
        val expire: Long,
    )

    /** 解析 Clash 订阅响应头：`upload=1; download=2; total=3; expire=1700000000`。 */
    private fun parseSubscriptionUserInfo(raw: String): SubscriptionTraffic? {
        if (raw.isBlank()) return null
        val values = raw.split(';').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx).trim().lowercase() to
                part.substring(idx + 1).trim().toDoubleOrNull()
        }.toMap()
        val upload = values["upload"]?.toLong() ?: 0
        val download = values["download"]?.toLong() ?: 0
        val total = values["total"]?.toLong() ?: 0
        val expire = values["expire"]?.toLong() ?: 0
        if (upload == 0L && download == 0L && total == 0L && expire == 0L) return null
        return SubscriptionTraffic(upload, download, total, expire)
    }

    private data class FetchedSubscription(val bytes: ByteArray, val displayName: String, val traffic: SubscriptionTraffic?)

    /**
     * 先按配置的代理（代理运行中且开关打开）下载，失败再直连；两者都失败时返回 null。
     * 部分机场按 UA 分流：默认 UA 对齐 Android 客户端，否则会得到 404/空 body/base64。
     */
    private suspend fun fetchSubscription(url: String, userAgent: String, proxyPort: Int?): FetchedSubscription? {
        suspend fun attempt(port: Int?): FetchedSubscription {
            val response = clientFor(port).get(url) { header("User-Agent", userAgent) }
            val status = response.status.value
            if (status !in 200..299) error("HTTP $status")
            val bytes = response.bodyAsBytes()
            if (bytes.isEmpty()) error("订阅内容为空")
            checkLooksLikeClashConfig(bytes, status)
            val displayName = nameFromContentDisposition(response.headers["Content-Disposition"].orEmpty())
                .ifBlank { URI.create(url).host?.removePrefix("www.").orEmpty() }
            val traffic = parseSubscriptionUserInfo(response.headers["Subscription-Userinfo"].orEmpty())
            return FetchedSubscription(bytes, displayName, traffic)
        }
        val order = if (proxyPort != null) listOf(proxyPort, null) else listOf<Int?>(null, null)
        for (port in order.distinct()) {
            runCatching { attempt(port) }.onSuccess { return it }
        }
        return null
    }

    /** 机场在 UA 不匹配时可能返回 HTML 登录页或 base64（V2Ray 订阅），提前给出可读错误。 */
    private fun checkLooksLikeClashConfig(bytes: ByteArray, status: Int) {
        val text = bytes.toString(Charsets.UTF_8).trimStart('﻿').trimStart()
        if (text.startsWith("<")) {
            error("返回的是 HTML 页面（HTTP $status，可能是登录页/需要代理或 User-Agent）")
        }
        if (text.startsWith("eyJ") || Regex("^[A-Za-z0-9+/=\r\n]{200,}$").matches(text)) {
            error("内容不是 Clash 配置（疑似 base64/V2Ray 订阅）")
        }
    }

    private suspend fun validate(candidate: File, ageSecretKey: String): Result = withContext(Dispatchers.IO) {
        val binary = File(binaryPath())
        if (!binary.isFile) return@withContext Result.Failure("找不到 mihomo 可执行文件：${binary.path}")
        val home = File(baseDir, "validate").apply { mkdirs() }
        seedGeoData(home)
        val command = buildList {
            add(binary.path)
            addAll(listOf("-t", "-f", candidate.path, "-d", home.path))
            if (ageSecretKey.isNotBlank()) addAll(listOf("-age-secret-key", ageSecretKey))
        }
        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)
        // mihomo -t 会按需下载 GeoIP/GeoSite：代理运行时让校验也走代理，避免直连 GitHub 失败被误判为坏配置
        proxyPortOrNull()?.let { port ->
            val proxyUrl = "http://127.0.0.1:$port"
            processBuilder.environment()["HTTP_PROXY"] = proxyUrl
            processBuilder.environment()["HTTPS_PROXY"] = proxyUrl
            processBuilder.environment()["ALL_PROXY"] = proxyUrl
        }
        val process = runCatching { processBuilder.start() }.getOrElse {
            return@withContext Result.Failure("无法执行 mihomo：${it.message}")
        }
        val output = runCatching { process.inputStream.bufferedReader().use { it.readText() } }.getOrDefault("")
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return@withContext Result.Failure("mihomo -t 超时")
        }
        return@withContext if (process.exitValue() == 0) {
            Result.Success()
        } else {
            Result.Failure(
                output.lineSequence().filter { it.isNotBlank() }.toList().takeLast(2).joinToString(" | ")
                    .ifBlank { "配置校验失败" },
            )
        }
    }

    private fun nameFromContentDisposition(header: String): String {
        if (header.isBlank()) return ""
        val encoded = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(header)?.groupValues?.get(1)
        if (encoded != null) {
            return runCatching { java.net.URLDecoder.decode(encoded, Charsets.UTF_8) }.getOrDefault(encoded)
                .removeSuffix(".yaml").removeSuffix(".yml")
        }
        val plain = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(header)?.groupValues?.get(1)
        return plain.orEmpty().removeSuffix(".yaml").removeSuffix(".yml")
    }

    /** 把内置/已下载的 geodata 放进 mihomo 的 `-d` 目录，避免校验时联网下载。 */
    private fun seedGeoData(home: File) {
        val source = geoDataDirProvider() ?: return
        listOf("geoip.metadb", "geosite.dat", "ASN.mmdb").forEach { name ->
            val input = File(source, name)
            val target = File(home, name)
            if (!input.isFile || input.length() == 0L || target.isFile) return@forEach
            runCatching { input.copyTo(target, overwrite = true) }
        }
    }

    private fun errorResult(error: Throwable): Result.Failure =
        Result.Failure(error.message ?: error::class.simpleName.orEmpty())

    private fun LocalSubscription.toUiModel(): Subscription = Subscription(
        id = id,
        name = name,
        type = if (isUrl) ProfileType.Url else ProfileType.File,
        url = url,
        userAgent = userAgent,
        ageSecretKey = ageSecretKey,
        interval = intervalMinutes * 60_000,
        upload = upload,
        download = download,
        total = total,
        expire = expire,
        updatedAt = updatedAt,
        isActive = active,
        imported = true,
        pending = false,
    )
}
