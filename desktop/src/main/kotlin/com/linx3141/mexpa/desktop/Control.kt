package com.linx3141.mexpa.desktop

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

/** 运行中实例的本地控制口（Unix domain socket），供 CLI/脚本调用。 */
object ControlServer {
    fun socketPath(): Path = DesktopPlatform.stateDir.toPath().resolve("control.sock")

    /** Windows 的 AF_UNIX 兼容性不稳定，控制口改用 loopback TCP；端口写在这个文件。 */
    fun portFile(): Path = DesktopPlatform.stateDir.toPath().resolve("control.port")

    /** Windows 没有 Unix socket 权限模型，控制口另加一次性 token，避免同机其他进程直接驱动开始/停止。 */
    fun tokenFile(): Path = DesktopPlatform.stateDir.toPath().resolve("control.token")

    fun start(handler: (String) -> String): ServerSocketChannel? {
        val path = socketPath()
        runCatching { Files.createDirectories(path.parent) }
        // 已有实例在监听时绝不删除/覆盖它的 socket：MEXPA_ALLOW_MULTI 调试实例曾把
        // 正常实例的 socket 路径换成一个无人监听的 inode，托盘菜单从此全部 connection refused。
        if (ControlClient.send("status") != null) return null
        val channel = if (DesktopPlatform.isWindows) {
            runCatching {
                ServerSocketChannel.open().apply {
                    bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0))
                }
            }.getOrNull() ?: return null
        } else {
            runCatching { Files.deleteIfExists(path) }
            runCatching {
                ServerSocketChannel.open(StandardProtocolFamily.UNIX).apply {
                    bind(UnixDomainSocketAddress.of(path))
                }
            }.getOrNull() ?: return null
        }
        val token = if (DesktopPlatform.isWindows) UUID.randomUUID().toString() else null
        if (DesktopPlatform.isWindows) {
            runCatching {
                Files.writeString(tokenFile(), token!!)
                Files.writeString(portFile(), (channel.localAddress as InetSocketAddress).port.toString())
            }
        } else {
            runCatching { Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")) }
        }
        Thread({
            while (channel.isOpen) {
                val client = runCatching { channel.accept() }.getOrNull() ?: continue
                runCatching {
                    client.use { socket ->
                        val reader = Channels.newInputStream(socket).bufferedReader()
                        val writer = Channels.newOutputStream(socket).bufferedWriter()
                        val line = if (token == null) {
                            reader.readLine().orEmpty().trim()
                        } else {
                            val supplied = reader.readLine().orEmpty().trim()
                            if (supplied != token) {
                                writer.write("error: unauthorized")
                                writer.newLine()
                                writer.flush()
                                return@runCatching
                            }
                            reader.readLine().orEmpty().trim()
                        }
                        val response = if (line.isEmpty()) "error: empty command" else handler(line)
                        writer.write(response)
                        writer.newLine()
                        writer.flush()
                    }
                }
            }
        }, "mexpa-control").apply { isDaemon = true }.start()
        return channel
    }

    fun stop(channel: ServerSocketChannel?) {
        if (channel == null) return
        runCatching { channel.close() }
        runCatching { Files.deleteIfExists(socketPath()) }
        runCatching { Files.deleteIfExists(portFile()) }
        runCatching { Files.deleteIfExists(tokenFile()) }
    }
}

object ControlClient {
    /** 返回 null 表示运行中的实例不可达。 */
    fun send(command: String): String? {
        if (DesktopPlatform.isWindows) {
            val port = runCatching { Files.readString(ControlServer.portFile()).trim().toInt() }.getOrNull()
                ?: return null
            val token = runCatching { Files.readString(ControlServer.tokenFile()).trim() }.getOrNull()
                ?.takeIf { it.isNotEmpty() } ?: return null
            return runCatching {
                SocketChannel.open().use { channel ->
                    channel.connect(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
                    val writer = Channels.newOutputStream(channel).bufferedWriter()
                    writer.write(token)
                    writer.newLine()
                    writer.write(command)
                    writer.newLine()
                    writer.flush()
                    Channels.newInputStream(channel).bufferedReader().readText().trim()
                }
            }.getOrNull()
        }
        val path = ControlServer.socketPath()
        if (!Files.exists(path)) return null
        return runCatching {
            SocketChannel.open(StandardProtocolFamily.UNIX).use { channel ->
                channel.connect(UnixDomainSocketAddress.of(path))
                val writer = Channels.newOutputStream(channel).bufferedWriter()
                writer.write(command)
                writer.newLine()
                writer.flush()
                Channels.newInputStream(channel).bufferedReader().readText().trim()
            }
        }.getOrNull()
    }
}
