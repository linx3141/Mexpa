package com.linx3141.mexpa.desktop

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 读取随包 mihomo 的静态版本。
 *
 * `/version` 只有核心运行时才存在，首页在「从未启动过」时没有可用的 homeInfo.version；
 * Android 用 BuildConfig.MIHOMO_VERSION 兜底，桌面端对应地跑一次 `mihomo -v`（结果缓存）。
 */
object MihomoCoreVersion {
    private val cache = ConcurrentHashMap<String, String>()

    fun read(binaryPath: String): String {
        if (binaryPath.isBlank()) return ""
        return cache.getOrPut(binaryPath) {
            runCatching {
                val process = ProcessBuilder(binaryPath, "-v")
                    .redirectErrorStream(true)
                    .start()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return@runCatching ""
                }
                val output = process.inputStream.bufferedReader().readText()
                Regex("""Mihomo(?:\s+Meta)?\s+(\S+)""")
                    .find(output)?.groupValues?.get(1).orEmpty()
            }.getOrDefault("")
        }
    }
}
