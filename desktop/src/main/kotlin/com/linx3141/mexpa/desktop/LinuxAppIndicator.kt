package com.linx3141.mexpa.desktop

import java.io.File

/**
 * KDE/GNOME Wayland 下 AWT 托盘只经 xembedsniproxy 转发图标，点击/菜单经常不工作。
 * 这里优先用 Ayatana AppIndicator（StatusNotifierItem）起一个轻量 Python 托盘 helper；
 * 宿主没有 python3-gi/AppIndicator 时返回 null，由调用方回退 AWT 托盘。
 */
object LinuxAppIndicator {
    var lastFailure: String? = null
        private set

    private const val SCRIPT = """
import os, socket, sys, threading, time
import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib
try:
    gi.require_version("AyatanaAppIndicator3", "0.1")
    from gi.repository import AyatanaAppIndicator3 as AppIndicator
except Exception:
    gi.require_version("AppIndicator3", "0.1")
    from gi.repository import AppIndicator3 as AppIndicator

socket_path, icon_path, parent_pid = sys.argv[1], sys.argv[2], int(sys.argv[3])

def send(cmd):
    try:
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.settimeout(2)
        s.connect(socket_path)
        s.sendall((cmd + "\n").encode())
        try:
            s.recv(256)
        except Exception:
            pass
        s.close()
    except Exception:
        pass

menu = Gtk.Menu()
for label, cmd in [("显示主窗口", "show"), ("启动代理", "start"), ("停止代理", "stop"), (None, None), ("退出", "quit")]:
    if label is None:
        menu.append(Gtk.SeparatorMenuItem())
        continue
    item = Gtk.MenuItem(label=label)
    item.connect("activate", lambda w, c=cmd: send(c))
    menu.append(item)
menu.show_all()

icon_dir = os.path.dirname(icon_path)
icon_name = os.path.splitext(os.path.basename(icon_path))[0]
indicator = AppIndicator.Indicator.new("mexpa", icon_name, AppIndicator.IndicatorCategory.APPLICATION_STATUS)
try:
    indicator.set_icon_theme_path(icon_dir)
except Exception:
    pass
indicator.set_icon_full(icon_name, "Mexpa")
indicator.set_status(AppIndicator.IndicatorStatus.ACTIVE)
indicator.set_menu(menu)
try:
    indicator.set_secondary_activate_target(menu.get_children()[0])
except Exception:
    pass

def watch_parent():
    while True:
        time.sleep(2)
        if not os.path.exists("/proc/%d" % parent_pid):
            GLib.idle_add(Gtk.main_quit)
            return

threading.Thread(target=watch_parent, daemon=True).start()
Gtk.main()
"""

    fun install(stateDir: File): Process? {
        lastFailure = null
        if (System.getenv("MEXPA_DISABLE_APPINDICATOR") == "1") {
            lastFailure = "已通过 MEXPA_DISABLE_APPINDICATOR 禁用"
            return null
        }
        val python = findPython()
        if (python == null) {
            lastFailure = "缺少 python3"
            return null
        }
        if (!supportsIndicator(python)) {
            lastFailure = "缺少 python3-gi / libayatana-appindicator3"
            return null
        }
        val icon = prepareIcon(stateDir) ?: return null
        val script = File(stateDir, "tray-helper.py")
        script.writeText(SCRIPT.trimIndent() + "\n")
        val process = runCatching {
            ProcessBuilder(
                python.path,
                script.path,
                ControlServer.socketPath().toString(),
                icon.path,
                ProcessHandle.current().pid().toString(),
            )
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(File(stateDir, "tray-helper.log")))
                .start()
        }.getOrNull() ?: return null
        Thread.sleep(350)
        if (!process.isAlive) return null
        return process
    }

    /**
     * 旧实现只在 tray-icon.png 不存在时写入，更新包后 AppIndicator 仍会读旧文件。
     * 用内容 hash 命名，既强制刷新托盘图标，也避开桌面环境按 icon name 的缓存。
     */
    private fun prepareIcon(stateDir: File): File? {
        val stream = javaClass.classLoader?.getResourceAsStream(
            "composeResources/com.linx3141.mexpa.shared.resources/drawable/mexpa_icon.png",
        ) ?: return null
        val bytes = stream.use { it.readBytes() }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
            .take(12)
        stateDir.mkdirs()
        val icon = File(stateDir, "tray-icon-$digest.png")
        if (!icon.isFile || icon.length() != bytes.size.toLong()) {
            val temp = File(stateDir, "${icon.name}.part")
            temp.writeBytes(bytes)
            if (icon.exists()) icon.delete()
            if (!temp.renameTo(icon)) {
                icon.writeBytes(bytes)
                temp.delete()
            }
        }
        return icon
    }

    fun uninstall(process: Process?) {
        runCatching { process?.destroy() }
    }

    private fun supportsIndicator(python: File): Boolean = runCatching {
        val test = ProcessBuilder(
            python.path, "-c",
            "import gi\n" +
                "try:\n" +
                "    gi.require_version('AyatanaAppIndicator3','0.1')\n" +
                "except Exception:\n" +
                "    gi.require_version('AppIndicator3','0.1')\n",
        ).redirectErrorStream(true).start()
        test.waitFor(4, java.util.concurrent.TimeUnit.SECONDS) && test.exitValue() == 0
    }.getOrDefault(false)

    private fun findPython(): File? {
        listOf("/usr/bin/python3", "/usr/local/bin/python3").map(::File).firstOrNull { it.canExecute() }?.let { return it }
        val path = System.getenv("PATH").orEmpty().split(':').map { File(it, "python3") }
        return path.firstOrNull { it.canExecute() }
    }
}
