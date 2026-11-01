package com.linx3141.mexpa.desktop

import java.io.File
import java.util.concurrent.TimeUnit

data class ConflictInfo(
    /** 本机已有的、不是本客户端创建的 TUN 设备（例如 FlClash 的 `FlClash`）。 */
    val foreignTun: List<String> = emptyList(),
    /** 系统代理指向别的端口时记录 `类型@地址`。 */
    val otherSystemProxy: String? = null,
    /** mixed-port 已被别的进程监听。 */
    val mixedPortBusy: Boolean = false,
) {
    val hasConflict: Boolean get() = foreignTun.isNotEmpty() || otherSystemProxy != null || mixedPortBusy
}

/**
 * 与其他代理客户端的冲突检测：同时开 FlClash 等客户端时，
 * TUN 路由/系统代理/端口会互相覆盖，mihomo 只在日志里报错，用户看不到原因。
 * 这里只做提示，不阻止启动（用户可能就是想接管）。
 */
object ConflictDetector {
    private val OWN_TUN_NAMES = setOf("Meta", "mexpa")

    fun detect(ourMixedPort: Int, ourProxyRunning: Boolean): ConflictInfo {
        if (DesktopPlatform.isWindows) {
            val busy = if (ourProxyRunning) false else runCatching { isPortListeningWindows(ourMixedPort) }.getOrDefault(false)
            return ConflictInfo(mixedPortBusy = busy)
        }
        val foreignTun = runCatching { detectForeignTun() }.getOrDefault(emptyList())
        val otherSystemProxy = runCatching { detectOtherSystemProxy(ourMixedPort) }.getOrNull()
        val mixedPortBusy = if (ourProxyRunning) false else runCatching { isPortListening(ourMixedPort) }.getOrDefault(false)
        return ConflictInfo(foreignTun, otherSystemProxy, mixedPortBusy)
    }

    private fun detectForeignTun(): List<String> {
        val output = run("ip", "-o", "link", "show").output
        return output.lineSequence().mapNotNull { line ->
            val match = Regex("^\\d+:\\s+([^:]+):").find(line) ?: return@mapNotNull null
            val name = match.groupValues[1].trim()
            if (name == "lo" || name in OWN_TUN_NAMES || name.startsWith("mexpa")) return@mapNotNull null
            val flags = Regex("<([^>]*)>").find(line)?.groupValues?.get(1).orEmpty()
            if ("POINTOPOINT" in flags && "NOARP" in flags) name else null
        }.toList()
    }

    private fun detectOtherSystemProxy(ourMixedPort: Int): String? {
        val desktops = System.getenv("XDG_CURRENT_DESKTOP")?.uppercase().orEmpty()
        if ("KDE" in desktops) {
            val type = run("kreadconfig6", "--file", "${System.getProperty("user.home")}/.config/kioslaverc",
                "--group", "Proxy Settings", "--key", "ProxyType").output
            if (type != "1") return null
            val proxy = run("kreadconfig6", "--file", "${System.getProperty("user.home")}/.config/kioslaverc",
                "--group", "Proxy Settings", "--key", "httpProxy").output
            val port = Regex(":(\\d+)$").find(proxy)?.groupValues?.get(1)?.toIntOrNull()
            return if (port != null && port != ourMixedPort) "KDE@$proxy" else null
        }
        // GNOME/MATE
        if (run("gsettings", "get", "org.gnome.system.proxy", "mode").output.trim() == "'manual'") {
            val port = run("gsettings", "get", "org.gnome.system.proxy.http", "port").output.trim().toIntOrNull()
            if (port != null && port != ourMixedPort) return "GNOME@127.0.0.1:$port"
        }
        return null
    }

    private fun isPortListeningWindows(port: Int): Boolean = runCatching {
        java.net.Socket().use { socket ->
            socket.connect(java.net.InetSocketAddress("127.0.0.1", port), 250)
            true
        }
    }.getOrDefault(false)

    private fun isPortListening(port: Int): Boolean {
        val hex = port.toString(16).uppercase().padStart(4, '0')
        val tables = listOf("/proc/net/tcp", "/proc/net/tcp6")
        return tables.any { table ->
            runCatching {
                File(table).readLines().drop(1).any { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    val local = parts.getOrNull(1) ?: return@any false
                    val state = parts.getOrNull(3) ?: return@any false
                    state == "0A" && local.endsWith(":$hex")
                }
            }.getOrDefault(false)
        }
    }

    private fun run(vararg command: String): CommandOut = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
        CommandOut(output)
    }.getOrDefault(CommandOut(""))

    private data class CommandOut(val output: String)
}
