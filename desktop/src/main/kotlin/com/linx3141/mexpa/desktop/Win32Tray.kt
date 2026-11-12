package com.linx3141.mexpa.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.StdCallLibrary.StdCallCallback
import com.sun.jna.win32.W32APIOptions
import java.io.File
import javax.swing.SwingUtilities

/**
 * Windows 原生托盘图标 + 原生右键菜单。
 *
 * AWT SystemTray 的右键事件和 Win32 TrackPopupMenuEx 混用时，菜单打开时 AWT 事件
 * 仍会继续投递，第二次右键容易把菜单关掉。这里直接用 Shell_NotifyIconW + 自己的
 * 窗口过程，鼠标消息和菜单都在同一个 Win32 消息线程里处理。
 */
object Win32Tray {
    private const val WM_DESTROY = 0x0002
    private const val WM_CLOSE = 0x0010
    private const val WM_COMMAND = 0x0111
    private const val WM_LBUTTONUP = 0x0202
    private const val WM_LBUTTONDBLCLK = 0x0203
    private const val WM_RBUTTONUP = 0x0205
    private const val WM_NULL = 0x0000
    private const val WM_APP = 0x8000
    private const val TRAY_MESSAGE = WM_APP + 1

    private const val NIM_ADD = 0
    private const val NIM_MODIFY = 1
    private const val NIM_DELETE = 2
    private const val NIF_MESSAGE = 0x00000001
    private const val NIF_ICON = 0x00000002
    private const val NIF_TIP = 0x00000004
    private const val NIF_INFO = 0x00000010
    private const val NIIF_INFO = 0x00000001

    private const val MF_STRING = 0x00000000
    private const val MF_SEPARATOR = 0x00000800
    private const val TPM_RETURNCMD = 0x0100
    private const val TPM_NONOTIFY = 0x0080
    private const val TPM_RIGHTBUTTON = 0x0002
    private const val WS_POPUP = 0x80000000.toInt()
    private const val SW_SHOWNA = 8

