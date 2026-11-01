package com.linx3141.mexpa.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.linx3141.mexpa.ui.screen.proxy.ProxyIconLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** 代理组图标加载：内存缓存 + 失败负缓存；主流程只从配置里的 http(s) 图标地址取。 */
object DesktopProxyIconLoader {
    private val client = HttpClient(OkHttp) {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
        }
    }
    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val failures = ConcurrentHashMap<String, Long>()

    fun loader(): ProxyIconLoader = ProxyIconLoader(version = 0, load = { url -> load(url) })

    suspend fun load(url: String): ImageBitmap? {
        if (url.isBlank()) return null
        cache[url]?.let { return it }
        val failedAt = failures[url]
        if (failedAt != null && System.currentTimeMillis() - failedAt < 10_000) return null
        return mutex.withLock {
            cache[url] ?: runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = client.get(url).bodyAsBytes()
                    if (bytes.isEmpty()) error("empty icon")
                    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                }
            }.onSuccess {
                cache[url] = it
                failures.remove(url)
            }.onFailure {
                failures[url] = System.currentTimeMillis()
            }.getOrNull()
        }
    }

    fun clearFailures() = failures.clear()
}
