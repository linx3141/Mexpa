package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConfigurationOverride
import java.io.File
import kotlin.system.exitProcess

/**
 * P1 自检：设置持久化、订阅导入/校验/更新/active、运行时 override 生成、GeoIP 下载。
 * 环境变量：MEXPA_LINUX_DIAG_BIN（mihomo 路径）、MEXPA_LINUX_DIAG_GEO=1（真实下载 GeoIP）。
 */
object LinuxDiagnosticsP1 {
    fun run(): Boolean {
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
        val root = File("/tmp/mexpa-diag-p1").apply { deleteRecursively(); mkdirs() }

        kotlinx.coroutines.runBlocking {
        check("mihomo binary present: ${binary.path}") { binary.isFile }
        check("settings json roundtrip") {
            val store = LinuxSettingsStore(File(root, "settings.json"))
            val settings = LinuxSettings(
                systemProxy = true,
                tunEnabled = true,
                tunStack = "system",
                configPath = "/tmp/a.yaml",
                network = ConfigurationOverride(mixedPort = 17891, allowLan = true),
                theme = ThemeSnapshot(colorMode = 1, uiMode = "material", accentColor = "purple", animationSpeed = 1.5f),
                autoUpdateEnabled = false,
                updateViaProxy = true,
                windowX = 120,
                windowY = 80,
                windowWidth = 430,
                windowHeight = 940,
            )
            store.save(settings)
            store.load() == settings.normalized()
        }

        check("runtime override merged + mihomo -t") {
            val config = File(root, "config.yaml")
            config.writeText(
                """
                mixed-port: 17890
                mode: rule
                proxies: []
                proxy-groups:
                  - name: P
                    type: select
                    proxies: [DIRECT]
                rules:
                  - MATCH,P
                """.trimIndent(),
            )
            val settings = LinuxSettings(network = ConfigurationOverride(mixedPort = 17891, allowLan = true))
            val override = LinuxRuntimeConfig.writeOverrideFile(settings.network, settings, config, binary, root)
            if (override == null) return@check false
            val result = LinuxTun.run(binary.path, "-t", "-f", config.path, "--override-json", override.path)
            println("  mihomo -t exit=${result.exitCode} ${result.output.lineSequence().lastOrNull().orEmpty()}")
            result.ok
        }

        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        val validConfig = """
            mixed-port: 17890
            mode: rule
            proxies: []
            proxy-groups:
              - name: P
                type: select
                proxies: [DIRECT]
            rules:
              - MATCH,P
        """.trimIndent().toByteArray()
        var servedConfig = validConfig
        server.createContext("/config.yaml") { exchange ->
            val body = servedConfig
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val providerBody = """
            proxies:
              - {name: pf-node, type: socks5, server: 127.0.0.1, port: 1080}
        """.trimIndent().toByteArray()
        server.createContext("/provider.yaml") { exchange ->
            exchange.sendResponseHeaders(200, providerBody.size.toLong())
            exchange.responseBody.use { it.write(providerBody) }
        }
        val providerConfig = """
            mixed-port: 17890
            proxy-providers:
              test:
                type: http
                url: http://127.0.0.1:PORT/provider.yaml
                path: ./providers/test.yaml
                interval: 3600
            proxies: []
            proxy-groups:
              - {name: P, type: select, use: [test]}
            rules:
              - MATCH,P
        """.trimIndent().replace("PORT", server.address.port.toString()).toByteArray()
        server.createContext("/provider-config.yaml") { exchange ->
            exchange.sendResponseHeaders(200, providerConfig.size.toLong())
            exchange.responseBody.use { it.write(providerConfig) }
        }
        server.createContext("/bad.yaml") { exchange ->
            val body = "this: [is, not, valid".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            run {
                val repo = SubscriptionRepository(
                    baseDir = File(root, "data"),
                    binaryPath = { binary.path },
                    scope = this,
                )
                var activeConfigUpdates = 0
                repo.onActiveConfigUpdated = { activeConfigUpdates++ }
                repo.load()
                suspend fun waitIdle(timeoutMs: Long = 20_000) {
                    val deadline = System.currentTimeMillis() + timeoutMs
                    kotlinx.coroutines.delay(200)
                    while (System.currentTimeMillis() < deadline) {
                        val state = repo.state.value
                        if (!state.isLoading && state.importProgress == null && state.updateAll == null) return
                        kotlinx.coroutines.delay(50)
                    }
                }
                check("age key generation (bech32)") {
            // BIP173 测试向量：encode("a", empty) = a12uel5l
            val vector = AgeKey.bech32Encode("a", ByteArray(0))
            println("  bech32 vector=$vector")
            val pair = AgeKey.generate()
            println("  age secret prefix=${pair.secretKey.take(20)} public prefix=${pair.publicKey.take(8)}")
            vector == "a12uel5l" &&
                pair.secretKey.startsWith("AGE-SECRET-KEY-1") &&
                pair.publicKey.startsWith("age1") &&
                pair.secretKey.length == 74 &&
                AgeKey.generate().secretKey != pair.secretKey
        }
        check("hybrid age key via mihomo age keygen-pq") {
            val pair = AgeKey.hybrid(binary.path)
            println("  hybrid secret=${pair?.secretKey?.take(24)} public=${pair?.publicKey?.take(12)}")
            pair != null && pair.secretKey.startsWith("AGE-SECRET-KEY-PQ-1") && pair.publicKey.startsWith("age1pq1")
        }
        check("per-app uid + dns-hijack in override json") {
            val config = File(root, "perapp-config.yaml").apply {
                writeText(
                    """
                    mixed-port: 17890
                    proxies: []
                    proxy-groups:
                      - {name: P, type: select, proxies: [DIRECT]}
                    rules:
                      - MATCH,P
                    """.trimIndent(),
                )
            }
            val settings = LinuxSettings(
                tunEnabled = true,
                tunStack = "gvisor",
                dnsTakeover = true,
                perAppMode = "include",
                perAppUids = listOf(1000, 1001),
            )
            val text = LinuxRuntimeConfig.build(null, settings, config, binary) ?: ""
            println("  override=${text.replace("\n", " ").take(320)}")
            val hasUid = "\"include-uid\"" in text && "1000" in text && "1001" in text
            val hasDnsHijack = "dns-hijack" in text && "any:53" in text
            val hasRouteExclude = "route-exclude-address" in text && "192.168.0.0/16" in text
            val withoutDns = LinuxRuntimeConfig.build(null, settings.copy(dnsTakeover = false), config, binary) ?: ""
            hasUid && hasDnsHijack && hasRouteExclude && "dns-hijack" !in withoutDns
        }
        check("subscription import (url + mihomo -t)") {
                    repo.importUrl("测试订阅", "http://127.0.0.1:${server.address.port}/config.yaml", 0, "", "")
                    waitIdle()
                    val state = repo.state.value
                    println("  subscriptions=${state.subscriptions.map { it.name to it.isActive }} error='${state.error}'")
                    state.subscriptions.size == 1 && state.error.isBlank()
                }
                check("invalid subscription rejected") {
                    repo.importUrl("坏配置", "http://127.0.0.1:${server.address.port}/bad.yaml", 0, "", "")
                    waitIdle()
                    val state = repo.state.value
                    println("  after bad: size=${state.subscriptions.size} error='${state.error}'")
                    state.subscriptions.size == 1 && state.error.isNotBlank()
                }
                val id = repo.state.value.subscriptions.first().id
                check("subscription update") {
                    repo.update(id)
                    waitIdle()
                    repo.state.value.error.isBlank() && repo.state.value.subscriptions.size == 1
                }
                check("active config update callback") {
                    repo.update(id)
                    waitIdle()
                    println("  activeConfigUpdates=$activeConfigUpdates")
                    activeConfigUpdates >= 2
                }
                check("import with proxy-providers prefetch") {
                    repo.importUrl("带 provider", "http://127.0.0.1:${server.address.port}/provider-config.yaml", 0, "", "")
                    waitIdle()
                    val providerCache = File(root, "data/validate/providers/test.yaml")
                    val state = repo.state.value
                    println("  provider import: size=${state.subscriptions.size} error='${state.error}' cache=${providerCache.isFile}")
                    state.subscriptions.any { it.name == "带 provider" } && state.error.isBlank() && providerCache.isFile
                }
                check("failed update keeps old config (rollback)") {
                    servedConfig = "not: [valid".toByteArray()
                    repo.update(id)
                    waitIdle()
                    val stillOk = repo.subscriptions().firstOrNull { it.id == id }?.path
                        ?.let { File(it).readText().contains("mixed-port: 17890") } == true
                    val failed = repo.state.value.error.isNotBlank()
                    servedConfig = validConfig
                    println("  rollback: stillOk=$stillOk failed=$failed")
                    stillOk && failed
                }
                check("subscription metadata edit") {
                    repo.updateMetadata(id, "改名订阅", "", 30, "UA-Test", "")
                    waitIdle()
                    repo.state.value.subscriptions.first().name == "改名订阅"
                }
                check("active persistence") {
                    repo.setActive(id)
                    val reloaded = SubscriptionRepository(
                        baseDir = File(root, "data"),
                        binaryPath = { binary.path },
                        scope = this,
                    ).also { it.load() }
                    reloaded.state.value.subscriptions.firstOrNull { it.id == id }?.isActive == true
                }
                check("subscription delete") {
                    repo.delete(id)
                    waitIdle()
                    repo.subscriptions().none { it.id == id }
                }
                check("local file import") {
                    val localConfig = File(root, "local-config.yaml")
                    localConfig.writeText(validConfig.toString(Charsets.UTF_8))
                    repo.importFile(localConfig)
                    waitIdle()
                    val imported = repo.state.value.subscriptions.firstOrNull { it.name == "local-config" }
                    println("  local file import: ${imported?.name} error='${repo.state.value.error}'")
                    imported != null && repo.state.value.error.isBlank()
                }
                check("overrides store + transform (yaml + js)") {
            val base = File(root, "ovr-base.yaml").apply {
                writeText(
                    """
                    mixed-port: 17890
                    mode: rule
                    log-level: info
                    proxies: []
                    proxy-groups:
                      - {name: P, type: select, proxies: [DIRECT]}
                    rules:
                      - MATCH,P
                    """.trimIndent(),
                )
            }
            val store = OverridesStore(File(root, "overrides"), kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
            store.load()
            val yamlEntry = store.addBlank("yaml-test", "yaml")
            store.writeContent(yamlEntry.id, "mixed-port: 17999\nmode: global\n")
            val jsEntry = store.addBlank("js-test", "js")
            store.writeContent(jsEntry.id, "function main(c) { c[\"log-level\"] = \"warning\"; return c; }")
            val reloaded = OverridesStore(File(root, "overrides"), kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
            reloaded.load()
            val helper = OverrideRuntime.resolveHelper(binary.path)
            println("  transform helper=${helper?.path}")
            val output = File(root, "ovr-out.yaml")
            val result = OverrideRuntime.apply(
                base = base,
                overrideIds = listOf(yamlEntry.id, jsEntry.id),
                store = reloaded,
                helper = helper,
                output = output,
            )
            println("  transform result file=${result.file?.path} error='${result.error}'")
            val text = output.takeIf { it.isFile }?.readText().orEmpty()
            val transformed = "17999" in text && "global" in text && "log-level: warning" in text
            if (transformed) {
                val validate = LinuxTun.run(
                    binary.path, "-t", "-f", output.path, "-d", File(root, "ovr-validate").path,
                )
                println("  transformed mihomo -t exit=${validate.exitCode}")
                transformed && validate.ok && reloaded.profiles.value.size == 2
            } else {
                false
            }
        }
        check("age key is passed to mihomo runtime command") {
            val command = buildMihomoCommand(
                binaryPath = "/bin/mihomo",
                workDirPath = "/tmp/work",
                configPath = "/tmp/config.yaml",
                endpoint = "127.0.0.1:9090",
                secret = "s",
                ageSecretKey = "AGE-SECRET-KEY-TEST",
                overrideJsonPath = null,
            )
            println("  command=${command.joinToString(" ")}")
            val index = command.indexOf("--age-secret-key")
            index >= 0 && command.getOrNull(index + 1) == "AGE-SECRET-KEY-TEST"
        }
        check("selections store roundtrip") {
                    val store = SelectionsStore(File(root, "selections.json"))
                    store.put("sub-1", "PROXY", "节点A")
                    store.put("sub-1", "AUTO", "节点B")
                    store.put("sub-2", "PROXY", "节点C")
                    val reloaded = SelectionsStore(File(root, "selections.json"))
                    reloaded.get("sub-1") == mapOf("PROXY" to "节点A", "AUTO" to "节点B") &&
                        reloaded.get("sub-2") == mapOf("PROXY" to "节点C")
                }
            }
        } finally {
            server.stop(0)
        }

        if (System.getenv("MEXPA_LINUX_DIAG_GEO") == "1") {
            run {
                // 用本地 HTTP 服务验证下载/落盘/体积校验逻辑；真实 GitHub 下载由 UI「GeoIP 数据」按钮触发
                val payload = ByteArray(600 * 1024) { index -> (index % 251).toByte() }
                val geoServer = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
                listOf("/geoip.metadb", "/geosite.dat", "/ASN.mmdb").forEach { path ->
                    geoServer.createContext(path) { exchange ->
                        exchange.sendResponseHeaders(200, payload.size.toLong())
                        exchange.responseBody.use { it.write(payload) }
                    }
                }
                geoServer.start()
                try {
                    val geoDir = File(root, "geo").apply { mkdirs() }
                    val base = "http://127.0.0.1:${geoServer.address.port}"
                    val geo = GeoDataManager(
                        workDir = geoDir,
                        scope = this@runBlocking,
                        sources = listOf(
                            "$base/geoip.metadb" to "geoip.metadb",
                            "$base/geosite.dat" to "geosite.dat",
                            "$base/ASN.mmdb" to "ASN.mmdb",
                        ),
                    )
                    geo.download()
                    kotlinx.coroutines.delay(500)
                    var waited = 0L
                    while (waited < 30_000 && geo.state.value.downloading) {
                        kotlinx.coroutines.delay(200)
                        waited += 200
                    }
                    val state = geo.state.value
                    println("  geo: ${state.message} error='${state.error}'")
                    val files = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb").map { File(geoDir, it) }
                    check("GeoIP download") {
                        !state.downloading && state.error.isBlank() &&
                            files.all { it.isFile && it.length() == payload.size.toLong() }
                    }
                } finally {
                    geoServer.stop(0)
                }
            }
        }

        val subsFile = System.getenv("MEXPA_LINUX_DIAG_SUBS")?.let(::File)
        if (subsFile != null && subsFile.isFile) {
            runRealSubscriptions(subsFile, root) { name, ok -> if (ok) println("PASS $name") else failures++ }
        }

        val latencyConfig = System.getenv("MEXPA_LINUX_DIAG_LATENCY")?.takeIf { it == "1" }
            ?.let { System.getenv("MEXPA_LINUX_DIAG_PROXY_CONFIG") }?.let(::File)?.takeIf { it.isFile }
        if (latencyConfig != null) {
            run {
                val dir = File(root, "latency-proxy").apply { deleteRecursively(); mkdirs() }
                val override = File(dir, "override.json")
                override.writeText(
                    """{"port":17880,"socks-port":17881,"mixed-port":0,"allow-lan":false,"external-controller":"127.0.0.1:19080","mode":"rule"}""",
                )
                val proxyProcess = ProcessBuilder(
                    binary.path, "-d", dir.path, "-f", latencyConfig.path, "--override-json", override.path,
                ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(File(dir, "mihomo.log"))).start()
                try {
                    val data = MihomoDataSource(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO))
                    val ready = data.start(latencyConfig, 17880, "127.0.0.1:19080", "")
                    println("  latency data ready=$ready")
                    if (ready) {
                        kotlinx.coroutines.delay(2_500)
                        val mem = data.memory.value
                        println("  core memory=${mem.ramUsage} / ${mem.ramTotal}")
                        check("memory card shows mihomo core memory") {
                            mem.ramUsage != "-- MB" && mem.ramUsage.isNotBlank()
                        }
                        data.testLatency()
                        var waited = 0L
                        while (waited < 40_000 && data.latency.value.testing) {
                            kotlinx.coroutines.delay(200); waited += 200
                        }
                        val state = data.latency.value
                        println("  latency baidu=${state.baidu} cloudflare=${state.cloudflare} google=${state.google} viaRules=${state.viaRules}")
                        check("rule-based latency probes") {
                            !state.testing && state.baidu > 0 && (state.cloudflare > 0 || state.google > 0)
                        }
                    } else {
                        check("rule-based latency probes") { false }
                    }
                    data.stop()
                } finally {
                    proxyProcess.destroy()
                    proxyProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }

        }

        println(if (failures == 0) "LINUX P1 DIAG OK" else "LINUX P1 DIAG FAILED ($failures)")
        exitProcess(if (failures == 0) 0 else 1)
    }

    /**
     * 真实订阅列表验证：MEXPA_LINUX_DIAG_SUBS=/path/airports.txt（每行一个 URL）。
     * 可选 MEXPA_LINUX_DIAG_PROXY_CONFIG=<可用 config.yaml>：先起一个本地 mihomo 代理，
     * 订阅下载与校验经 mixed-port 走代理（与 App 里启动代理后的行为一致）。
     */
    private suspend fun runRealSubscriptions(
        urlsFile: File,
        root: File,
        report: (String, Boolean) -> Unit,
    ) {
        val urls = urlsFile.readLines().mapNotNull { line ->
            line.split(Regex("\\s+")).firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        }
        if (urls.isEmpty()) {
            report("real subscriptions file has no URLs", false)
            return
        }
        val binary = System.getenv("MEXPA_LINUX_DIAG_BIN")?.let(::File)
            ?: File(System.getProperty("user.dir")).resolve("../mihomo-linux")
        val proxyConfig = System.getenv("MEXPA_LINUX_DIAG_PROXY_CONFIG")?.let(::File)?.takeIf { it.isFile }
        var proxyProcess: Process? = null
        var proxyPort: Int? = null
        if (proxyConfig != null) {
            val dir = File(root, "real-proxy").apply { deleteRecursively(); mkdirs() }
            val override = File(dir, "override.json")
            override.writeText(
                """{"port":17880,"socks-port":17881,"mixed-port":0,"allow-lan":false,"external-controller":"127.0.0.1:19080","mode":"rule"}""",
            )
            proxyProcess = runCatching {
                ProcessBuilder(
                    binary.path, "-d", dir.path, "-f", proxyConfig.path, "--override-json", override.path,
                ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(File(dir, "mihomo.log"))).start()
            }.getOrNull()
            val api = MihomoApi("127.0.0.1:19080", "", kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
            for (attempt in 0 until 60) {
                if (api.version() != null) {
                    proxyPort = 17880
                    break
                }
                kotlinx.coroutines.delay(250)
            }
            api.close()
            println("  proxy for subscription test: port=$proxyPort")
        }
        try {
            val repo = SubscriptionRepository(
                baseDir = File(root, "real-subs"),
                binaryPath = { binary.path },
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO),
                mixedPortProvider = { proxyPort },
                viaProxyProvider = { true },
            )
            repo.load()
            suspend fun waitIdle(timeoutMs: Long = 90_000) {
                val deadline = System.currentTimeMillis() + timeoutMs
                kotlinx.coroutines.delay(200)
                while (System.currentTimeMillis() < deadline) {
                    val state = repo.state.value
                    if (!state.isLoading && state.importProgress == null && state.updateAll == null) return
                    kotlinx.coroutines.delay(100)
                }
            }
            urls.forEachIndexed { index, url ->
                val expected = repo.state.value.subscriptions.size + 1
                val startedAt = System.currentTimeMillis()
                repo.clearError()
                repo.importUrl("sub${index + 1}", url, 0, "", "")
                kotlinx.coroutines.delay(500)
                val deadline = System.currentTimeMillis() + 120_000
                var settled = false
                while (System.currentTimeMillis() < deadline) {
                    val state = repo.state.value
                    val finished = !state.isLoading && state.importProgress == null
                    if (finished && (state.subscriptions.size >= expected || state.error.isNotBlank())) {
                        settled = true
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }
                val state = repo.state.value
                println("  #${index + 1} took=${System.currentTimeMillis() - startedAt}ms settled=$settled")
                val imported = state.subscriptions.size == expected && state.error.isBlank()
                val label = "subscription #${index + 1} import"
                println(
                    "  #${index + 1}: imported=$imported size=${state.subscriptions.size} " +
                        "error='${state.error.take(160)}'",
                )
                report(label, imported)
                if (!imported) repo.clearError()
            }
            report("all subscriptions imported", repo.state.value.subscriptions.size == urls.size)
        } finally {
            proxyProcess?.destroy()
            proxyProcess?.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
