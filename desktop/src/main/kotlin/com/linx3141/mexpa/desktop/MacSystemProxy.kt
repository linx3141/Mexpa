package com.linx3141.mexpa.desktop

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * macOS 系统代理：networksetup 没有统一的非特权 API，修改代理状态需要管理员授权，
 * 所以读取用当前用户、写入统一包进一次 `osascript ... with administrator privileges`。
 */
class MacSystemProxy(
    private val snapshotFile: File,
    private val networksetup: String = "/usr/sbin/networksetup",
) {
    private data class ServiceState(
        val name: String,
        val webEnabled: Boolean = false,
        val webServer: String = "",
        val webPort: Int = 0,
        val secureEnabled: Boolean = false,
        val secureServer: String = "",
        val securePort: Int = 0,
        val autoEnabled: Boolean = false,
        val autoUrl: String = "",
        val bypass: List<String> = emptyList(),
    )

    private data class CommandResult(val ok: Boolean, val output: String = "")

    fun available(): Boolean = File(networksetup).canExecute() && services().isNotEmpty()

    fun backendLabel(): String = if (available()) "macOS (networksetup)" else "未检测到 networksetup"

    @Synchronized
    fun recoverIfStale(): String? {
        if (!snapshotFile.isFile) return null
        val states = readSnapshot() ?: run {
            snapshotFile.delete()
            return null
        }
        val result = restoreStates(states)
        if (result.ok) snapshotFile.delete()
        return if (result.ok) {
            "已恢复上次异常退出前的系统代理设置（macOS networksetup）"
        } else {
            "恢复上次系统代理设置失败：${result.output.ifBlank { "networksetup 退出失败" }}"
        }
    }

    @Synchronized
    fun apply(port: Int, pacUrl: String? = null): LinuxSystemProxy.ProxyResult {
        val current = readServices()
        if (current.isEmpty()) return LinuxSystemProxy.ProxyResult(false, "没有可用的网络服务（networksetup）")
        if (snapshotFile.isFile) {
            readSnapshot()?.let { runCatching { restoreStates(it) } }
            snapshotFile.delete()
        }
        writeSnapshot(current)
        val bypass = DEFAULT_BYPASS
        val commands = mutableListOf<List<String>>()
        current.forEach { service ->
            if (!pacUrl.isNullOrBlank()) {
                commands += listOf("-setwebproxystate", service.name, "off")
                commands += listOf("-setsecurewebproxystate", service.name, "off")
                commands += listOf("-setautoproxyurl", service.name, pacUrl)
                commands += listOf("-setautoproxystate", service.name, "on")
            } else {
                commands += listOf("-setwebproxy", service.name, "127.0.0.1", port.toString())
                commands += listOf("-setsecurewebproxy", service.name, "127.0.0.1", port.toString())
                commands += listOf("-setwebproxystate", service.name, "on")
                commands += listOf("-setsecurewebproxystate", service.name, "on")
                commands += listOf("-setautoproxystate", service.name, "off")
                commands += listOf("-setproxybypassdomains", service.name) + bypass
            }
        }
        val result = runAdmin(commands)
        if (!result.ok) {
            runCatching { restoreStates(current) }
            snapshotFile.delete()
            return LinuxSystemProxy.ProxyResult(false, "设置 macOS 系统代理失败：${result.output.ifBlank { "已取消管理员授权" }}")
        }
        return LinuxSystemProxy.ProxyResult(
            true,
            if (pacUrl.isNullOrBlank()) "系统代理已指向 127.0.0.1:$port（macOS networksetup）" else "系统 PAC 已指向 $pacUrl（macOS networksetup）",
        )
    }

    @Synchronized
    fun restore(): LinuxSystemProxy.ProxyResult {
        if (!snapshotFile.isFile) {
            return LinuxSystemProxy.ProxyResult(true, "系统代理未由 Mexpa 修改")
        }
        val states = readSnapshot()
        if (states == null) {
            snapshotFile.delete()
            return LinuxSystemProxy.ProxyResult(false, "系统代理快照损坏，无法恢复")
        }
        val result = restoreStates(states)
        if (result.ok) snapshotFile.delete()
        return if (result.ok) {
            LinuxSystemProxy.ProxyResult(true, "已恢复 macOS 系统代理")
        } else {
            LinuxSystemProxy.ProxyResult(false, "恢复 macOS 系统代理失败：${result.output.ifBlank { "networksetup 退出失败" }}")
        }
    }

    private fun services(): List<String> {
        val result = run(listOf(networksetup, "-listallnetworkservices"))
        if (!result.ok) return emptyList()
        return result.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("An asterisk") }
            .map { it.removePrefix("*").trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun readServices(): List<ServiceState> = services().map { name ->
        val web = readProxy(name, "-getwebproxy")
        val secure = readProxy(name, "-getsecurewebproxy")
        val auto = readAutoProxy(name)
        val bypass = run(listOf(networksetup, "-getproxybypassdomains", name)).output
            .lineSequence().map { it.trim() }.filter { it.isNotEmpty() && it != "There aren't any proxy bypass domains set." }.toList()
        ServiceState(
            name = name,
            webEnabled = web.enabled,
            webServer = web.server,
            webPort = web.port,
            secureEnabled = secure.enabled,
            secureServer = secure.server,
            securePort = secure.port,
            autoEnabled = auto.first,
            autoUrl = auto.second,
            bypass = bypass,
        )
    }

    private data class ProxyRead(val enabled: Boolean, val server: String, val port: Int)

    private fun readProxy(service: String, flag: String): ProxyRead {
        val output = run(listOf(networksetup, flag, service)).output
        fun field(name: String): String = output.lineSequence()
            .firstOrNull { it.substringBefore(':').trim().equals(name, ignoreCase = true) }
            ?.substringAfter(':')?.trim().orEmpty()
        return ProxyRead(
            enabled = field("Enabled").equals("Yes", ignoreCase = true),
            server = field("Server"),
            port = field("Port").toIntOrNull() ?: 0,
        )
    }

    private fun readAutoProxy(service: String): Pair<Boolean, String> {
        val output = run(listOf(networksetup, "-getautoproxyurl", service)).output
        fun field(name: String): String = output.lineSequence()
            .firstOrNull { it.substringBefore(':').trim().equals(name, ignoreCase = true) }
            ?.substringAfter(':')?.trim().orEmpty()
        return field("Enabled").equals("Yes", ignoreCase = true) to field("URL")
    }

    private fun writeSnapshot(states: List<ServiceState>) {
        val props = Properties()
        props["count"] = states.size.toString()
        states.forEachIndexed { index, state ->
            val prefix = "s$index."
            props["${prefix}name"] = state.name
            props["${prefix}webEnabled"] = state.webEnabled.toString()
            props["${prefix}webServer"] = state.webServer
            props["${prefix}webPort"] = state.webPort.toString()
            props["${prefix}secureEnabled"] = state.secureEnabled.toString()
            props["${prefix}secureServer"] = state.secureServer
            props["${prefix}securePort"] = state.securePort.toString()
            props["${prefix}autoEnabled"] = state.autoEnabled.toString()
            props["${prefix}autoUrl"] = state.autoUrl
            props["${prefix}bypass"] = state.bypass.joinToString("\u001f")
        }
        snapshotFile.parentFile?.mkdirs()
        snapshotFile.outputStream().use { props.store(it, "Mexpa macOS proxy snapshot") }
    }

    private fun readSnapshot(): List<ServiceState>? = runCatching {
        val props = Properties()
        snapshotFile.inputStream().use { props.load(it) }
        val count = props.getProperty("count")?.toIntOrNull() ?: return null
        (0 until count).map { index ->
            val prefix = "s$index."
            ServiceState(
                name = props.getProperty("${prefix}name").orEmpty(),
                webEnabled = props.getProperty("${prefix}webEnabled").toBoolean(),
                webServer = props.getProperty("${prefix}webServer").orEmpty(),
                webPort = props.getProperty("${prefix}webPort")?.toIntOrNull() ?: 0,
                secureEnabled = props.getProperty("${prefix}secureEnabled").toBoolean(),
                secureServer = props.getProperty("${prefix}secureServer").orEmpty(),
                securePort = props.getProperty("${prefix}securePort")?.toIntOrNull() ?: 0,
                autoEnabled = props.getProperty("${prefix}autoEnabled").toBoolean(),
                autoUrl = props.getProperty("${prefix}autoUrl").orEmpty(),
                bypass = props.getProperty("${prefix}bypass").orEmpty()
                    .split('\u001f').filter { it.isNotBlank() },
            )
        }
    }.getOrNull()

    private fun restoreStates(states: List<ServiceState>): CommandResult {
        val commands = mutableListOf<List<String>>()
        states.forEach { state ->
            if (state.webEnabled) {
                if (state.webServer.isNotBlank() && state.webPort > 0) {
                    commands += listOf("-setwebproxy", state.name, state.webServer, state.webPort.toString())
                }
                commands += listOf("-setwebproxystate", state.name, "on")
            } else {
                commands += listOf("-setwebproxystate", state.name, "off")
            }
            if (state.secureEnabled) {
                if (state.secureServer.isNotBlank() && state.securePort > 0) {
                    commands += listOf("-setsecurewebproxy", state.name, state.secureServer, state.securePort.toString())
                }
                commands += listOf("-setsecurewebproxystate", state.name, "on")
            } else {
                commands += listOf("-setsecurewebproxystate", state.name, "off")
            }
            if (state.autoEnabled && state.autoUrl.isNotBlank()) {
                commands += listOf("-setautoproxyurl", state.name, state.autoUrl)
                commands += listOf("-setautoproxystate", state.name, "on")
            } else {
                commands += listOf("-setautoproxystate", state.name, "off")
            }
            if (state.bypass.isNotEmpty()) {
                commands += listOf("-setproxybypassdomains", state.name) + state.bypass
            }
        }
        return runAdmin(commands)
    }

    private fun runAdmin(commands: List<List<String>>): CommandResult {
        if (commands.isEmpty()) return CommandResult(true)
        val shell = commands.joinToString("; ") { args ->
            args.joinToString(" ") { shellQuote(it) }
        }
        val script = "do shell script ${appleQuote(shell)} with administrator privileges"
        val result = run(listOf("/usr/bin/osascript", "-e", script), timeoutSeconds = 120)
        if (result.ok) return result
        // 没有图形会话/Authorization 不可用时，至少给出可诊断的输出。
        return result
    }

    private fun run(command: List<String>, timeoutSeconds: Long = 8): CommandResult = runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            CommandResult(false, "命令超时：${command.joinToString(" ")}")
        } else {
            CommandResult(process.exitValue() == 0, output.trim())
        }
    }.getOrElse { CommandResult(false, it.message ?: "命令执行失败") }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun appleQuote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        val DEFAULT_BYPASS = listOf(
            "localhost", "127.0.0.1", "::1", "*.local", "169.254/16", "10.0.0.0/8",
            "172.16.0.0/12", "192.168.0.0/16",
        )
    }
}
