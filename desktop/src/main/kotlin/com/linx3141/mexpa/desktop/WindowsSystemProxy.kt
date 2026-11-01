package com.linx3141.mexpa.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Windows 系统代理：写 HKCU Internet Settings + 通知 WinINet。
 *
 * FlClash 用 C++ 直接 InternetSetOption；这里用 reg.exe 写注册表，再用 PowerShell
 * P/Invoke 通知 WinINet，避免为一次系统代理引入额外的 native 依赖。
 * 与 Linux 实现一样，先保存原始值，停止/崩溃恢复时原样写回。
 */
class WindowsSystemProxy(private val snapshotFile: File) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"
    private val bypass = listOf(
        "localhost", "127.*", "10.*", "172.16.*", "172.17.*", "172.18.*", "172.19.*",
        "172.2*", "172.30.*", "172.31.*", "192.168.*", "*.local", "*.lan", "<local>",
    ).joinToString(";")

    @Volatile private var applied = false

    fun backendLabel(): String = "Windows (WinINet)"

    fun available(): Boolean = DesktopPlatform.isWindows

    fun recoverIfStale(): String? {
        if (!snapshotFile.isFile) return null
        val result = restore()
        return if (result.ok) "已恢复上次异常退出前的 Windows 系统代理设置" else result.message
    }

    @Synchronized
    fun apply(port: Int, pacUrl: String? = null): LinuxSystemProxy.ProxyResult {
        if (applied) return LinuxSystemProxy.ProxyResult(true, "系统代理已是 127.0.0.1:$port")
        if (snapshotFile.isFile) restore()
        val current = readSettings()
        writeSnapshot(current)
        val proxy = "127.0.0.1:$port"
        val ok = if (!pacUrl.isNullOrBlank()) {
            setValue("ProxyEnable", "0", "REG_DWORD") &&
                setValue("AutoConfigURL", pacUrl, "REG_SZ") &&
                setValue("ProxyOverride", bypass, "REG_SZ")
        } else {
            setValue("ProxyEnable", "1", "REG_DWORD") &&
                setValue("ProxyServer", proxy, "REG_SZ") &&
                setValue("ProxyOverride", bypass, "REG_SZ") &&
                deleteValue("AutoConfigURL")
        }
        if (!ok) {
            restore()
            return LinuxSystemProxy.ProxyResult(false, "写入 Windows 系统代理注册表失败")
        }
        notifyWinInet()
        applied = true
        return LinuxSystemProxy.ProxyResult(true, "Windows 系统代理已指向 127.0.0.1:$port")
    }

    @Synchronized
    fun restore(): LinuxSystemProxy.ProxyResult {
        if (!snapshotFile.isFile) {
            applied = false
            return LinuxSystemProxy.ProxyResult(true, "系统代理未由 Mexpa 修改")
        }
        val snapshot = runCatching {
            json.decodeFromString(ProxySnapshot.serializer(), snapshotFile.readText())
        }.getOrNull()
        if (snapshot == null) {
            snapshotFile.delete()
            applied = false
            return LinuxSystemProxy.ProxyResult(false, "系统代理快照损坏，无法恢复")
        }
        var ok = true
        ok = restoreValue("ProxyEnable", snapshot.proxyEnable, "REG_DWORD") && ok
        ok = restoreValue("ProxyServer", snapshot.proxyServer, "REG_SZ") && ok
        ok = restoreValue("ProxyOverride", snapshot.proxyOverride, "REG_SZ") && ok
        ok = restoreValue("AutoConfigURL", snapshot.autoConfigUrl, "REG_SZ") && ok
        notifyWinInet()
        if (ok) snapshotFile.delete()
        applied = false
        return LinuxSystemProxy.ProxyResult(ok, if (ok) "已恢复原 Windows 系统代理设置" else "恢复 Windows 系统代理部分失败")
    }

    private fun readSettings(): ProxySnapshot = ProxySnapshot(
        proxyEnable = readValue("ProxyEnable"),
        proxyServer = readValue("ProxyServer"),
        proxyOverride = readValue("ProxyOverride"),
        autoConfigUrl = readValue("AutoConfigURL"),
    )

    private fun readValue(name: String): String? = runCatching {
        val result = run("reg", "query", key, "/v", name)
        if (!result.ok) return null
        val line = result.output.lineSequence().firstOrNull { it.trim().startsWith(name, ignoreCase = true) }
            ?: return null
        line.trim().split(Regex("\\s+"), limit = 3).getOrNull(2)
    }.getOrNull()

    private fun setValue(name: String, value: String, type: String): Boolean =
        run("reg", "add", key, "/v", name, "/t", type, "/d", value, "/f").ok

    private fun deleteValue(name: String): Boolean =
        run("reg", "delete", key, "/v", name, "/f").ok || run("reg", "query", key, "/v", name).ok.not()

    private fun restoreValue(name: String, value: String?, type: String): Boolean =
        if (value == null) deleteValue(name) else setValue(name, value, type)

    private fun writeSnapshot(snapshot: ProxySnapshot) {
        snapshotFile.parentFile?.mkdirs()
        snapshotFile.writeText(json.encodeToString(ProxySnapshot.serializer(), snapshot))
    }

    private fun notifyWinInet() {
        // 避免 PowerShell here-string 对缩进/终止符敏感；TypeDefinition 用单引号字符串。
        val script = """
            Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class MexpaWinInet { [DllImport("wininet.dll", SetLastError=true)] public static extern bool InternetSetOption(IntPtr h, int o, IntPtr b, int l); }'
            [MexpaWinInet]::InternetSetOption([IntPtr]::Zero, 39, [IntPtr]::Zero, 0) | Out-Null
            [MexpaWinInet]::InternetSetOption([IntPtr]::Zero, 37, [IntPtr]::Zero, 0) | Out-Null
        """.trimIndent()
        runCatching {
            val p = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectErrorStream(true).start()
            p.waitFor(3, TimeUnit.SECONDS)
        }
    }

    private data class CommandResult(val ok: Boolean, val output: String)

    private fun run(vararg command: String): CommandResult = runCatching {
        val p = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = p.inputStream.bufferedReader().use { it.readText() }
        if (!p.waitFor(8, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            CommandResult(false, "timeout")
        } else {
            CommandResult(p.exitValue() == 0, output)
        }
    }.getOrElse { CommandResult(false, it.message.orEmpty()) }

    @Serializable
    private data class ProxySnapshot(
        val proxyEnable: String? = null,
        val proxyServer: String? = null,
        val proxyOverride: String? = null,
        val autoConfigUrl: String? = null,
    )
}
