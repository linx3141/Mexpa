package com.linx3141.mexpa.util

object FormatUtils {

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return if (value == value.toLong().toDouble()) {
            "${value.toLong()} ${units[unitIndex]}"
        } else {
            // 不依赖 Locale：避免默认 Locale 输出本地数字符号，与 ASCII 单位混排
            val tenths = kotlin.math.round(value * 10).toInt()
            "${tenths / 10}.${tenths % 10} ${units[unitIndex]}"
        }
    }

    fun formatLatency(delay: Int): String {
        return if (delay < 0) "-- ms" else "$delay ms"
    }
}
