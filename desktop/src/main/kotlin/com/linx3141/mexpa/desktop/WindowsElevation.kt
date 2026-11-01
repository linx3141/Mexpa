package com.linx3141.mexpa.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.win32.StdCallLibrary
import java.io.File

/**
 * Windows TUN 需要管理员令牌。用 ShellExecuteW(..., "runas", ...) 直接拉起 UAC：
 * 不经过 PowerShell / java.exe console，避免多一个黑框日志窗口。
 */
object WindowsElevation {
    private interface Shell32 : StdCallLibrary {
        fun ShellExecuteW(
            hwnd: Pointer?,
            operation: WString?,
            file: WString?,
            parameters: WString?,
            directory: WString?,
            showCmd: Int,
        ): Pointer?
    }

    private val shell32 by lazy { Native.load("shell32", Shell32::class.java) }

    fun relaunchElevated(binaryPath: String): Boolean {
        if (!DesktopPlatform.isWindows) return false
        val jpackage = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
        val workingDir = File(System.getProperty("user.dir")).absolutePath
        val file: String
        val parameters: String
        if (jpackage != null) {
            file = jpackage
            parameters = quoteArg("--elevated-start")
        } else {
            // 便携包没有 jpackage 启动器：用 javaw.exe（不是 java.exe）重开，避免控制台窗口。
            val javaExe = File(System.getProperty("java.home").orEmpty(), "bin/javaw.exe")
            if (!javaExe.isFile) return false
            val classpath = System.getProperty("java.class.path").orEmpty()
            val transform = File(File(binaryPath).absoluteFile.parentFile, DesktopPlatform.transformBinaryName)
            file = javaExe.absolutePath
            parameters = buildList {
                add(quoteArg("-Dmexpa.binary=$binaryPath"))
                if (transform.isFile) add(quoteArg("-Dmexpa.transform=${transform.absolutePath}"))
                add("-cp")
                add(quoteArg(classpath))
                add("com.linx3141.mexpa.desktop.MainKt")
                add("--elevated-start")
            }.joinToString(" ")
        }
        return runCatching {
            val result = shell32.ShellExecuteW(
                null,
                WString("runas"),
                WString(file),
                WString(parameters),
                WString(workingDir),
                1, // SW_SHOWNORMAL, not SW_HIDE: the elevated Mexpa window should show
            )
            Pointer.nativeValue(result) > 32
        }.getOrDefault(false)
    }

    /** Windows command line argument quoting; paths/classpaths can contain spaces. */
    private fun quoteArg(value: String): String {
        if (value.isEmpty()) return "\"\""
        if (value.none { it == ' ' || it == '\t' || it == '"' }) return value
        val escaped = StringBuilder()
        var backslashes = 0
        for (ch in value) {
            when (ch) {
                '\\' -> { backslashes++; escaped.append(ch) }
                '"' -> {
                    repeat(backslashes * 2 + 1) { escaped.append('\\') }
                    escaped.append('"')
                    backslashes = 0
                }
                else -> { backslashes = 0; escaped.append(ch) }
            }
        }
        repeat(backslashes) { escaped.append('\\') }
        return "\"" + escaped + "\""
    }
}
