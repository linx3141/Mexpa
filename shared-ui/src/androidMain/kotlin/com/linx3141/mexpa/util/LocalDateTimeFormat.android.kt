package com.linx3141.mexpa.util

import android.text.format.DateUtils
import kotlin.time.Instant

actual fun formatIsoTimeRelative(isoTime: String): String {
    if (isoTime.isBlank() || isoTime.startsWith("0001-")) return ""
    return try {
        DateUtils.getRelativeTimeSpanString(
            Instant.parse(isoTime).toEpochMilliseconds(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } catch (_: Exception) {
        ""
    }
}
