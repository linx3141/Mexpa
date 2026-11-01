package com.linx3141.mexpa.desktop

import java.awt.TrayIcon

/**
 * 系统通知：优先 `notify-send`（KDE/GNOME 原生通知），
 * 托盘 displayMessage 只在没有 notify-send 时兜底（AWT 的 X11 气泡在 KDE 上很丑）。
 */
object LinuxNotifications {
    @Volatile
    private var tray: TrayIcon? = null

    fun attach(icon: TrayIcon?) {
        tray = icon
    }

    fun notify(title: String, message: String) {
        if (DesktopPlatform.isMac) {
            Thread({
                val escapedTitle = title.replace("\\", "\\\\").replace("\"", "\\\"")
                val escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"")
                val script = "display notification \"$escapedMessage\" with title \"$escapedTitle\""
                val ok = runCatching {
                    val process = ProcessBuilder("/usr/bin/osascript", "-e", script).start()
                    process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0
                }.getOrDefault(false)
                if (!ok) {
                    tray?.let { icon ->
                        javax.swing.SwingUtilities.invokeLater {
                            runCatching { icon.displayMessage(title, message, TrayIcon.MessageType.INFO) }
                        }
                    }
                }
            }, "mexpa-notify").apply { isDaemon = true }.start()
            return
        }
        if (DesktopPlatform.isWindows) {
            val icon = tray ?: return
            javax.swing.SwingUtilities.invokeLater {
                runCatching { icon.displayMessage(title, message, TrayIcon.MessageType.INFO) }
            }
            return
        }
        Thread({
            val native = runCatching {
                val process = ProcessBuilder("notify-send", "-a", "Mexpa", "-i", "network-vpn", title, message).start()
                process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0
            }.getOrDefault(false)
            if (!native) {
                val icon = tray
                if (icon != null) {
                    javax.swing.SwingUtilities.invokeLater {
                        runCatching { icon.displayMessage(title, message, TrayIcon.MessageType.INFO) }
                    }
                }
            }
        }, "mexpa-notify").apply { isDaemon = true }.start()
    }
}
