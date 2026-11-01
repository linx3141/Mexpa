package com.linx3141.mexpa.desktop

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

enum class SystemProxyBackend(val label: String) {
    Kde("KDE (kwriteconfig)"),
    Gnome("GNOME (gsettings)"),
    Mate("MATE (gsettings)"),
}

private const val KDE_CONFIG_GROUP = "Proxy Settings"
private val KDE_KEYS = listOf("ProxyType", "httpProxy", "httpsProxy", "socksProxy", "NoProxyFor", "Proxy Config Script")

private val GNOME_KEYS = listOf(
    "org.gnome.system.proxy" to "mode",
    "org.gnome.system.proxy" to "ignore-hosts",
    "org.gnome.system.proxy.http" to "host",
    "org.gnome.system.proxy.http" to "port",
    "org.gnome.system.proxy.https" to "host",
    "org.gnome.system.proxy.https" to "port",
    "org.gnome.system.proxy.socks" to "host",
    "org.gnome.system.proxy.socks" to "port",
    "org.gnome.system.proxy" to "autoconfig-url",
)

private val MATE_KEYS = GNOME_KEYS.map { (schema, key) ->
    "org.mate.system.proxy" + schema.removePrefix("org.gnome.system.proxy") to key
}

/** 跟随 FlClash：KDE 写 kioslaverc + DBus reparse，GNOME/MATE 走 gsettings。 */
private val DEFAULT_BYPASS = listOf(
    "localhost",
    "127.*",
    "10.*",
    "172.16.*",
    "172.17.*",
    "172.18.*",
    "172.19.*",
    "172.2*",
    "172.30.*",
    "172.31.*",
    "192.168.*",
    "*.local",
    "*.lan",
)

/**
 * Linux 桌面没有统一的系统代理 API，按 FlClash 的做法分后端实现。
 *
 * 与 FlClash 的差异：应用前先把上一份设置快照写盘（[snapshotFile]），停止时恢复原样。
 * 用户开机时往往已经用别的客户端（比如 FlClash）配好了代理，直接 `mode none` 会把它抹掉；
 * 崩溃/被 kill 时下次启动也会先按快照恢复，避免系统代理停在已退出的端口上。
 */
