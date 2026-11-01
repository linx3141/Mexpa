package com.linx3141.mexpa.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.win32.StdCallLibrary

/**
 * Windows 原生托盘菜单：CreatePopupMenu + TrackPopupMenuEx。
 * 不走 AWT/Swing/Compose，菜单字体和样式由 Windows 自己绘制（用户要求的 Win32 原版）。
 */
object Win32TrayMenu {
    private const val MF_STRING = 0x00000000
    private const val MF_SEPARATOR = 0x00000800
    private const val TPM_RETURNCMD = 0x0100
    private const val TPM_NONOTIFY = 0x0080
    private const val WS_POPUP = 0x80000000.toInt()
    private const val WM_NULL = 0x0000

    private interface User32 : StdCallLibrary {
        fun CreatePopupMenu(): Pointer?
        fun DestroyMenu(menu: Pointer?): Boolean
        fun AppendMenuW(menu: Pointer?, flags: Int, id: Int, text: WString?): Boolean
        fun TrackPopupMenuEx(menu: Pointer?, flags: Int, x: Int, y: Int, owner: Pointer?, params: Pointer?): Int
        fun CreateWindowExW(
            exStyle: Int, className: WString?, windowName: WString?, style: Int,
            x: Int, y: Int, width: Int, height: Int,
            parent: Pointer?, menu: Pointer?, instance: Pointer?, param: Pointer?,
        ): Pointer?
        fun DestroyWindow(window: Pointer?): Boolean
        fun SetForegroundWindow(window: Pointer?): Boolean
        fun GetCursorPos(point: POINT): Boolean
        fun PostMessageW(window: Pointer?, msg: Int, wParam: Pointer?, lParam: Pointer?): Boolean
    }

    class POINT : Structure() {
        @JvmField var x: Int = 0
        @JvmField var y: Int = 0

        override fun getFieldOrder(): List<String> = listOf("x", "y")
    }

    private val user32 by lazy { Native.load("user32", User32::class.java) }

    fun show(
        onShow: () -> Unit,
        onStart: () -> Unit,
        onStop: () -> Unit,
        onExit: () -> Unit,
    ) {
        val hwnd = user32.CreateWindowExW(
            0, WString("STATIC"), WString(""), WS_POPUP,
            0, 0, 0, 0, null, null, null, null,
        ) ?: return
        val menu = user32.CreatePopupMenu() ?: run {
            user32.DestroyWindow(hwnd)
            return
        }
        try {
            user32.AppendMenuW(menu, MF_STRING, 1, WString("显示主窗口"))
            user32.AppendMenuW(menu, MF_STRING, 2, WString("启动代理"))
            user32.AppendMenuW(menu, MF_STRING, 3, WString("停止代理"))
            user32.AppendMenuW(menu, MF_SEPARATOR, 0, null)
            user32.AppendMenuW(menu, MF_STRING, 4, WString("退出"))
            val point = POINT()
            user32.GetCursorPos(point)
            user32.SetForegroundWindow(hwnd)
            val command = user32.TrackPopupMenuEx(
                menu,
                TPM_RETURNCMD or TPM_NONOTIFY,
                point.x, point.y, hwnd, null,
            )
            user32.PostMessageW(hwnd, WM_NULL, null, null)
            when (command) {
                1 -> onShow()
                2 -> onStart()
                3 -> onStop()
                4 -> onExit()
            }
        } finally {
            user32.DestroyMenu(menu)
            user32.DestroyWindow(hwnd)
        }
    }
}
