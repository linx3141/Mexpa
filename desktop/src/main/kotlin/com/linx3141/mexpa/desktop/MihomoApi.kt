package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConnectionsResponse
import com.linx3141.mexpa.domain.model.DnsQueryResponse
import com.linx3141.mexpa.domain.model.MihomoConfig
import com.linx3141.mexpa.domain.model.MihomoVersion
import com.linx3141.mexpa.domain.model.ProxyNode
import com.linx3141.mexpa.domain.model.ProvidersResponse
import com.linx3141.mexpa.domain.model.RuleProvidersResponse
import com.linx3141.mexpa.viewmodel.ProxyGroupUi
import com.linx3141.mexpa.viewmodel.ProxyUiState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import java.io.File
import java.net.URLEncoder

/**
 * mihomo external-controller 客户端（Linux 用整机 mihomo 子进程，不是 Android 的 embed mode，
 * 所以 PATCH /configs、/restart 这些接口都可用）。
 *
 * 数据模型直接复用 shared-ui 的 `@Serializable` domain model，避免桌面端再维护一套解析结构。
 */
class MihomoApi(
    private val endpoint: String,
    private val secret: String,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val baseUrl = "http://$endpoint"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        // mihomo 的 /connections 在空列表时返回 "connections": null，需要用默认值兜底
        coerceInputValues = true
    }
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 6_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 15_000
        }
    }

    suspend fun version(): MihomoVersion? = getTyped("/version", MihomoVersion.serializer())
    suspend fun configs(): MihomoConfig? = getTyped("/configs", MihomoConfig.serializer())
    suspend fun connections(): ConnectionsResponse? = getTyped("/connections", ConnectionsResponse.serializer())
    suspend fun providers(): ProvidersResponse? = getTyped("/providers/proxies", ProvidersResponse.serializer())
    suspend fun ruleProviders(): RuleProvidersResponse? = getTyped("/providers/rules", RuleProvidersResponse.serializer())

    suspend fun proxies(): Map<String, ProxyNode>? = runCatching {
        val root = json.parseToJsonElement(getText("/proxies").orEmpty()) as? JsonObject ?: return null
        val proxies = root["proxies"] as? JsonObject ?: return null
        proxies.mapValues { (_, element) -> json.decodeFromJsonElement(ProxyNode.serializer(), element) }
    }.getOrNull()

    suspend fun selectProxy(group: String, name: String) = putJson(
        "/proxies/${encode(group)}",
        """{"name":"${escape(name)}"}""",
    )

    suspend fun testNodeDelay(
        name: String,
        timeout: Int = 5000,
        url: String = "http://www.gstatic.com/generate_204",
    ): Int? = runCatching {
        val text = getText("/proxies/${encode(name)}/delay?timeout=$timeout&url=${encode(url)}") ?: return null
        val element = json.parseToJsonElement(text) as? JsonObject ?: return null
        (element["delay"] as? JsonPrimitive)?.int
    }.getOrNull()

    suspend fun updateProvider(name: String, isRuleProvider: Boolean) {
        val path = if (isRuleProvider) "/providers/rules/${encode(name)}" else "/providers/proxies/${encode(name)}"
        client.put(baseUrl + path) { authorize() }
    }

    suspend fun testProviderNodeDelay(provider: String, node: String): Int? = runCatching {
        val response = client.get(
            baseUrl + "/providers/proxies/${encode(provider)}/${encode(node)}/healthcheck",
        ) { authorize() }
        if (response.status.value !in 200..299) return null
        val element = json.parseToJsonElement(response.bodyAsText()) as? JsonObject ?: return null
        (element["delay"] as? JsonPrimitive)?.int
    }.getOrNull()

    suspend fun healthcheckProvider(name: String) {
        client.get(baseUrl + "/providers/proxies/${encode(name)}/healthcheck") { authorize() }
    }

    suspend fun closeConnection(id: String) {
        client.delete(baseUrl + "/connections/${encode(id)}") { authorize() }
    }

    suspend fun closeAllConnections() {
        client.delete(baseUrl + "/connections") { authorize() }
    }

    suspend fun dnsQuery(name: String, type: String): DnsQueryResponse? = runCatching {
        val text = getText("/dns/query?name=${encode(name)}&type=${encode(type)}") ?: return null
        json.decodeFromString(DnsQueryResponse.serializer(), text)
    }.getOrNull()

    suspend fun flushCache() {
        client.post(baseUrl + "/cache/flush") { authorize() }
    }

    suspend fun flushFakeIp() {
        client.post(baseUrl + "/cache/fakeip/flush") { authorize() }
    }

    suspend fun memoryInUse(): Long? = runCatching {
        val text = getText("/memory") ?: return null
        val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
        (obj["inuse"] as? JsonPrimitive)?.long
    }.getOrNull()

    suspend fun patchMode(mode: String) {
        patchJson("/configs", """{"mode":"${escape(mode)}"}""")
    }

    /** 整机 mihomo 的 restart：让核心重新读 config.yaml。 */
    suspend fun restart() {
        client.post(baseUrl + "/restart") { authorize() }
    }

    /**
     * 长连接：断线自动重连（1s→10s 退避）。回调文本保持原始 JSON，由调用方按需解析。
     */
    fun stream(path: String, onText: suspend (String) -> Unit): Job = scope.launch {
        var backoff = 1_000L
        while (isActive) {
            try {
                client.webSocket(urlString = wsUrl(path)) {
                    backoff = 1_000L
                    for (frame in incoming) {
                        if (frame is Frame.Text) onText(frame.readText())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // 连接失败/断开：退避后重连
            }
            if (!isActive) break
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(10_000L)
        }
    }

    private suspend fun getText(path: String): String? = runCatching {
        val response = client.get(baseUrl + path) { authorize() }
        if (response.status.value !in 200..299) return null
        response.bodyAsText()
    }.getOrNull()

    private suspend fun <T> getTyped(path: String, serializer: KSerializer<T>): T? = runCatching {
        val text = getText(path) ?: return null
        json.decodeFromString(serializer, text)
    }.getOrNull()

    private suspend fun putJson(path: String, body: String) {
        client.put(baseUrl + path) {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun patchJson(path: String, body: String) {
        client.patch(baseUrl + path) {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorize() {
        if (secret.isNotEmpty()) header("Authorization", "Bearer $secret")
    }

    private fun wsUrl(path: String): String {
        val separator = if (path.contains('?')) '&' else '?'
        val token = if (secret.isEmpty()) "" else "${separator}token=${encode(secret)}"
        return "ws://$endpoint$path$token"
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    // 路径段不能用 URLEncoder 的 "+" 表示空格；mihomo 的路由按字面 "+" 处理，
    // 含空格的组名（如 "🚀 节点选择"）会 404，选择请求实际没有送达。
    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    override fun close() {
        client.close()
    }

    companion object {
        /** 从 config.yaml 读出 external-controller / secret；读不到时用控制器默认值。 */
        fun endpointFromConfig(configFile: File): Pair<String, String> {
            val text = runCatching { configFile.readText() }.getOrDefault("")
            val controller = Regex("(?m)^\\s*external-controller\\s*:\\s*['\"]?([^'\"\\s]+)")
                .find(text)?.groupValues?.get(1)
            val secret = Regex("(?m)^\\s*secret\\s*:\\s*['\"]?([^'\"\\s]+)")
                .find(text)?.groupValues?.get(1)
            return (controller?.takeIf { it.isNotBlank() } ?: "127.0.0.1:9090") to secret.orEmpty()
        }

        /** 把 /proxies 映射成 Proxy 页 UI 状态（组顺序与 Android 对齐：GLOBAL 在非 global 模式沉底）。 */
        fun buildProxyState(
            proxies: Map<String, ProxyNode>,
            mode: String,
            previous: ProxyUiState = ProxyUiState(),
        ): ProxyUiState {
            val nodeTypes = proxies.mapValues { it.value.type }.toPersistentMap()
            val groups = proxies.values
                .filter { it.all.isNotEmpty() && !it.hidden }
                .sortedBy { group ->
                    if (group.name.equals("GLOBAL", ignoreCase = true) && mode != "global") 1 else 0
                }
                .map { group ->
                    val delays = group.all.mapNotNull { name ->
                        val delay = proxies[name]?.history?.lastOrNull()?.delay?.takeIf { it > 0 }
                        if (delay != null) name to delay else null
                    }.toMap().toPersistentMap()
                    ProxyGroupUi(
                        name = group.name,
                        type = group.type,
                        now = group.now,
                        all = group.all.toPersistentList(),
                        delays = delays,
                        nodeTypes = nodeTypes,
                        icon = group.icon,
                        fixed = group.fixed,
                        testUrl = group.testUrl,
                    )
                }
                .toPersistentList()
            return previous.copy(groups = groups, mode = mode)
        }
    }
}
