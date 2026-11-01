package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConfigurationOverride
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * 生成 mihomo 的 `--override-json`：把 Network 页覆写与 TUN 设置合并成一个 JSON。
 *
 * mihomo 只覆盖 JSON 里出现的字段，因此：
 * - Network 覆写里 null 的字段不输出（`explicitNulls = false`）；
 * - TUN 开启时强制 `tun` 段与 `dns.enable = true`（dns-hijack 依赖），并保留用户/订阅的 DNS nameserver；
 * - 订阅配置没有 `dns:` 段时补一份默认 nameserver，否则 TUN 模式下 DNS 解析没有上游。
 */
object LinuxRuntimeConfig {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        prettyPrint = true
    }
    private val ROOT_DNS = Regex("(?m)^dns\\s*:")
    private val DEFAULT_NAMESERVERS = listOf("223.5.5.5", "119.29.29.29", "8.8.8.8")

    /** 私网/保留段/多播不进 TUN，避免劫持 LAN/P2P（对齐 Android ROOT TUN 的 route-exclude-address）。 */
    private val DEFAULT_ROUTE_EXCLUDE = listOf(
        "0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10", "127.0.0.0/8", "169.254.0.0/16",
        "172.16.0.0/12", "192.0.0.0/24", "192.0.2.0/24", "192.168.0.0/16", "198.18.0.0/15",
        "198.51.100.0/24", "203.0.113.0/24", "224.0.0.0/4", "240.0.0.0/4", "255.255.255.255/32",
        "::1/128", "fc00::/7", "fe80::/10", "ff00::/8",
    )

    fun build(
        network: ConfigurationOverride?,
        settings: LinuxSettings,
        configFile: File,
        binary: File,
    ): String? {
        val hasNetwork = network != null && network != ConfigurationOverride()
        if (!settings.tunEnabled && !hasNetwork) return null
        val base = if (hasNetwork) {
            json.encodeToJsonElement(ConfigurationOverride.serializer(), network!!) as? JsonObject ?: JsonObject(emptyMap())
        } else {
            JsonObject(emptyMap())
        }
        val merged = base.toMutableMap()
        if (settings.tunEnabled) {
            val stack = LinuxTun.resolveStack(binary, settings.tunStack)
            merged["tun"] = buildJsonObject {
                put("enable", true)
                put("stack", stack)
                put("auto-route", true)
                put("auto-detect-interface", true)
                put("route-exclude-address", buildJsonArray {
                    DEFAULT_ROUTE_EXCLUDE.forEach { add(JsonPrimitive(it)) }
                })
                if (settings.dnsTakeover) {
                    put("dns-hijack", buildJsonArray { add(JsonPrimitive("any:53")) })
                }
                val uids = settings.perAppUids.filter { it >= 0 }.distinct()
                if (uids.isNotEmpty()) {
                    when (settings.perAppMode) {
                        "include" -> put("include-uid", buildJsonArray { uids.forEach { add(JsonPrimitive(it)) } })
                        "exclude" -> put("exclude-uid", buildJsonArray { uids.forEach { add(JsonPrimitive(it)) } })
                    }
                }
            }
            val dns = (merged["dns"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            dns["enable"] = JsonPrimitive(true)
            val hasDnsInConfig = runCatching { configFile.isFile && ROOT_DNS.containsMatchIn(configFile.readText()) }.getOrDefault(false)
            if (dns["nameserver"] == null && !hasDnsInConfig) {
                dns["nameserver"] = JsonArray(DEFAULT_NAMESERVERS.map { JsonPrimitive(it) })
            }
            merged["dns"] = JsonObject(dns)
        }
        return json.encodeToString(JsonObject.serializer(), JsonObject(merged))
    }

    /** 把生成的 override 写到 workDir/runtime/override.json；不需要覆写时返回 null 并清掉旧文件。 */
    fun writeOverrideFile(
        network: ConfigurationOverride?,
        settings: LinuxSettings,
        configFile: File,
        binary: File,
        workDir: File,
    ): File? {
        val target = File(workDir, "runtime/override.json")
        val content = build(network, settings, configFile, binary)
        if (content == null) {
            target.delete()
            return null
        }
        target.parentFile?.mkdirs()
        target.writeText(content)
        return target
    }
}
