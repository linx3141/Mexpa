package com.linx3141.mexpa.desktop

import java.awt.Font
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Window
import java.awt.event.ActionListener
import javax.imageio.ImageIO

/**
 * 托盘：显示窗口 / 启动 / 停止 / 退出。窗口关闭仍然是退出进程（P2 不引入真正的后台常驻），
 * 托盘只提供最小化之外的快速操作。
 */
object LinuxTray {
    fun install(
        window: Window,
        onShow: () -> Unit,
        onStart: () -> Unit,
        onStop: () -> Unit,
        onExit: () -> Unit,
    ): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        val image = loadIcon() ?: return null
        if (DesktopPlatform.isWindows) {
            Win32Tray.install(
                onShow = { javax.swing.SwingUtilities.invokeLater(onShow) },
                onStart = { javax.swing.SwingUtilities.invokeLater(onStart) },
                onStop = { javax.swing.SwingUtilities.invokeLater(onStop) },
                onExit = { javax.swing.SwingUtilities.invokeLater(onExit) },
            )
            return null
        }
        val popup = PopupMenu()
        // AWT 托盘事件在 AWT EventQueue 上，Compose 的窗口/状态必须在 EDT 上改。
        fun dispatch(block: () -> Unit) = javax.swing.SwingUtilities.invokeLater(block)
        val show = MenuItem("显示主窗口")
        show.addActionListener { dispatch(onShow) }
        val start = MenuItem("启动代理")
        start.addActionListener { dispatch(onStart) }
        val stop = MenuItem("停止代理")
        stop.addActionListener { dispatch(onStop) }
        val exit = MenuItem("退出")
        exit.addActionListener { dispatch(onExit) }
        // AWT 原生菜单默认字体在中文 Windows 上可能没有 CJK glyph，出现整排方块。
        val menuFont = if (DesktopPlatform.isWindows) Font("Microsoft YaHei UI", Font.PLAIN, 12) else null
        listOf(show, start, stop, exit).forEach { item -> menuFont?.let { item.font = it } }
        menuFont?.let { popup.font = it }
        listOf(show, start, stop).forEach { popup.add(it) }
        popup.addSeparator()
        popup.add(exit)
        val trayIcon = TrayIcon(image, "Mexpa", popup)
        trayIcon.isImageAutoSize = true
        trayIcon.addActionListener(ActionListener { dispatch(onShow) })
        runCatching { SystemTray.getSystemTray().add(trayIcon) }
        LinuxNotifications.attach(trayIcon)
        return trayIcon
    }

    fun uninstall(icon: TrayIcon?) {
        if (DesktopPlatform.isWindows) {
            runCatching { Win32Tray.uninstall() }
            return
        }
        if (icon == null) return
        runCatching { SystemTray.getSystemTray().remove(icon) }
    }

    private fun loadIcon(): Image? = runCatching {
        val stream = javaClass.classLoader?.getResourceAsStream(
            "composeResources/com.linx3141.mexpa.shared.resources/drawable/mexpa_icon.png",
        ) ?: return null
        stream.use { ImageIO.read(it) }
    }.getOrNull()
}
