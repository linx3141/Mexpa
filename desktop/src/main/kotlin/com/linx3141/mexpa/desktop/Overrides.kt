package com.linx3141.mexpa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class OverrideEntry(
    val id: String,
    val name: String,
    /** yaml | js */
    val format: String,
    /** local | remote */
    val sourceType: String = "local",
    val url: String = "",
    val updatedAt: Long = 0,
)

/**
 * 覆写配置存储：`~/.config/mexpa/overrides/{id}.{yaml|js}` + `overrides.json` 索引。
 *
 * 语义与 Android 的 mishka_core/overrides 一致（YAML 深度合并、JS `function main(c)`），
 * 应用由 `mexpa-transform`（同一份 Go 实现）在启动前合成运行时配置。
 */
class OverridesStore(
    private val dir: File,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit = {},
    private val proxyPortProvider: () -> Int? = { null },
) {
    @Serializable
    private data class Index(val profiles: List<OverrideEntry> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val indexFile = File(dir, "overrides.json")
    private val _profiles = MutableStateFlow<List<OverrideEntry>>(emptyList())
    val profiles: StateFlow<List<OverrideEntry>> = _profiles.asStateFlow()

    private val directClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    private fun clientFor(proxyPort: Int?): HttpClient {
        if (proxyPort == null) return directClient
        return HttpClient(OkHttp) {
            engine {
                config {
                    proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", proxyPort)))
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    fun load() {
        dir.mkdirs()
        _profiles.value = runCatching {
            json.decodeFromString(Index.serializer(), indexFile.readText()).profiles
        }.getOrDefault(emptyList())
    }

    private fun save(profiles: List<OverrideEntry>) {
        dir.mkdirs()
        indexFile.writeText(json.encodeToString(Index.serializer(), Index(profiles)))
        _profiles.value = profiles
        onChanged()
    }

    fun contentFile(entry: OverrideEntry): File = File(dir, "${entry.id}.${entry.format}")

    fun readContent(id: String): String {
        val entry = _profiles.value.firstOrNull { it.id == id } ?: return ""
        return runCatching { contentFile(entry).readText() }.getOrDefault("")
    }

    fun addBlank(name: String, format: String): OverrideEntry {
        val entry = OverrideEntry(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "覆写" },
            format = format,
            updatedAt = System.currentTimeMillis(),
        )
        contentFile(entry).writeText("")
        save(_profiles.value + entry)
        return entry
    }

    fun importFile(file: File, name: String = file.name): OverrideEntry? {
        val format = when (file.extension.lowercase()) {
            "js" -> "js"
            "yaml", "yml" -> "yaml"
            else -> return null
        }
        val entry = addBlank(name.substringBeforeLast('.'), format)
        runCatching { contentFile(entry).writeBytes(file.readBytes()) }
        save(_profiles.value)
        return entry
    }

    fun addRemote(name: String, url: String, format: String): OverrideEntry? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val entry = OverrideEntry(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "远程覆写" },
            format = format,
            sourceType = "remote",
            url = url,
            updatedAt = 0,
        )
        save(_profiles.value + entry)
        updateRemote(entry.id) {}
        return entry
    }

    fun updateRemote(id: String, onComplete: (String?) -> Unit = {}) {
        val entry = _profiles.value.firstOrNull { it.id == id } ?: return
        if (!entry.isRemote) return
        scope.launch {
            val error = runCatching {
                val port = proxyPortProvider()
                val text = withContext(Dispatchers.IO) {
                    val client = clientFor(port)
                    try {
                        client.get(entry.url).bodyAsText()
                    } finally {
                        client.close()
                    }
                }
                if (text.isBlank()) error("远程覆写内容为空")
                contentFile(entry).writeText(text)
                null
            }.getOrElse { it.message ?: it::class.simpleName.orEmpty() }
            if (error == null) {
                save(_profiles.value.map { if (it.id == id) it.copy(updatedAt = System.currentTimeMillis()) else it })
            }
            onComplete(error)
        }
    }

    fun writeContent(id: String, text: String) {
        val entry = _profiles.value.firstOrNull { it.id == id } ?: return
        contentFile(entry).writeText(text)
        save(_profiles.value.map { if (it.id == id) it.copy(updatedAt = System.currentTimeMillis()) else it })
    }

    fun delete(id: String) {
        val entry = _profiles.value.firstOrNull { it.id == id } ?: return
        runCatching { contentFile(entry).delete() }
        save(_profiles.value.filterNot { it.id == id })
    }

    val OverrideEntry.isRemote: Boolean get() = sourceType == "remote"
}

/** 启动前把 active 订阅的覆写套用到 config.yaml；结果为运行时配置文件。 */
object OverrideRuntime {
    data class Result(val file: File?, val error: String? = null)

    fun apply(
        base: File,
        overrideIds: List<String>,
        store: OverridesStore,
        helper: File?,
        output: File,
    ): Result {
        if (overrideIds.isEmpty()) return Result(null)
        if (helper == null || !helper.canExecute()) return Result(null, "找不到 mexa-transform，覆写未生效")
        val profiles = store.profiles.value.filter { it.id in overrideIds }
        if (profiles.isEmpty()) return Result(null)
        val specsFile = File(output.parentFile, "transform-specs.json")
        specsFile.parentFile?.mkdirs()
        val specs = profiles.joinToString(prefix = "{\"overrides\":[", postfix = "]}") { entry ->
            val path = store.contentFile(entry).path.replace("\\", "\\\\").replace("\"", "\\\"")
            "{\"name\":\"${entry.name.replace("\"", "\\\"")}\",\"format\":\"${entry.format}\",\"path\":\"$path\"}"
        }
        specsFile.writeText(specs)
        val process = runCatching {
            ProcessBuilder(
                helper.path, "-base", base.path, "-out", output.path, "-specs", specsFile.path,
            ).redirectErrorStream(true).start()
        }.getOrElse { return Result(null, "无法执行 mexa-transform：${it.message}") }
        val log = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return Result(null, "覆写变换超时")
        }
        if (process.exitValue() != 0) {
            return Result(null, log.lineSequence().lastOrNull { it.isNotBlank() } ?: "覆写变换失败")
        }
        return Result(output)
    }

    fun resolveHelper(mihomoBinaryPath: String?): File? {
        System.getProperty("mexpa.transform")?.takeIf { it.isNotBlank() }?.let { return File(it) }
        System.getenv("MEXPA_TRANSFORM_BIN")?.takeIf { it.isNotBlank() }?.let { return File(it) }
        mihomoBinaryPath?.let { path ->
            File(File(path).absoluteFile.parentFile, DesktopPlatform.transformBinaryName).takeIf { it.canExecute() }?.let { return it }
        }
        val cwd = File(System.getProperty("user.dir"))
        listOf(cwd, cwd.parentFile, File(cwd, "desktop"), File(cwd.parentFile ?: cwd, "desktop"))
            .filterNotNull().forEach { dir ->
                File(dir, DesktopPlatform.transformBinaryName).takeIf { it.canExecute() }?.let { return it }
                File(dir, "desktop/build/windows-bin/${DesktopPlatform.transformBinaryName}")
                    .takeIf { it.canExecute() }?.let { return it }
            }
        return null
    }
}
