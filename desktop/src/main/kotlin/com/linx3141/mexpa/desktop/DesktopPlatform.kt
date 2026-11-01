package com.linx3141.mexpa.desktop

import java.io.File

/** 桌面端平台差异的唯一入口：路径、二进制名、窗口标题等。 */
object DesktopPlatform {
    val isWindows: Boolean = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
    val isMac: Boolean = !isWindows && System.getProperty("os.name").orEmpty().contains("Mac", ignoreCase = true)
    val isLinux: Boolean = !isWindows && !isMac && System.getProperty("os.name").orEmpty().contains("Linux", ignoreCase = true)

    private val home: File get() = File(System.getProperty("user.home"))

    /** 配置目录：Linux `~/.config/mexpa`，Windows `%APPDATA%\\mexpa`，macOS `~/Library/Application Support/mexpa`。 */
    val configDir: File by lazy {
        when {
            isWindows -> File(System.getenv("APPDATA")?.takeIf { it.isNotBlank() } ?: home.path, "mexpa")
            isMac -> File(home, "Library/Application Support/mexpa")
            else -> File(home, ".config/mexpa")
        }
    }

    /** 运行状态目录：Linux `~/.local/state/mexpa`，Windows `%LOCALAPPDATA%\\mexpa`，macOS 同 Application Support。 */
    val stateDir: File by lazy {
        when {
            isWindows -> File(System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: home.path, "mexpa")
            isMac -> File(home, "Library/Application Support/mexpa/state")
            else -> File(home, ".local/state/mexpa")
        }
    }

    val dataDir: File by lazy {
        when {
            isWindows -> File(System.getenv("APPDATA")?.takeIf { it.isNotBlank() } ?: home.path, "mexpa")
            isMac -> File(home, "Library/Application Support/mexpa")
            else -> File(home, ".local/share/mexpa")
        }
    }

    val binaryName: String get() = if (isWindows) "mihomo.exe" else "mihomo"
    val transformBinaryName: String get() = if (isWindows) "mexpa-transform.exe" else "mexpa-transform"
    val windowTitle: String get() = when {
        isWindows -> "Mexpa for Windows"
        isMac -> "Mexpa for macOS"
        else -> "Mexpa for Linux"
    }
    val systemProxyLabel: String get() = when {
        isWindows -> "Windows (WinINet)"
        isMac -> "macOS (networksetup)"
        else -> "系统代理"
    }

    fun defaultConfigFile(): File = File(configDir, "config.yaml")

    fun ensureDirs() {
        configDir.mkdirs()
        stateDir.mkdirs()
        dataDir.mkdirs()
    }
}
