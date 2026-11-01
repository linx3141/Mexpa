package com.linx3141.mexpa.desktop

import java.io.File
import kotlin.system.exitProcess

/** P2 自检：单实例锁、并发冲突检测、本地备份恢复、自启可用性。 */
object LinuxDiagnosticsP2 {
    fun run(): Boolean {
        var failures = 0
        fun check(name: String, block: () -> Boolean) {
            val ok = runCatching(block).getOrElse {
                println("FAIL $name: ${it.message ?: it::class.simpleName}")
                false
            }
            if (ok) println("PASS $name") else failures++
        }
        val root = File("/tmp/mexpa-diag-p2").apply { deleteRecursively(); mkdirs() }

        check("single instance lock") {
            val stateDir = File(root, "lock").apply { mkdirs() }
            val first = SingleInstance.tryAcquire(stateDir, allowMulti = false)
            val second = SingleInstance.tryAcquire(stateDir, allowMulti = false)
            println("  first=${first != null} second=${second != null}")
            first != null && second == null
        }

        check("conflict detector") {
            val info = ConflictDetector.detect(17890, ourProxyRunning = false)
            println("  conflict=$info")
            true
        }

        check("backup export/restore roundtrip") {
            val configDir = File(root, "config").apply { mkdirs() }
            val stateDir = File(root, "state").apply { mkdirs() }
            File(configDir, "settings.json").writeText("""{"systemProxy":true}""")
            File(configDir, "config.yaml").writeText("mixed-port: 7890")
            File(stateDir, "subscriptions.json").writeText("""{"subscriptions":[]}""")
            val subDir = File(stateDir, "subscriptions/abc").apply { mkdirs() }
            File(subDir, "config.yaml").writeText("mixed-port: 17890")
            val archive = File(root, "backup.zip")
            val subConfig = File(stateDir, "subscriptions/abc/config.yaml")
            val exported = LinuxBackup.export(
                archive,
                configDir,
                stateDir,
                listOf(
                    LocalSubscription(
                        id = "abc",
                        name = "测试订阅",
                        url = "https://example.com/sub",
                        path = subConfig.path,
                        active = true,
                    ),
                ),
            )
            println("  export: ${exported.message}")
            if (!exported.ok) return@check false
            // 破坏现场后恢复
            File(configDir, "settings.json").writeText("{}")
            File(subDir, "config.yaml").delete()
            val restored = LinuxBackup.restore(archive, configDir, stateDir)
            println("  restore: ${restored.message}")
            restored.ok &&
                File(configDir, "settings.json").readText().contains("systemProxy") &&
                File(configDir, "config.yaml").readText().contains("7890") &&
                File(stateDir, "subscriptions/abc/config.yaml").readText().contains("17890")
        }

        check("android backup import (backup.json + files/)") {
            val src = File(root, "android-backup.zip")
            val cfg = "mixed-port: 17890\nproxies: []\nproxy-groups:\n  - {name: P, type: select, proxies: [DIRECT]}\nrules:\n  - MATCH,P\n"
            java.util.zip.ZipOutputStream(src.outputStream()).use { zip ->
                fun put(name: String, text: String) {
                    zip.putNextEntry(java.util.zip.ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
                }
                put(
                    "backup.json",
                    """{"version":1,"createdAt":1,"imported":[{"uuid":"11111111-1111-1111-1111-111111111111","name":"安卓订阅","type":"Url","source":"https://example.com/sub","interval":1800000,"createdAt":1}],"selections":[{"uuid":"11111111-1111-1111-1111-111111111111","proxy":"PROXY","selected":"节点A"}],"stringPrefs":{"active_profile_uuid":"11111111-1111-1111-1111-111111111111","active_profile_name":"安卓订阅"},"stringSetPrefs":{},"bootStartEnabled":false}""",
                )
                put("files/imported/11111111-1111-1111-1111-111111111111/config.yaml", cfg)
                put("files/overrides/overrides_list.json", """{"overrides":[{"id":"ovr1","name":"测试覆写","sourceType":"Local","format":"Yaml","sourceLocation":"","createdAt":1}]}""")
                put("files/overrides/ovr1.yaml", "mode: global\n")
                put("files/override.user.json", """{"mixed-port":12345,"allow-lan":true}""")
            }
            val cfgDir = File(root, "android-restore-config").apply { mkdirs() }
            val stateDir2 = File(root, "android-restore-state").apply { mkdirs() }
            val restored = LinuxBackup.restore(src, cfgDir, stateDir2)
            val subsText = File(stateDir2, "subscriptions.json").readText()
            val overrideIndex = File(cfgDir, "overrides/overrides.json").readText()
            val settingsText = File(cfgDir, "settings.json").readText()
            val selectionsText = File(stateDir2, "selections.json").readText()
            println("  android restore: ok=${restored.ok} subs=${subsText.contains("安卓订阅")} override=${overrideIndex.contains("测试覆写")} network=${settingsText.contains("12345")} selections=${selectionsText.contains("节点A")}")
            restored.ok &&
                subsText.contains("安卓订阅") && subsText.contains("\"active\":true") &&
                File(stateDir2, "subscriptions/11111111-1111-1111-1111-111111111111/config.yaml").isFile &&
                overrideIndex.contains("测试覆写") && File(cfgDir, "overrides/ovr1.yaml").readText().contains("global") &&
                settingsText.contains("12345") && selectionsText.contains("节点A")
        }

        check("backup rejects zip-slip") {
            val archive = File(root, "evil.zip")
            java.util.zip.ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("../escape.txt"))
                zip.write("evil".toByteArray())
                zip.closeEntry()
            }
            val configDir = File(root, "evil-config").apply { mkdirs() }
            val stateDir = File(root, "evil-state").apply { mkdirs() }
            val result = LinuxBackup.restore(archive, configDir, stateDir)
            println("  restore evil: ${result.message}")
            !result.ok && !File(root, "escape.txt").exists()
        }

