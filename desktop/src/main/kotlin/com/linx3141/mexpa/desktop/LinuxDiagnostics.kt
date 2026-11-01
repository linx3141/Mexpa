package com.linx3141.mexpa.desktop

import java.io.File
import kotlin.system.exitProcess

/**
 * 无 GUI 的 Linux 平台层自检：`MEXPA_LINUX_DIAG=1 ./gradlew :desktop:run`。
 *
 * 用来在不拉起窗口的情况下验证两件最容易回归的事：
 * 1. 系统代理后端探测、写入与按快照恢复（`MEXPA_LINUX_DIAG_PROXY=1` 才会真的改桌面设置）；
 * 2. TUN 的 override.json 能通过 mihomo `-t` 配置校验。
 * 环境变量：
 * - `MEXPA_LINUX_DIAG_BIN`：mihomo 二进制路径；
 * - `MEXPA_LINUX_DIAG_CONFIG`：用于生成/校验的 config.yaml；
 * - `MEXPA_LINUX_DIAG_STACK`：gvisor/system/mixed（默认取二进制支持的第一项）；
 * - `MEXPA_LINUX_DIAG_PROXY=1`：实际执行系统代理写入+恢复；
 * - `MEXPA_LINUX_DIAG_PORT`：系统代理测试端口（默认 17890）。
 */
object LinuxDiagnostics {
    fun runIfRequested(): Boolean {
        if (DesktopPlatform.isWindows) return false
        if (System.getenv("MEXPA_LINUX_DIAG") != "1") return false
        if (System.getenv("MEXPA_LINUX_DIAG_DATA") == "1") {
            return runDataDiagnostics()
        }
        if (System.getenv("MEXPA_LINUX_DIAG_P1") == "1") {
            return LinuxDiagnosticsP1.run()
        }
        if (System.getenv("MEXPA_LINUX_DIAG_P2") == "1") {
            return LinuxDiagnosticsP2.run()
        }
        var failures = 0
        fun check(name: String, block: () -> Boolean) {
            val ok = runCatching(block).getOrElse {
                println("FAIL $name: ${it.message ?: it::class.simpleName}")
                false
            }
            if (ok) println("PASS $name") else failures++
        }

        val home = File(System.getProperty("user.home"))
        val stateDir = File(home, ".local/state/mexpa")

        check("system proxy backend detection") {
            val proxy = LinuxSystemProxy(
                configDir = File(home, ".config"),
                snapshotFile = File(stateDir, "diag-system-proxy.snapshot"),
            )
            println("  backend: ${proxy.backendLabel()}")
            proxy.detectBackend() != null
        }

        val binary = System.getenv("MEXPA_LINUX_DIAG_BIN")?.let(::File)
            ?: File(System.getProperty("user.dir")).resolve("../mihomo-linux")
        check("mihomo binary present: ${binary.path}") { binary.isFile }

        check("TUN capability") {
            println("  capability: ${LinuxTun.capability(binary)}")
            true
        }

        val config = System.getenv("MEXPA_LINUX_DIAG_CONFIG")?.let(::File) ?: File("/tmp/mexpa-diag-config.yaml")
        val stack = System.getenv("MEXPA_LINUX_DIAG_STACK") ?: LinuxTun.supportedStacks(binary).firstOrNull() ?: "system"
        check("TUN override.json generation + mihomo -t") {
            if (!config.isFile) return@check false
            val override = File("/tmp/mexpa-diag-override.json")
            val content = LinuxRuntimeConfig.build(
                network = null,
                settings = LinuxSettings(tunEnabled = true, tunStack = stack),
                configFile = config,
                binary = binary,
            )
            if (content == null) return@check false
            override.writeText(content)
            println("  override: ${override.readText().trim().prependIndent("  ")}")
            val result = LinuxTun.run(
                binary.path, "-t", "-f", config.path, "--override-json", override.path,
            )
            println("  mihomo -t exit=${result.exitCode} ${result.output.lineSequence().lastOrNull() ?: ""}")
            result.ok
        }

        if (System.getenv("MEXPA_LINUX_DIAG_PROXY") == "1") {
            check("system proxy apply + restore") {
                val port = System.getenv("MEXPA_LINUX_DIAG_PORT")?.toIntOrNull() ?: 17890
                val proxy = LinuxSystemProxy(
                    configDir = File(home, ".config"),
                    snapshotFile = File(stateDir, "diag-system-proxy.snapshot"),
                )
                val applied = proxy.apply(port)
                println("  apply: ${applied.message}")
                val restored = proxy.restore()
                println("  restore: ${restored.message}")
                applied.ok && restored.ok
            }
        }

        println(if (failures == 0) "LINUX DIAG OK" else "LINUX DIAG FAILED ($failures)")
        exitProcess(if (failures == 0) 0 else 1)
    }

