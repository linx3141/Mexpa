package com.linx3141.mexpa.desktop

import java.io.File

/** 打包版注册 clash:// / clashmeta:// scheme（xdg-mime + 用户级 .desktop）。 */
object DesktopIntegration {
    fun registerClashScheme(): Boolean {
        val launcher = System.getenv("APPIMAGE")?.takeIf { File(it).isFile }
            ?: System.getProperty("jpackage.app-path") ?: return false
        if (DesktopPlatform.isWindows) {
            val command = "\"$launcher\" \"%1\""
            listOf("clash", "clashmeta").forEach { scheme ->
                val base = "HKCU\\Software\\Classes\\$scheme"
                runCatching {
                    ProcessBuilder("reg", "add", base, "/ve", "/t", "REG_SZ", "/d", "URL:Mexpa Protocol", "/f").start().waitFor()
                    ProcessBuilder("reg", "add", base, "/v", "URL Protocol", "/t", "REG_SZ", "/d", "", "/f").start().waitFor()
                    ProcessBuilder("reg", "add", "$base\\shell\\open\\command", "/ve", "/t", "REG_SZ", "/d", command, "/f")
                        .start().waitFor()
                }
            }
            return true
        }
        val dir = File(System.getProperty("user.home"), ".local/share/applications")
        dir.mkdirs()
        val desktop = File(dir, "mexpa-url.desktop")
        val exec = if (launcher.contains(' ')) "\"$launcher\"" else launcher
        val content = """
            [Desktop Entry]
            Type=Application
            Name=Mexpa URL Handler
            NoDisplay=true
            Exec=$exec %u
            Terminal=false
            MimeType=x-scheme-handler/clash;x-scheme-handler/clashmeta;
        """.trimIndent() + "\n"
        val changed = !desktop.isFile || desktop.readText() != content
        if (changed) desktop.writeText(content)
        runCatching {
            ProcessBuilder("update-desktop-database", dir.path).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        }
        listOf("clash", "clashmeta").forEach { scheme ->
            runCatching {
                ProcessBuilder("xdg-mime", "default", "mexpa-url.desktop", "x-scheme-handler/$scheme")
                    .start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        return true
    }
}
