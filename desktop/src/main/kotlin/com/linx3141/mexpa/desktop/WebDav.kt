package com.linx3141.mexpa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.isSuccess
import java.net.URI

/**
 * 最小 WebDAV 客户端（PUT/GET/MKCOL）。
 *
 * 与 Android 一致：不带尾斜杠的集合 URL 服务器常 301 到带斜杠版本，而 Ktor 默认只对
 * GET/HEAD 跟随重定向；PUT/MKCOL 必须自己跟随，且只允许同主机（Basic 凭据不跨主机转发）。
 */
class WebDavClient(
    private val baseUrl: String,
    private val username: String = "",
    private val password: String = "",
) {
    data class Result(val ok: Boolean, val message: String = "", val bytes: ByteArray? = null) {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    private fun collectionUrl(): String = baseUrl.trim().trimEnd('/') + "/"

    private fun fileUrl(name: String): String = collectionUrl() + name.trimStart('/')

    suspend fun test(): Result {
        val mkcol = mkcol()
        if (!mkcol.ok) return mkcol
        return put(".mexpa-test", "ok".toByteArray())
    }

    suspend fun mkcol(): Result = runCatching {
        val response = execute(HttpMethod("MKCOL"), collectionUrl(), null)
        when {
            response.status.value in 200..299 -> Result(true, "集合可用")
            response.status.value == 405 -> Result(true, "集合已存在")
            else -> Result(false, "MKCOL ${response.status.value}")
        }
    }.getOrElse { Result(false, it.message ?: it::class.simpleName.orEmpty()) }

    suspend fun put(name: String, bytes: ByteArray): Result = runCatching {
        val response = execute(HttpMethod("PUT"), fileUrl(name), bytes)
        if (response.status.isSuccess()) Result(true, "已上传 ${bytes.size} B") else Result(false, "PUT ${response.status.value}")
    }.getOrElse { Result(false, it.message ?: it::class.simpleName.orEmpty()) }

    suspend fun get(name: String): Result = runCatching {
        val response = execute(HttpMethod.Get, fileUrl(name), null)
        if (response.status.isSuccess()) {
            Result(true, "已下载", response.bodyAsBytes())
        } else {
            Result(false, "GET ${response.status.value}")
        }
    }.getOrElse { Result(false, it.message ?: it::class.simpleName.orEmpty()) }

    private suspend fun execute(method: HttpMethod, url: String, body: ByteArray?): HttpResponse {
        var current = URI(url)
        repeat(6) { attempt ->
            val response = client.request(current.toString()) {
                this.method = method
                if (username.isNotBlank()) basicAuth(username, password)
                if (body != null) setBody(body)
                header("User-Agent", SubscriptionRepository.DEFAULT_USER_AGENT)
            }
            if (response.status.value in listOf(301, 302, 307, 308)) {
                val location = response.headers["Location"] ?: return response
                val next = current.resolve(location)
                if (!next.host.equals(current.host, ignoreCase = true)) return response
                current = next
                return@repeat
            }
            return response
        }
        error("too many redirects")
    }
}
