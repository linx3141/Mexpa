package com.linx3141.mexpa.util

import kotlin.time.Clock
import kotlin.time.Instant

actual fun formatIsoTimeRelative(isoTime: String): String {
    if (isoTime.isBlank() || isoTime.startsWith("0001-")) return ""
    return try {
        val then = Instant.parse(isoTime)
        val minutes = (Clock.System.now() - then).inWholeMinutes.coerceAtLeast(0)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 24 * 60 -> "${minutes / 60} hr ago"
            else -> "${minutes / (24 * 60)} days ago"
        }
    } catch (_: Exception) {
        ""
    }
}
