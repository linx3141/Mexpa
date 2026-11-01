package com.linx3141.mexpa.desktop

import java.io.File
import java.util.concurrent.TimeUnit

enum class TunCapability {
    /** 已经能以当前用户创建 TUN（root 或二进制带 cap_net_admin）。 */
    Capable,

    /** 二进制存在但缺少 cap_net_admin，需要一次授权。 */
    MissingCapability,

    BinaryMissing,

    /** 检测环境异常（例如没有 getcap），此时允许尝试启动，失败再看日志。 */
    Unknown,
}

/**
 * TUN 模式的运行配置与权限。
 *
 * 运行时通过 mihomo 的 `--override-json` 把 tun/dns 字段叠到用户配置上（Android 也是改 override，
 * 不是改写用户的 config.yaml）。TUN 需要 CAP_NET_ADMIN：Linux 上没有 Android 的 VpnService，
 * 这里采用 FlClash 之外更轻的做法——对随包 mihomo 一次性 `setcap cap_net_admin+ep`
 * （FlClash 是装一个 root helper systemd 服务；两者都能达到同一目的，后者无需为二进制授 cap）。
 */
object LinuxTun {
    private val STACK_ORDER = listOf(TunStack.Gvisor.id, TunStack.System.id, TunStack.Mixed.id)

    fun supportedStacks(binary: File): List<String> {
        if (!binary.isFile) return STACK_ORDER
        val tags = run(binary.path, "-v").output
            .lineSequence()
            .firstOrNull { it.startsWith("Use tags:") }
            ?.removePrefix("Use tags:")
            ?.split(",")
            ?.map { it.trim() }
            ?.toSet()
            .orEmpty()
        val available = buildSet {
            add(TunStack.System.id)
            if ("with_gvisor" in tags) {
                add(TunStack.Gvisor.id)
                add(TunStack.Mixed.id)
            }
        }
        return STACK_ORDER.filter { it in available }
    }

    fun resolveStack(binary: File, requested: String): String {
        val supported = supportedStacks(binary)
        if (supported.isEmpty()) return TunStack.System.id
        return requested.takeIf { it in supported } ?: supported.first()
    }

    fun capability(binary: File): TunCapability {
        if (!binary.isFile) return TunCapability.BinaryMissing
        if (DesktopPlatform.isWindows) {
            // Windows 没有 setcap；TUN 由 wintun 驱动创建，必须整个进程以管理员身份运行。
            return if (isWindowsAdmin()) TunCapability.Capable else TunCapability.MissingCapability
        }
        if (DesktopPlatform.isMac) {
            // macOS 的 utun 没有 setcap 等价物，需要整个进程以 root 运行。
            return if (currentUid() == 0) TunCapability.Capable else TunCapability.MissingCapability
        }
        // root 直接拥有 CAP_NET_ADMIN（也用于在 netns 里跑集成测试）
        if (currentUid() == 0) return TunCapability.Capable
        val getcap = executable("getcap") ?: return TunCapability.Unknown
        val result = run(getcap, binary.path)
        if (!result.ok) return TunCapability.Unknown
        return if ("cap_net_admin" in result.output) TunCapability.Capable else TunCapability.MissingCapability
    }

    /** 通过 pkexec 走系统 polkit 对话框；返回是否成功。 */
    fun grantCapability(binary: File): ProxyGrantResult {
        if (DesktopPlatform.isWindows) {
            return ProxyGrantResult(
                false,
                "TUN 需要管理员权限：请关闭 Mexpa，右键以“以管理员身份运行”重新启动",
            )
        }
        val pkexec = executable("pkexec")
            ?: return ProxyGrantResult(false, "找不到 pkexec，请手动执行：sudo setcap cap_net_admin+ep ${binary.path}")
        val setcap = executable("setcap")
            ?: return ProxyGrantResult(false, "找不到 setcap，请先安装 libcap（例如 apt install libcap2-bin）")
        val result = run(pkexec, setcap, "cap_net_admin+ep", binary.path)
        return if (result.ok) {
            ProxyGrantResult(true, "TUN 权限已授予")
        } else {
            ProxyGrantResult(
                false,
                "授权失败：${result.output.ifBlank { "pkexec 退出码 ${result.exitCode}" }}；" +
                    "也可手动执行 sudo setcap cap_net_admin+ep ${binary.path}",
            )
        }
    }

    /**
     * 生成 `--override-json` 文件。只写需要的字段：mihomo 的 override 是 JSON 合并到 RawConfig，
     * 未出现的字段保持用户配置原值（嵌套结构同理，所以只写 `dns.enable` 不会清掉用户的 nameserver）。
     */
    fun writeOverride(
        config: File,
        settings: LinuxSettings,
        binary: File,
        target: File,
    ): File {
        val stack = resolveStack(binary, settings.tunStack)
        val hasDns = runCatching { config.isFile && ROOT_DNS.containsMatchIn(config.readText()) }
            .getOrDefault(false)
        val normalized = normalizedOverrideJson(stack, hasDns)
        target.parentFile?.mkdirs()
        target.writeText(normalized)
        return target
    }

    private fun normalizedOverrideJson(stack: String, hasDns: Boolean): String = buildString {
        append("{\n")
        append("  \"tun\": {\n")
        append("    \"enable\": true,\n")
        append("    \"stack\": \"$stack\",\n")
        append("    \"auto-route\": true,\n")
        append("    \"auto-detect-interface\": true,\n")
        append("    \"dns-hijack\": [\"any:53\"]\n")
        append("  }")
        if (hasDns) {
            append(",\n  \"dns\": {\n    \"enable\": true\n  }")
        } else {
            append(",\n  \"dns\": {\n")
            append("    \"enable\": true,\n")
            append("    \"nameserver\": [\"223.5.5.5\", \"119.29.29.29\", \"8.8.8.8\"]\n")
            append("  }")
        }
        append("\n}\n")
    }

    /** 打包脚本不保证带 with_gvisor；找不到 gvisor 时自动退回 system 栈。 */
    fun stackSummary(binary: File): String {
        val supported = supportedStacks(binary)
        return supported.joinToString(" / ")
    }

    private fun isWindowsAdmin(): Boolean {
        val groups = run("whoami", "/groups")
        return groups.ok && (groups.output.contains("S-1-16-12288") || groups.output.contains("S-1-16-16384"))
    }

    private fun currentUid(): Int = if (System.getProperty("user.name") == "root") 0 else -1

    private fun executable(name: String): String? = System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.asSequence()
        ?.mapNotNull { path -> path.takeIf { it.isNotEmpty() }?.let { File(it, name) } }
        ?.firstOrNull { it.isFile && it.canExecute() }
        ?.path

    private val ROOT_DNS = Regex("(?m)^dns\\s*:")

    data class ProxyGrantResult(val ok: Boolean, val message: String)

    internal data class CommandResult(val ok: Boolean, val exitCode: Int, val output: String)

    internal fun run(vararg command: String): CommandResult = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            CommandResult(false, -1, "timeout: ${command.joinToString(" ")}")
        } else {
            CommandResult(process.exitValue() == 0, process.exitValue(), output)
        }
    }.getOrElse { CommandResult(false, -1, it.message ?: it::class.simpleName.orEmpty()) }
}
