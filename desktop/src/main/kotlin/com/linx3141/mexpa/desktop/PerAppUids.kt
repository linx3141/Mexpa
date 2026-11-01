package com.linx3141.mexpa.desktop

import java.io.File

/** Linux 侧的「分应用」只能按 uid 过滤（mihomo TUN include-uid/exclude-uid）。 */
data class UnixUser(val uid: Int, val name: String, val processCount: Int)

object PerAppUids {
    fun users(): List<UnixUser> {
        val names = passwdNames()
        val counts = mutableMapOf<Int, Int>()
        File("/proc").listFiles().orEmpty().forEach { entry ->
            val pid = entry.name.toIntOrNull() ?: return@forEach
            val status = File(entry, "status")
            if (!status.isFile) return@forEach
            val uid = runCatching {
                status.useLines { lines ->
                    lines.firstOrNull { it.startsWith("Uid:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toIntOrNull()
                }
            }.getOrNull() ?: return@forEach
            counts[uid] = (counts[uid] ?: 0) + 1
        }
        return counts.map { (uid, count) ->
            UnixUser(uid = uid, name = names[uid] ?: "uid $uid", processCount = count)
        }.sortedWith(compareBy<UnixUser> { it.uid })
    }

    /** 当前桌面用户常见 uid（用于 UI 默认展示），其余进程也一并列出。 */
    fun currentUid(): Int = runCatching {
        ProcessHandle.current().pid().let { pid ->
            File("/proc/$pid/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("Uid:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toIntOrNull()
            }
        }
    }.getOrNull() ?: -1

    private fun passwdNames(): Map<Int, String> = runCatching {
        File("/etc/passwd").readLines().mapNotNull { line ->
            val parts = line.split(':')
            if (parts.size >= 3) parts[2].toIntOrNull()?.let { it to parts[0] } else null
        }.toMap()
    }.getOrDefault(emptyMap())
}