    private interface User32 : StdCallLibrary {
        fun RegisterClassExW(windowClass: WNDCLASSEXW): Short
        fun UnregisterClassW(className: WString?, instance: Pointer?): Boolean
        fun CreateWindowExW(
            exStyle: Int, className: WString?, windowName: WString?, style: Int,
            x: Int, y: Int, width: Int, height: Int,
            parent: Pointer?, menu: Pointer?, instance: Pointer?, param: Pointer?,
        ): Pointer?
        fun DestroyWindow(window: Pointer?): Boolean
        fun DefWindowProcW(window: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer?
        fun GetMessageW(msg: MSG, window: Pointer?, min: Int, max: Int): Int
        fun TranslateMessage(msg: MSG): Boolean
        fun DispatchMessageW(msg: MSG): Pointer?
        fun PostQuitMessage(exitCode: Int)
        fun PostMessageW(window: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Boolean
        fun GetCursorPos(point: POINT): Boolean
        fun SetForegroundWindow(window: Pointer?): Boolean
        fun GetForegroundWindow(): Pointer?
        fun ShowWindow(window: Pointer?, command: Int): Boolean
        fun GetWindowThreadProcessId(window: Pointer?, processId: IntByReference?): Int
        fun CreatePopupMenu(): Pointer?
        fun AppendMenuW(menu: Pointer?, flags: Int, id: Int, text: WString?): Boolean
        fun TrackPopupMenuEx(menu: Pointer?, flags: Int, x: Int, y: Int, owner: Pointer?, params: Pointer?): Int
        fun DestroyMenu(menu: Pointer?): Boolean
        fun LoadIconW(instance: Pointer?, iconName: Pointer?): Pointer?
        fun DestroyIcon(icon: Pointer?): Boolean
    }

    private interface Kernel32 : StdCallLibrary {
        fun GetModuleHandleW(name: WString?): Pointer?
        fun GetCurrentThreadId(): Int
        fun AttachThreadInput(idAttach: Int, idAttachTo: Int, attach: Boolean): Boolean
    }

    private interface Shell32 : StdCallLibrary {
        fun Shell_NotifyIconW(message: Int, data: NOTIFYICONDATAW): Boolean
        fun ExtractIconExW(fileName: WString?, index: Int, large: PointerByReference?, small: PointerByReference?, icons: Int): Int
    }

    interface WndProc : StdCallCallback {
        fun callback(window: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer?
    }

    @Structure.FieldOrder(
        "cbSize", "style", "lpfnWndProc", "cbClsExtra", "cbWndExtra",
        "hInstance", "hIcon", "hCursor", "hbrBackground", "lpszMenuName",
        "lpszClassName", "hIconSm",
    )
    class WNDCLASSEXW : Structure() {
        @JvmField var cbSize: Int = 0
        @JvmField var style: Int = 0
        @JvmField var lpfnWndProc: WndProc? = null
        @JvmField var cbClsExtra: Int = 0
        @JvmField var cbWndExtra: Int = 0
        @JvmField var hInstance: Pointer? = null
        @JvmField var hIcon: Pointer? = null
        @JvmField var hCursor: Pointer? = null
        @JvmField var hbrBackground: Pointer? = null
        @JvmField var lpszMenuName: WString? = null
        @JvmField var lpszClassName: WString? = null
        @JvmField var hIconSm: Pointer? = null

        init { cbSize = size() }
    }

    class POINT : Structure() {
        @JvmField var x: Int = 0
        @JvmField var y: Int = 0

        override fun getFieldOrder(): List<String> = listOf("x", "y")
    }

    @Structure.FieldOrder("hwnd", "message", "wParam", "lParam", "time", "pt", "lPrivate")
    class MSG : Structure() {
        @JvmField var hwnd: Pointer? = null
        @JvmField var message: Int = 0
        @JvmField var wParam: Pointer? = null
        @JvmField var lParam: Pointer? = null
        @JvmField var time: Int = 0
        @JvmField var pt: POINT = POINT()
        @JvmField var lPrivate: Int = 0
    }

    @Structure.FieldOrder(
        "cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage", "hIcon", "szTip",
        "dwState", "dwStateMask", "szInfo", "uTimeoutOrVersion", "szInfoTitle",
        "dwInfoFlags", "guidItem", "hBalloonIcon",
    )
    class NOTIFYICONDATAW : Structure() {
        @JvmField var cbSize: Int = 0
        @JvmField var hWnd: Pointer? = null
        @JvmField var uID: Int = 1
        @JvmField var uFlags: Int = 0
        @JvmField var uCallbackMessage: Int = 0
        @JvmField var hIcon: Pointer? = null
        @JvmField var szTip: CharArray = CharArray(128)
        @JvmField var dwState: Int = 0
        @JvmField var dwStateMask: Int = 0
        @JvmField var szInfo: CharArray = CharArray(256)
        @JvmField var uTimeoutOrVersion: Int = 0
        @JvmField var szInfoTitle: CharArray = CharArray(64)
        @JvmField var dwInfoFlags: Int = 0
        @JvmField var guidItem: ByteArray = ByteArray(16)
        @JvmField var hBalloonIcon: Pointer? = null

        init { cbSize = size() }
    }

    private val user32: User32 by lazy { Native.load("user32", User32::class.java, W32APIOptions.DEFAULT_OPTIONS) }
    private val kernel32: Kernel32 by lazy { Native.load("kernel32", Kernel32::class.java, W32APIOptions.DEFAULT_OPTIONS) }
    private val shell32: Shell32 by lazy { Native.load("shell32", Shell32::class.java, W32APIOptions.DEFAULT_OPTIONS) }

    private var callbacks: Callbacks? = null
    private var window: Pointer? = null
    private var trayThread: Thread? = null
    private var wndProc: WndProc? = null
    private var trayIcon: Pointer? = null

    private data class Callbacks(
        val onShow: () -> Unit,
        val onStart: () -> Unit,
        val onStop: () -> Unit,
        val onExit: () -> Unit,
    )

    fun install(
        onShow: () -> Unit,
        onStart: () -> Unit,
        onStop: () -> Unit,
        onExit: () -> Unit,
    ) {
        if (!DesktopPlatform.isWindows || trayThread != null) return
        callbacks = Callbacks(onShow, onStart, onStop, onExit)
        trayThread = Thread({
            try {
                runTrayLoop()
            } catch (t: Throwable) {
                log("tray thread failed: ${t::class.simpleName}: ${t.message}")
                t.stackTrace.take(8).forEach { log("  at $it") }
            }
        }, "mexpa-win32-tray").apply {
            isDaemon = true
            start()
        }
    }

    fun uninstall() {
        val hwnd = window
        if (hwnd != null) user32.PostMessageW(hwnd, WM_CLOSE, null, null)
        runCatching { trayThread?.join(1500) }
        trayThread = null
        window = null
        callbacks = null
    }

    fun notify(title: String, message: String) {
        val hwnd = window ?: return
        val data = newData(hwnd)
        data.uFlags = NIF_INFO
        data.dwInfoFlags = NIIF_INFO
        data.uTimeoutOrVersion = 5000
        copyWide(data.szInfo, message.take(255))
        copyWide(data.szInfoTitle, title.take(63))
        runCatching { shell32.Shell_NotifyIconW(NIM_MODIFY, data) }
    }

    private fun runTrayLoop() {
        val instance = kernel32.GetModuleHandleW(null) ?: return
        val callback = object : WndProc {
            override fun callback(hwnd: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Pointer? {
                return when (msg) {
                    TRAY_MESSAGE -> {
                        when (lParam.lowWord()) {
                            WM_RBUTTONUP -> showMenu(hwnd)
                            WM_LBUTTONUP, WM_LBUTTONDBLCLK -> dispatch(callbacks?.onShow)
                        }
                        Pointer(0)
                    }
                    WM_COMMAND -> {
                        when (wParam.lowWord()) {
                            1 -> dispatch(callbacks?.onShow)
                            2 -> dispatch(callbacks?.onStart)
                            3 -> dispatch(callbacks?.onStop)
                            4 -> dispatch(callbacks?.onExit)
                        }
                        Pointer(0)
                    }
                    WM_CLOSE -> {
                        user32.DestroyWindow(hwnd)
                        Pointer(0)
                    }
                    WM_DESTROY -> {
                        user32.PostQuitMessage(0)
                        Pointer(0)
                    }
                    else -> user32.DefWindowProcW(hwnd, msg, wParam, lParam)
                }
            }
        }
        wndProc = callback
        val className = WString("MexpaWin32TrayWindow")
        val windowClass = WNDCLASSEXW().apply {
            lpfnWndProc = callback
            hInstance = instance
            lpszClassName = className
        }
        if (user32.RegisterClassExW(windowClass).toInt() == 0) return
        val hwnd = user32.CreateWindowExW(
            0, className, WString("Mexpa"), 0,
            0, 0, 0, 0, null, null, instance, null,
        ) ?: return
        window = hwnd
        val icon = loadTrayIcon() ?: user32.LoadIconW(null, Pointer(32512))
        trayIcon = icon
        val data = newData(hwnd).apply {
            uFlags = NIF_MESSAGE or NIF_ICON or NIF_TIP
            uCallbackMessage = TRAY_MESSAGE
            hIcon = icon
            copyWide(szTip, "Mexpa")
        }
        if (!shell32.Shell_NotifyIconW(NIM_ADD, data)) {
            user32.DestroyWindow(hwnd)
            return
        }
        val msg = MSG()
        while (user32.GetMessageW(msg, null, 0, 0) > 0) {
            user32.TranslateMessage(msg)
            user32.DispatchMessageW(msg)
        }
        runCatching {
            val remove = newData(hwnd)
            shell32.Shell_NotifyIconW(NIM_DELETE, remove)
        }
        trayIcon?.let { user32.DestroyIcon(it) }
        trayIcon = null
        user32.UnregisterClassW(className, instance)
    }

    private fun showMenu(hwnd: Pointer?) {
        val menu = user32.CreatePopupMenu() ?: return
        try {
            user32.AppendMenuW(menu, MF_STRING, 1, WString("显示主窗口"))
            user32.AppendMenuW(menu, MF_STRING, 2, WString("启动代理"))
            user32.AppendMenuW(menu, MF_STRING, 3, WString("停止代理"))
            user32.AppendMenuW(menu, MF_SEPARATOR, 0, null)
            user32.AppendMenuW(menu, MF_STRING, 4, WString("退出"))
            val point = POINT()
            user32.GetCursorPos(point)
            bringToForeground(hwnd)
            val command = user32.TrackPopupMenuEx(
                menu,
                TPM_RETURNCMD or TPM_NONOTIFY or TPM_RIGHTBUTTON,
                point.x, point.y, hwnd, null,
            )
            user32.PostMessageW(hwnd, WM_NULL, null, null)
            when (command) {
                1 -> dispatch(callbacks?.onShow)
                2 -> dispatch(callbacks?.onStart)
                3 -> dispatch(callbacks?.onStop)
                4 -> dispatch(callbacks?.onExit)
            }
        } finally {
            user32.DestroyMenu(menu)
        }
    }

    private fun bringToForeground(hwnd: Pointer?) {
        if (hwnd == null) return
        user32.ShowWindow(hwnd, SW_SHOWNA)
        if (user32.SetForegroundWindow(hwnd)) return
        val foreground = user32.GetForegroundWindow() ?: return
        val foregroundThread = user32.GetWindowThreadProcessId(foreground, null)
        val currentThread = kernel32.GetCurrentThreadId()
        if (foregroundThread == 0 || currentThread == 0) return
        if (kernel32.AttachThreadInput(currentThread, foregroundThread, true)) {
            user32.SetForegroundWindow(hwnd)
            kernel32.AttachThreadInput(currentThread, foregroundThread, false)
        }
    }

    private fun log(message: String) {
        runCatching {
            File(DesktopPlatform.stateDir, "win32-tray.log").appendText("${System.currentTimeMillis()} $message\n")
        }
    }

    private fun newData(hwnd: Pointer): NOTIFYICONDATAW = NOTIFYICONDATAW().apply {
        this.hWnd = hwnd
        uID = 1
    }

    private fun loadTrayIcon(): Pointer? {
        val exe = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
            ?: return null
        if (!File(exe).isFile) return null
        val large = PointerByReference()
        val small = PointerByReference()
        val count = shell32.ExtractIconExW(WString(exe), 0, large, small, 1)
        if (count <= 0) return null
        small.value?.let { return it }
        return large.value
    }

    private fun dispatch(block: (() -> Unit)?) {
        if (block == null) return
        SwingUtilities.invokeLater(block)
    }

    private fun Pointer?.lowWord(): Int =
        if (this == null) 0 else (Pointer.nativeValue(this) and 0xffffL).toInt()

    private fun copyWide(target: CharArray, value: String) {
        val chars = value.toCharArray()
        val count = minOf(chars.size, target.size - 1)
        chars.copyInto(target, 0, 0, count)
        target[count] = '\u0000'
    }
}
