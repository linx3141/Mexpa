package com.linx3141.mexpa.desktop

import java.io.File

/**
 * 登录自启：写 `~/.config/autostart/mexpa.desktop`。
 * 仅打包版可用（jpackage 启动器会设置 `jpackage.app-path`）；开发态返回不可用。
 */
object Autostart {
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val RUN_VALUE = "Mexpa"

    fun desktopFile(): File = File(DesktopPlatform.configDir.parentFile, "autostart/mexpa.desktop")
    private fun launchAgentFile(): File = File(
        System.getProperty("user.home"),
        "Library/LaunchAgents/com.linx3141.mexpa.plist",
    )

    fun isEnabled(): Boolean = when {
        DesktopPlatform.isWindows -> run("reg", "query", RUN_KEY, "/v", RUN_VALUE).ok
        DesktopPlatform.isMac -> launchAgentFile().isFile
        else -> desktopFile().isFile
    }

    fun available(): Boolean = launcherCommand() != null

    fun setEnabled(enabled: Boolean): Boolean {
        if (DesktopPlatform.isWindows) {
            if (!enabled) {
                run("reg", "delete", RUN_KEY, "/v", RUN_VALUE, "/f")
                return true
            }
            val command = launcherCommand() ?: return false
            return run("reg", "add", RUN_KEY, "/v", RUN_VALUE, "/t", "REG_SZ", "/d", "$command --minimized", "/f").ok
        }
        val file = desktopFile()
        if (DesktopPlatform.isMac) {
            if (!enabled) {
                launchAgentFile().delete()
                return true
            }
        } else if (!enabled) {
            file.delete()
            return true
        }
        if (DesktopPlatform.isMac) {
            val command = launcherCommand() ?: return false
            launchAgentFile().parentFile?.mkdirs()
            launchAgentFile().writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>Label</key>
                    <string>com.linx3141.mexpa</string>
                    <key>ProgramArguments</key>
                    <array>
                        <string>${xmlEscape(command)}</string>
                        <string>--minimized</string>
                    </array>
                    <key>RunAtLoad</key>
                    <true/>
                </dict>
                </plist>
                """.trimIndent() + "\n",
            )
            return true
        }
        val command = launcherCommand() ?: return false
        file.parentFile?.mkdirs()
        file.writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=Mexpa
            Comment=Mexpa for Linux
            Exec=$command --minimized
            Terminal=false
            X-GNOME-Autostart-enabled=true
            """.trimIndent() + "\n",
        )
        return true
    }

    private fun launcherCommand(): String? {
        System.getenv("APPIMAGE")?.takeIf { File(it).isFile }?.let { return quote(it) }
        System.getProperty("jpackage.app-path")?.let { return quote(it) }
        return null
    }

    private fun quote(path: String): String = if (path.contains(' ')) "\"$path\"" else path

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private data class Result(val ok: Boolean)

    private fun run(vararg command: String): Result = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0
    }.fold({ Result(it) }, { Result(false) })
}
