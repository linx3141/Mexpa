package com.linx3141.mexpa.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** per 订阅的代理组选择记录（对齐 Android 的 selections 表）。 */
class SelectionsStore(private val file: File) {
    @Serializable
    private data class Entry(val subscriptionId: String, val group: String, val node: String)

    @Serializable
    private data class Snapshot(val selections: List<Entry> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var cache: MutableMap<String, MutableMap<String, String>>? = null

    private fun loaded(): MutableMap<String, MutableMap<String, String>> = cache ?: run {
        val map = mutableMapOf<String, MutableMap<String, String>>()
        runCatching {
            val snapshot = json.decodeFromString<Snapshot>(file.readText())
            snapshot.selections.forEach { entry ->
                map.getOrPut(entry.subscriptionId) { mutableMapOf() }[entry.group] = entry.node
            }
        }
        map.also { cache = it }
    }

    private fun persist() {
        val entries = loaded().flatMap { (subId, groups) ->
            groups.map { (group, node) -> Entry(subId, group, node) }
        }
        val snapshot = Snapshot(selections = entries)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(Snapshot.serializer(), snapshot))
        }
    }

    fun get(subscriptionId: String): Map<String, String> = loaded()[subscriptionId].orEmpty().toMap()

    fun put(subscriptionId: String, group: String, node: String) {
        loaded().getOrPut(subscriptionId) { mutableMapOf() }[group] = node
        persist()
    }

    fun remove(subscriptionId: String, group: String) {
        loaded()[subscriptionId]?.remove(group)
        persist()
    }
}