class LinuxSystemProxy(
    private val configDir: File,
    private val snapshotFile: File,
    private val env: (String) -> String? = System::getenv,
) {
    private val kdeConfigFile = File(configDir, "kioslaverc")

    @Volatile
    private var applied = false

    fun detectBackend(): SystemProxyBackend? {
        val desktops = env("XDG_CURRENT_DESKTOP")
            ?.split(":")
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (desktops.contains("KDE")) return SystemProxyBackend.Kde.takeIf { kdeWriter() != null && kdeReader() != null }
        if (desktops.contains("MATE")) return SystemProxyBackend.Mate.takeIf { gnomeAvailable("org.mate.system.proxy") }
        if (desktops.any { it in setOf("GNOME", "CINNAMON", "BUDGIE", "UNITY") }) {
            return SystemProxyBackend.Gnome.takeIf { gnomeAvailable("org.gnome.system.proxy") }
        }
        // 未知桌面：按 FlClash 的回退顺序探测可用的后端
        return SystemProxyBackend.Gnome.takeIf { gnomeAvailable("org.gnome.system.proxy") }
            ?: SystemProxyBackend.Kde.takeIf { kdeWriter() != null && kdeReader() != null }
    }

    fun backendLabel(): String = detectBackend()?.label ?: "未检测到可用的系统代理后端"

    /** 上次进程被 kill 时可能把系统代理留在本客户端端口上，启动时先恢复。 */
    @Synchronized
    fun recoverIfStale(): String? {
        if (!snapshotFile.isFile) return null
        val backend = readSnapshotBackend(snapshotFile) ?: return null.also { snapshotFile.delete() }
        val message = restoreFrom(backend, readSnapshot(snapshotFile))
        applied = false
        return "已恢复上次异常退出前的系统代理设置（${backend.label}）：${message.message}"
    }

    @Synchronized
    fun apply(port: Int, pacUrl: String? = null): ProxyResult {
        val backend = detectBackend()
            ?: return ProxyResult(false, "未检测到可用的系统代理后端（支持 KDE / GNOME / MATE）")
        if (applied) return ProxyResult(true, "系统代理已是 127.0.0.1:$port")
        if (snapshotFile.isFile) {
            // 上一次没正常恢复：先恢复，避免把别人的代理设置当成自己的基线
            readSnapshotBackend(snapshotFile)?.let { restoreFrom(it, readSnapshot(snapshotFile)) }
            snapshotFile.delete()
        }
        val snapshot = readCurrent(backend)
        if (!snapshot.ok) return ProxyResult(false, snapshot.message)
        writeSnapshot(backend, snapshot.values)
        val result = applyTo(backend, port, snapshot.values, pacUrl)
        if (!result.ok) {
            restoreFrom(backend, snapshot.values)
            snapshotFile.delete()
            return result
        }
        applied = true
        return ProxyResult(true, "系统代理已指向 127.0.0.1:$port（${backend.label}）")
    }

    @Synchronized
    fun restore(): ProxyResult {
        if (!snapshotFile.isFile) {
            applied = false
            return ProxyResult(true, "系统代理未由 Mexpa 修改")
        }
        val backend = readSnapshotBackend(snapshotFile)
        if (backend == null) {
            snapshotFile.delete()
            applied = false
            return ProxyResult(false, "系统代理快照损坏，无法恢复")
        }
        val result = restoreFrom(backend, readSnapshot(snapshotFile))
        if (result.ok) snapshotFile.delete()
        applied = false
        return result
    }

    private fun applyTo(
        backend: SystemProxyBackend,
        port: Int,
        previous: Map<String, SnapshotValue>,
        pacUrl: String?,
    ): ProxyResult {
        val bypass = buildBypassList(previous)
        val usePac = !pacUrl.isNullOrBlank()
        return when (backend) {
            SystemProxyBackend.Kde -> {
                val writer = kdeWriter() ?: return ProxyResult(false, "找不到 kwriteconfig5/6")
                val failures = mutableListOf<String>()
                setKde(writer, "NoProxyFor", bypass.joinToString(",")).also { if (!it.ok) failures += it.message }
                for (type in listOf("http", "https")) {
                    setKde(writer, "${type}Proxy", "http://127.0.0.1:$port").also { if (!it.ok) failures += it.message }
                }
                setKde(writer, "socksProxy", "socks://127.0.0.1:$port").also { if (!it.ok) failures += it.message }
                if (usePac) {
                    setKde(writer, "Proxy Config Script", pacUrl!!).also { if (!it.ok) failures += it.message }
                    setKde(writer, "ProxyType", "2").also { if (!it.ok) failures += it.message }
                } else {
                    setKde(writer, "ProxyType", "1").also { if (!it.ok) failures += it.message }
                }
                kdeReparse()
                if (failures.isEmpty()) ProxyResult(true, "KDE 系统代理已启用") else ProxyResult(false, failures.joinToString("; "))
            }

            SystemProxyBackend.Gnome, SystemProxyBackend.Mate -> {
                val prefix = if (backend == SystemProxyBackend.Mate) "org.mate" else "org.gnome"
                val failures = mutableListOf<String>()
                setGsettings("$prefix.system.proxy", "ignore-hosts", formatGVariantList(bypass))
                    .also { if (!it.ok) failures += it.message }
                for (type in listOf("http", "https", "socks")) {
                    setGsettings("$prefix.system.proxy.$type", "host", qt("127.0.0.1"))
                        .also { if (!it.ok) failures += it.message }
                    setGsettings("$prefix.system.proxy.$type", "port", port.toString())
                        .also { if (!it.ok) failures += it.message }
                }
                if (usePac) {
                    setGsettings("$prefix.system.proxy", "autoconfig-url", qt(pacUrl!!))
                        .also { if (!it.ok) failures += it.message }
                    setGsettings("$prefix.system.proxy", "mode", qt("auto"))
                        .also { if (!it.ok) failures += it.message }
                } else {
                    setGsettings("$prefix.system.proxy", "mode", qt("manual"))
                        .also { if (!it.ok) failures += it.message }
                }
                if (failures.isEmpty()) ProxyResult(true, "${backend.label} 已启用") else ProxyResult(false, failures.joinToString("; "))
            }
        }
    }

    private fun restoreFrom(backend: SystemProxyBackend, snapshot: Map<String, SnapshotValue>): ProxyResult {
        val failures = mutableListOf<String>()
        when (backend) {
            SystemProxyBackend.Kde -> {
                val writer = kdeWriter()
                if (writer == null) {
                    failures += "找不到 kwriteconfig5/6"
                } else {
                    KDE_KEYS.forEach { key ->
                        val saved = snapshot["kde.$key"] ?: return@forEach
                        if (saved.present) {
                            setKde(writer, key, saved.value).also { if (!it.ok) failures += it.message }
                        }
                    }
                    // 原本没有 ProxyType（kioslaverc 不存在）时不能只跳过写入：那会把本客户端设的 1 留在文件里
                    if (snapshot["kde.ProxyType"]?.present != true) {
                        setKde(writer, "ProxyType", "0").also { if (!it.ok) failures += it.message }
                    }
                    kdeReparse()
                }
            }

            SystemProxyBackend.Gnome, SystemProxyBackend.Mate -> {
                val keys = if (backend == SystemProxyBackend.Mate) MATE_KEYS else GNOME_KEYS
                keys.forEach { (schema, key) ->
                    val saved = snapshot["gnome.$schema.$key"] ?: return@forEach
                    if (saved.present) {
                        setGsettings(schema, key, saved.value).also { if (!it.ok) failures += it.message }
                    }
                }
            }
        }
        // 快照没记录 mode（例如 schema 读失败）时兜底关掉，避免残留指向本客户端的配置
        val gnomeBackend = backend == SystemProxyBackend.Gnome || backend == SystemProxyBackend.Mate
        if (gnomeBackend && snapshot.keys.none { it.endsWith(".mode") }) {
            val prefix = if (backend == SystemProxyBackend.Mate) "org.mate" else "org.gnome"
            setGsettings("$prefix.system.proxy", "mode", qt("none"))
        }
        return if (failures.isEmpty()) ProxyResult(true, "已恢复原系统代理设置") else ProxyResult(false, failures.joinToString("; "))
    }

    private fun readCurrent(backend: SystemProxyBackend): SnapshotReadResult {
        val values = linkedMapOf<String, SnapshotValue>()
        when (backend) {
            SystemProxyBackend.Kde -> {
                val reader = kdeReader() ?: return SnapshotReadResult(false, "找不到 kreadconfig5/6", emptyMap())
                KDE_KEYS.forEach { key ->
                    val result = run(reader, "--file", kdeConfigFile.path, "--group", KDE_CONFIG_GROUP, "--key", key)
                    values["kde.$key"] = SnapshotValue(present = result.ok && result.output.isNotEmpty(), value = result.output)
                }
            }

            SystemProxyBackend.Gnome, SystemProxyBackend.Mate -> {
                val keys = if (backend == SystemProxyBackend.Mate) MATE_KEYS else GNOME_KEYS
                keys.forEach { (schema, key) ->
                    val result = run("gsettings", "get", schema, key)
                    values["gnome.$schema.$key"] = SnapshotValue(present = result.ok, value = result.output)
                }
            }
        }
        return SnapshotReadResult(true, "", values)
    }

    private fun buildBypassList(previous: Map<String, SnapshotValue>): List<String> {
        val existing = when {
            previous["kde.NoProxyFor"]?.present == true -> previous.getValue("kde.NoProxyFor").value
                .split(",", ";").map { it.trim() }

            else -> {
                val ignore = GNOME_KEYS.filter { it.second == "ignore-hosts" }
                    .mapNotNull { (schema, key) -> previous["gnome.$schema.$key"]?.takeIf { it.present }?.value }
                    .firstOrNull()
                parseGVariantList(ignore.orEmpty())
            }
        }
        return (existing + DEFAULT_BYPASS).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    private fun kdeReparse() {
        run(
            "dbus-send", "--session", "--type=signal", "/KIO/Scheduler",
            "org.kde.KIO.Scheduler.reparseSlaveConfiguration", "string:",
        )
    }

    private fun setKde(writer: String, key: String, value: String): ProxyResult {
        val result = run(writer, "--file", kdeConfigFile.path, "--group", KDE_CONFIG_GROUP, "--key", key, value)
        return ProxyResult(result.ok, if (result.ok) "" else "kwriteconfig $key 失败：${result.output}")
    }

    private fun setGsettings(schema: String, key: String, value: String): ProxyResult {
        val result = run("gsettings", "set", schema, key, value)
        return ProxyResult(result.ok, if (result.ok) "" else "gsettings $schema/$key 失败：${result.output}")
    }

    private fun kdeWriter(): String? = executable("kwriteconfig6") ?: executable("kwriteconfig5")

    private fun kdeReader(): String? = executable("kreadconfig6") ?: executable("kreadconfig5")

    private fun gnomeAvailable(schema: String): Boolean {
        if (executable("gsettings") == null) return false
        val schemas = run("gsettings", "list-schemas")
        return schemas.ok && schema in schemas.output.lineSequence().toSet()
    }

    private fun readSnapshot(file: File): Map<String, SnapshotValue> = runCatching {
        val properties = Properties().apply { file.inputStream().use { load(it) } }
        properties.stringPropertyNames()
            .filter { it.contains('.') && !it.endsWith(".present") }
            .associateWith { key ->
                SnapshotValue(
                    present = properties.getProperty("$key.present")?.toBooleanStrictOrNull() ?: true,
                    value = properties.getProperty(key).orEmpty(),
                )
            }
    }.getOrDefault(emptyMap())

    private fun readSnapshotBackend(file: File): SystemProxyBackend? = runCatching {
        val properties = Properties().apply { file.inputStream().use { load(it) } }
        SystemProxyBackend.entries.firstOrNull { it.name == properties.getProperty("backend") }
    }.getOrNull()

    private fun writeSnapshot(backend: SystemProxyBackend, values: Map<String, SnapshotValue>) {
        runCatching {
            snapshotFile.parentFile?.mkdirs()
            val properties = Properties().apply {
                setProperty("backend", backend.name)
                values.forEach { (key, saved) ->
                    setProperty(key, saved.value)
                    setProperty("$key.present", saved.present.toString())
                }
            }
            snapshotFile.outputStream().use { properties.store(it, "system proxy snapshot") }
        }
    }

    private data class SnapshotValue(val present: Boolean, val value: String)

    private data class SnapshotReadResult(val ok: Boolean, val message: String, val values: Map<String, SnapshotValue>)

    private data class CommandResult(val ok: Boolean, val output: String)

    private fun executable(name: String): String? = env("PATH")
        ?.split(File.pathSeparator)
        ?.asSequence()
        ?.mapNotNull { path -> path.takeIf { it.isNotEmpty() }?.let { File(it, name) } }
        ?.firstOrNull { it.isFile && it.canExecute() }
        ?.path

    private fun run(vararg command: String): CommandResult = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (!process.waitFor(8, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            CommandResult(false, "timeout: ${command.joinToString(" ")}")
        } else {
            CommandResult(process.exitValue() == 0, output)
        }
    }.getOrElse { CommandResult(false, it.message ?: it::class.simpleName.orEmpty()) }

    private fun qt(value: String): String = "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"

    private fun formatGVariantList(values: List<String>): String =
        "[" + values.joinToString(", ") { qt(it) } + "]"

    private fun parseGVariantList(raw: String): List<String> =
        Regex("'((?:[^'\\\\]|\\\\.)*)'").findAll(raw)
            .map { it.groupValues[1].replace("\\'", "'").replace("\\\\", "\\") }
            .toList()

    data class ProxyResult(val ok: Boolean, val message: String)
}