    /**
     * P0 数据链路自检：起一个真实 mihomo 子进程，走 MihomoApi + MihomoDataSource 全流程。
     * 环境变量：MEXPA_LINUX_DIAG_BIN / MEXPA_LINUX_DIAG_CONFIG（必须含 external-controller 与 mixed-port）。
     */
    private fun runDataDiagnostics(): Boolean {
        var failures = 0
        suspend fun check(name: String, block: suspend () -> Boolean) {
            val ok = runCatching { block() }.getOrElse {
                println("FAIL $name: ${it.message ?: it::class.simpleName}")
                false
            }
            if (ok) println("PASS $name") else failures++
        }

        val binary = System.getenv("MEXPA_LINUX_DIAG_BIN")?.let(::File)
            ?: File(System.getProperty("user.dir")).resolve("../mihomo-linux")
        val config = System.getenv("MEXPA_LINUX_DIAG_CONFIG")?.let(::File)
            ?: File("/tmp/mexpa-p0-diag-config.yaml")
        val workDir = File("/tmp/mexpa-diag-data").apply { deleteRecursively(); mkdirs() }
        val log = File(workDir, "mihomo.log")
        if (!binary.isFile || !config.isFile) {
            println("FAIL data diag prerequisites: binary=${binary.path} config=${config.path}")
            exitProcess(1)
        }
        val process = runCatching {
            ProcessBuilder(binary.path, "-d", workDir.path, "-f", config.path)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                .start()
        }.getOrNull()
        if (process == null) {
            println("FAIL mihomo start")
            exitProcess(1)
        }
        try {
            kotlinx.coroutines.runBlocking {
                val api = MihomoApi("127.0.0.1:9090", "", this)
                var ready = false
                for (attempt in 0 until 40) {
                    if (api.version() != null) {
                        ready = true
                        break
                    }
                    kotlinx.coroutines.delay(250)
                }
                check("controller /version ready") { ready }
                check("controller /configs") { api.configs()?.mixedPort == 17890 }
                check("controller /proxies") { api.proxies()?.isNotEmpty() == true }
                check("controller /providers/proxies") { api.providers() != null }
                val source = MihomoDataSource(this)
                check("data source start") { source.start(config, 17890, "127.0.0.1:9090", "") }
                kotlinx.coroutines.delay(3_500)
                check("proxy groups non-empty") {
                    val groups = source.proxyState.value.groups
                    println("  groups=${groups.map { it.name }}")
                    groups.any { it.name == "PROXY" }
                }
                check("home mode/version") {
                    val info = source.homeInfo.value
                    println("  mode=${info.mode} version=${info.version} subscription=${info.subscription}")
                    info.mode == "rule" && info.version.isNotBlank()
                }
                source.startLogStream()
                kotlinx.coroutines.delay(800)
                check("logs via WS") {
                    // 稳态没有日志：直接向 mixed-port 发一个必然被 PROXY 组代理到死端口的请求，触发 mihomo 的 warning 日志
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            java.net.Socket("127.0.0.1", 17890).use { socket ->
                                socket.soTimeout = 1_500
                                socket.getOutputStream().write(
                                    "GET http://example.com/ HTTP/1.1\r\nHost: example.com\r\nConnection: close\r\n\r\n".toByteArray(),
                                )
                                socket.getOutputStream().flush()
                                runCatching { socket.getInputStream().readBytes() }
                            }
                        }
                    }
                    kotlinx.coroutines.delay(1_500)
                    println("  logs=${source.logs.value.size}")
                    source.logs.value.isNotEmpty()
                }
                source.stopLogStream()
                source.startObservingConnections()
                kotlinx.coroutines.delay(1_500)
                check("connections REST + WS connected") {
                    api.connections() != null && source.connectionState.value.isConnected
                }
                source.stopObservingConnections()
                source.queryDns("example.com", "A")
                kotlinx.coroutines.delay(2_500)
                println("  dns answers=${source.dnsState.value.answers.size} error='${source.dnsState.value.error}'")
                check("system/memory sampled") {
                    println("  memory=${source.memory.value} system=${source.systemInfo.value}")
                    source.memory.value.ramTotal != "-- MB" && source.systemInfo.value.cpuUsage != "--%"
                }
                check("traffic WS frames") {
                    var frames = 0
                    val job = api.stream("/traffic") { frames++ }
                    kotlinx.coroutines.delay(3_000)
                    job.cancel()
                    println("  traffic frames=$frames")
                    frames > 0
                }
                source.stop()
                api.close()
            }
        } finally {
            process.destroy()
            if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly()
        }
        println(if (failures == 0) "LINUX DATA DIAG OK" else "LINUX DATA DIAG FAILED ($failures)")
        exitProcess(if (failures == 0) 0 else 1)
    }
}
