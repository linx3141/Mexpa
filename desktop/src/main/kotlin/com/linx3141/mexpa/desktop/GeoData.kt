package com.linx3141.mexpa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GeoDataState(
    val downloading: Boolean = false,
    val message: String = "",
    val error: String = "",
)

/**
 * GeoIP/GeoSite/ASN 数据管理（对齐 Android 构建期预置的 `geoip.metadb`/`geosite.dat`/`ASN.mmdb`）。
 *
 * 文件下载到 mihomo 的 `-d` home 目录，核心重启后生效。
 * 走直连下载：桌面端启动前 mixed-port 往往还没起来；下载失败时 UI 会显示具体错误。
 */
class GeoDataManager(
    private val workDir: File,
    private val scope: CoroutineScope,
    private val sources: List<Pair<String, String>> = REMOTE_SOURCES,
) {
    private val geoIp = File(workDir, "geoip.metadb")
    private val geoSite = File(workDir, "geosite.dat")
    private val geoAsn = File(workDir, "ASN.mmdb")

    private val _state = MutableStateFlow(GeoDataState(message = statusText()))
    val state: StateFlow<GeoDataState> = _state.asStateFlow()

    init {
        ensureBundled()
    }

    /**
     * 桌面包内置 Android 构建期的 GeoIP 数据。没有它时 `mihomo -t` 会去 GitHub 下载
     * geodata，国内网络/离线环境会卡到超时，订阅导入直接失败；启动时先落到 workDir。
     */
    private fun ensureBundled() {
        workDir.mkdirs()
        listOf(geoIp, geoSite, geoAsn).forEach { target ->
            if (target.isFile && target.length() > 0) return@forEach
            runCatching {
                val input = javaClass.classLoader?.getResourceAsStream("geo/${target.name}") ?: return@runCatching
                val temp = File(target.parentFile, "${target.name}.part")
                input.use { src -> temp.outputStream().use { dst -> src.copyTo(dst) } }
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    target.writeBytes(temp.readBytes())
                    temp.delete()
                }
            }
        }
        _state.value = GeoDataState(message = statusText())
    }

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
    }
    private var job: Job? = null

    fun statusText(): String {
        val files = listOf(geoIp, geoSite, geoAsn)
        val present = files.count { it.isFile && it.length() > 0 }
        return "$present/3 个文件" + files.filterNot { it.isFile }.joinToString("") { "；缺 ${it.name}" }
    }

    fun download() {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = GeoDataState(downloading = true, message = "正在下载 GeoIP 数据…")
            try {
                workDir.mkdirs()
                val failures = mutableListOf<String>()
                sources.forEach { (url, name) ->
                    val target = File(workDir, name)
                    val result = runCatching { downloadTo(url, target) }
                    if (result.isFailure) {
                        failures += "$name: ${result.exceptionOrNull()?.message ?: "下载失败"}"
                        target.delete()
                    }
                }
                if (failures.isEmpty()) {
                    _state.value = GeoDataState(message = "GeoIP 数据已更新：${statusText()}")
                } else {
                    _state.value = GeoDataState(error = failures.joinToString("；"), message = statusText())
                }
            } finally {
                _state.update { it.copy(downloading = false) }
                job = null
            }
        }
    }

    private suspend fun downloadTo(url: String, target: File) = withContext(Dispatchers.IO) {
        val response = client.get(url)
        if (response.status.value !in 200..299) error("HTTP ${response.status.value}")
        val bytes = response.bodyAsBytes()
        if (bytes.size < 512 * 1024) error("文件仅 ${bytes.size} 字节，疑似 404/限流页面")
        val temp = File(target.parentFile, "${target.name}.part")
        temp.writeBytes(bytes)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            target.writeBytes(temp.readBytes())
            temp.delete()
        }
    }

    private companion object {
        val REMOTE_SOURCES = listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb" to "geoip.metadb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat" to "geosite.dat",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb" to "ASN.mmdb",
        )
    }
}