        check("webdav put/get against local dav server") {
            val store = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
            val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/dav/") { exchange ->
                val name = exchange.requestURI.path.removePrefix("/dav/")
                when (exchange.requestMethod.uppercase()) {
                    "MKCOL" -> exchange.sendResponseHeaders(201, -1)
                    "PUT" -> {
                        val body = exchange.requestBody.readBytes()
                        store[name] = body
                        exchange.sendResponseHeaders(201, -1)
                    }
                    "GET" -> {
                        val body = store[name]
                        if (body == null) {
                            exchange.sendResponseHeaders(404, -1)
                        } else {
                            exchange.sendResponseHeaders(200, body.size.toLong())
                            exchange.responseBody.use { it.write(body) }
                        }
                    }
                    else -> exchange.sendResponseHeaders(405, -1)
                }
                exchange.close()
            }
            server.start()
            try {
                kotlinx.coroutines.runBlocking {
                    val client = WebDavClient("http://127.0.0.1:${server.address.port}/dav")
                    val test = client.test()
                    val put = client.put("backup.zip", "hello".toByteArray())
                    val get = client.get("backup.zip")
                    println("  webdav test='${test.message}' put='${put.message}' get='${get.message}'")
                    test.ok && put.ok && get.ok && get.bytes?.toString(Charsets.UTF_8) == "hello"
                }
            } finally {
                server.stop(0)
            }
        }

        check("tray install/uninstall") {
            if (!java.awt.SystemTray.isSupported()) {
                println("  tray unsupported in this session")
                return@check false
            }
            val frame = javax.swing.JFrame("mexpa-tray-diag").apply {
                isUndecorated = true
                setSize(1, 1)
                isVisible = true
            }
            try {
                val icon = LinuxTray.install(frame, {}, {}, {}, {})
                val added = icon != null && java.awt.SystemTray.getSystemTray().trayIcons.any { it === icon }
                LinuxTray.uninstall(icon)
                println("  tray added=$added")
                added
            } finally {
                frame.dispose()
            }
        }

        check("autostart availability (dev build)") {
            println("  available=${Autostart.available()} enabled=${Autostart.isEnabled()}")
            true
        }

        println(if (failures == 0) "LINUX P2 DIAG OK" else "LINUX P2 DIAG FAILED ($failures)")
        exitProcess(if (failures == 0) 0 else 1)
    }
}
