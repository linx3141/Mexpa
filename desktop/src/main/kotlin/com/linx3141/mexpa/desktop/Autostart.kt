package com.linx3141.mexpa.desktop

import java.io.File

/**
 * 登录自启：写 `~/.config/autostart/mexpa.desktop`。
 * 仅打包版可用（jpackage 启动器会设置 `jpackage.app-path`）；开发态返回不可用。
 */
object Autostart {
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val RUN_VALUE = "Mexpa"
    private val TASK_NAME: String get() = "MexpaAutostart-" + System.getProperty("user.name")

    fun desktopFile(): File = File(DesktopPlatform.configDir.parentFile, "autostart/mexpa.desktop")
    private fun launchAgentFile(): File = File(
        System.getProperty("user.home"),
        "Library/LaunchAgents/com.linx3141.mexpa.plist",
    )

    fun isEnabled(): Boolean = when {
        DesktopPlatform.isWindows -> taskEnabled() || legacyRunEnabled()
        DesktopPlatform.isMac -> launchAgentFile().isFile
        else -> desktopFile().isFile
    }

    fun available(): Boolean = launcherCommand() != null

    fun setEnabled(enabled: Boolean): Boolean {
        if (DesktopPlatform.isWindows) {
            val command = launcherCommand() ?: return false
            // 登录任务使用“最高权限”，否则 HKCU Run 只能以普通权限启动，开 TUN 时仍会弹 UAC。
            // 注册任务需要管理员；通过 runas 弹一次 UAC，之后登录不再需要。
            val script = if (enabled) {
                windowsEnableScript(command)
            } else {
                windowsDisableScript()
            }
            return WindowsElevation.runElevatedPowerShell(script)
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


    private fun taskEnabled(): Boolean =
        run("schtasks", "/Query", "/TN", TASK_NAME, "/FO", "LIST").ok

    private fun legacyRunEnabled(): Boolean =
        run("reg", "query", RUN_KEY, "/v", RUN_VALUE).ok

    private fun windowsEnableScript(command: String): String {
        val taskName = psQuote(TASK_NAME)
        val executable = psQuote(unquote(command))
        return """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}action = New-ScheduledTaskAction -Execute '$executable' -Argument '--minimized'
            ${'$'}trigger = New-ScheduledTaskTrigger -AtLogOn
            ${'$'}currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
            ${'$'}principal = New-ScheduledTaskPrincipal -UserId ${'$'}currentUser -LogonType Interactive -RunLevel Highest
            Register-ScheduledTask -TaskName '$taskName' -Action ${'$'}action -Trigger ${'$'}trigger -Principal ${'$'}principal -Force | Out-Null
            Remove-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' -Name 'Mexpa' -ErrorAction SilentlyContinue
        """.trimIndent()
    }

    private fun windowsDisableScript(): String = """
        ${'$'}ErrorActionPreference = 'SilentlyContinue'
        Unregister-ScheduledTask -TaskName '${psQuote(TASK_NAME)}' -Confirm:${'$'}false
        Remove-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' -Name 'Mexpa'
    """.trimIndent()

    private fun unquote(value: String): String =
        value.trim().removeSurrounding("\"", "\"")

    private fun psQuote(value: String): String = value.replace("'", "''")

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
