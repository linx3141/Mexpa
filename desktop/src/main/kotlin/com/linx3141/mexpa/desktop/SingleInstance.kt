package com.linx3141.mexpa.desktop

import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

/**
 * 单实例锁（`~/.local/state/mexpa/instance.lock`）。
 * 多开会各自拉起 mihomo 抢同一端口/TUN；第二个实例直接退出。
 * `MEXPA_ALLOW_MULTI=1` 用于开发/测试。
 */
object SingleInstance {
    fun tryAcquire(stateDir: File, allowMulti: Boolean = System.getenv("MEXPA_ALLOW_MULTI") == "1"): FileLock? {
        if (allowMulti) return null
        stateDir.mkdirs()
        val channel = FileChannel.open(
            File(stateDir, "instance.lock").toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        )
        val lock = runCatching { channel.tryLock() }.getOrNull()
        if (lock == null) channel.close()
        return lock
    }
}
