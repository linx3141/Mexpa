package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConfigurationOverride
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 本地备份/恢复，格式与 Android `BackupManager` 双向兼容：
 * - `backup.json` = Android 的 BackupSnapshot（imported / selections / stringPrefs…）
 * - `files/imported/{uuid}/…` = 订阅配置目录
 * - `files/overrides/…` = Android 覆写目录（overrides_list.json + 内容文件）
 * - `files/override.user.json` = ConfigurationOverride
 * - `linux/settings.json` = Linux 专属设置（Android 忽略，Linux 恢复时还原）
 *
 * 旧版 Linux 备份（settings/ + state/ + config/）仍可恢复。
 */
object LinuxBackup {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val compactJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class BackupProfile(
        val uuid: String,
        val name: String,
        val type: String,
        val source: String,
        val userAgent: String = "",
        val ageSecretKey: String = "",
        val overrideIds: List<String> = emptyList(),
        val overrideSortPreference: List<String> = emptyList(),
        val interval: Long = 0,
        val upload: Long = 0,
        val download: Long = 0,
        val total: Long = 0,
        val expire: Long = 0,
        val createdAt: Long,
    )

    @Serializable
    private data class BackupSelection(val uuid: String, val proxy: String, val selected: String)

    @Serializable
    private data class BackupSnapshot(
        val version: Int,
        val createdAt: Long,
        val imported: List<BackupProfile> = emptyList(),
        val pending: List<BackupProfile> = emptyList(),
        val selections: List<BackupSelection> = emptyList(),
        val stringPrefs: Map<String, String> = emptyMap(),
        val stringSetPrefs: Map<String, List<String>> = emptyMap(),
        val bootStartEnabled: Boolean = false,
    )

    @Serializable
    private data class OverrideMeta(
        val id: String,
        val name: String,
        val sourceType: String,
        val format: String,
        val sourceLocation: String,
        val createdAt: Long,
        val lastUpdatedAt: Long? = null,
    )

    @Serializable
    private data class OverrideList(val overrides: List<OverrideMeta> = emptyList())

    @Serializable
    private data class LinuxOverrideEntry(
        val id: String,
        val name: String,
        val format: String,
        val sourceType: String = "local",
        val url: String = "",
        val updatedAt: Long = 0,
    )

    @Serializable
    private data class LinuxOverrideIndex(val profiles: List<LinuxOverrideEntry> = emptyList())

    @Serializable
    private data class LinuxSelectionsSnapshot(
        val selections: List<LinuxSelection> = emptyList(),
    )

    @Serializable
    private data class LinuxSelection(val subscriptionId: String, val group: String, val node: String)

    @Serializable
    private data class SubscriptionsFile(val subscriptions: List<LocalSubscription> = emptyList())

    data class Result(val ok: Boolean, val message: String)

    fun export(
        destination: File,
        configDir: File,
        stateDir: File,
        subscriptions: List<LocalSubscription>,
    ): Result = runCatching {
        destination.parentFile?.mkdirs()
        val settings = LinuxSettingsStore(File(configDir, "settings.json")).load()
        val overrides = readLinuxOverrides(configDir)
        val selections = readLinuxSelections(stateDir)
        val active = subscriptions.firstOrNull { it.active }
        val snapshot = BackupSnapshot(
            version = 1,
            createdAt = System.currentTimeMillis(),
            imported = subscriptions.map { sub ->
                BackupProfile(
                    uuid = sub.id,
                    name = sub.name,
                    type = if (sub.isUrl) "Url" else "File",
                    source = sub.url.ifBlank { sub.name },
                    userAgent = sub.userAgent,
                    ageSecretKey = sub.ageSecretKey,
                    overrideIds = sub.overrideIds,
                    interval = sub.intervalMinutes * 60_000,
                    createdAt = sub.createdAt,
                )
            },
            selections = selections.flatMap { (subId, groups) ->
                groups.map { (group, node) -> BackupSelection(subId, group, node) }
            },
            stringPrefs = buildMap {
                active?.let {
                    put("active_profile_uuid", it.id)
                    put("active_profile_name", it.name)
                }
            },
        )
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            putBytes(zip, "backup.json", compactJson.encodeToString(BackupSnapshot.serializer(), snapshot).toByteArray())
            subscriptions.forEach { sub ->
                val dir = File(sub.path).parentFile
                addTree(zip, "files/imported/${sub.id}", dir)
            }
            if (overrides.isNotEmpty()) {
                val metas = overrides.map { entry ->
                    OverrideMeta(
                        id = entry.id,
                        name = entry.name,
                        sourceType = if (entry.sourceType == "remote") "Remote" else "Local",
                        format = if (entry.format == "js") "JavaScript" else "Yaml",
                        sourceLocation = entry.url,
                        createdAt = entry.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        lastUpdatedAt = entry.updatedAt.takeIf { it > 0 },
                    )
                }
                putBytes(
                    zip,
                    "files/overrides/overrides_list.json",
                    compactJson.encodeToString(OverrideList.serializer(), OverrideList(metas)).toByteArray(),
                )
                overrides.forEach { entry ->
                    val file = File(configDir, "overrides/${entry.id}.${entry.format}")
                    if (file.isFile) addFile(zip, "files/overrides/${entry.id}.${entry.format}", file)
                }
            }
            val overrideJson = settings.network?.let {
                compactJson.encodeToString(ConfigurationOverride.serializer(), it)
            } ?: "{}"
            putBytes(zip, "files/override.user.json", overrideJson.toByteArray())
            val linuxSettings = File(configDir, "settings.json")
            if (linuxSettings.isFile) addFile(zip, "linux/settings.json", linuxSettings)
        }
        Result(true, "已导出（Android 兼容格式）：${destination.path}")
    }.getOrElse { Result(false, "导出失败：${it.message ?: it::class.simpleName}") }

    fun restore(source: File, configDir: File, stateDir: File): Result = runCatching {
        if (!source.isFile) return@runCatching Result(false, "备份文件不存在")
        val staging = File(stateDir, ".restore")
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            ZipInputStream(source.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val target = safeResolve(staging, entry.name)
                        ?: return@runCatching Result(false, "备份包含非法路径：${entry.name}")
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
            }
            val snapshotFile = File(staging, "backup.json")
            if (snapshotFile.isFile) {
                restoreAndroidFormat(staging, snapshotFile, configDir, stateDir)
            } else {
                restoreLegacyFormat(staging, configDir, stateDir)
            }
            Result(true, "已恢复，请重启应用生效")
        } finally {
            staging.deleteRecursively()
        }
    }.getOrElse { Result(false, "恢复失败：${it.message ?: it::class.simpleName}") }

    private fun restoreAndroidFormat(staging: File, snapshotFile: File, configDir: File, stateDir: File) {
        val snapshot = runCatching {
            compactJson.decodeFromString(BackupSnapshot.serializer(), snapshotFile.readText())
        }.getOrElse { error("backup.json 解析失败：${it.message}") }
        if (snapshot.version > 1) error("备份版本 ${snapshot.version} 高于当前支持版本 1")

        val imported = snapshot.imported.filter { it.uuid.isNotBlank() }
        val activeId = snapshot.stringPrefs["active_profile_uuid"]
            ?.takeIf { id -> imported.any { it.uuid == id } }
            ?: imported.firstOrNull()?.uuid
        val subs = imported.map { profile ->
            val dir = File(stateDir, "subscriptions/${profile.uuid}")
            dir.deleteRecursively()
            dir.mkdirs()
            val staged = File(staging, "files/imported/${profile.uuid}")
            if (staged.isDirectory) staged.copyRecursively(dir, overwrite = true)
            val config = File(dir, "config.yaml")
            if (!config.isFile) error("备份缺少订阅配置：${profile.name}")
            LocalSubscription(
                id = profile.uuid,
                name = profile.name.ifBlank { "订阅" },
                url = if (profile.type == "File") "" else profile.source,
                path = config.path,
                type = if (profile.type == "File") LocalSubscription.TYPE_FILE else LocalSubscription.TYPE_URL,
                active = profile.uuid == activeId,
                intervalMinutes = profile.interval / 60_000,
                userAgent = profile.userAgent,
                ageSecretKey = profile.ageSecretKey,
                overrideIds = profile.overrideIds,
                createdAt = profile.createdAt,
                updatedAt = System.currentTimeMillis(),
            )
        }
        val subscriptionsFile = File(stateDir, "subscriptions.json")
        subscriptionsFile.parentFile?.mkdirs()
        subscriptionsFile.writeText(
            compactJson.encodeToString(SubscriptionsFile.serializer(), SubscriptionsFile(subs)),
        )

        // 覆写：Android overrides_list.json + 内容文件 → Linux overrides.json + 同目录内容
        val androidOverrideList = File(staging, "files/overrides/overrides_list.json")
        val overridesDir = File(configDir, "overrides")
        overridesDir.mkdirs()
        val linuxOverrides = mutableListOf<LinuxOverrideEntry>()
        if (androidOverrideList.isFile) {
            val list = runCatching {
                compactJson.decodeFromString(OverrideList.serializer(), androidOverrideList.readText())
            }.getOrDefault(OverrideList())
            list.overrides.forEach { meta ->
                val ext = if (meta.format == "JavaScript") "js" else "yaml"
                val sourceFile = File(staging, "files/overrides/${meta.id}.$ext")
                if (sourceFile.isFile) {
                    sourceFile.copyTo(File(overridesDir, "${meta.id}.$ext"), overwrite = true)
                }
                linuxOverrides += LinuxOverrideEntry(
                    id = meta.id,
                    name = meta.name.ifBlank { "覆写" },
                    format = ext,
                    sourceType = if (meta.sourceType == "Remote") "remote" else "local",
                    url = meta.sourceLocation,
                    updatedAt = meta.lastUpdatedAt ?: meta.createdAt,
                )
            }
            File(overridesDir, "overrides.json").writeText(
                compactJson.encodeToString(LinuxOverrideIndex.serializer(), LinuxOverrideIndex(linuxOverrides)),
            )
        }

        // override.user.json → LinuxSettings.network
        val overrideUser = File(staging, "files/override.user.json")
        val settingsFile = File(configDir, "settings.json")
        val settingsStore = LinuxSettingsStore(settingsFile)
        var settings = settingsStore.load()
        val linuxSettingsBackup = File(staging, "linux/settings.json")
        if (linuxSettingsBackup.isFile) {
            settings = runCatching {
                compactJson.decodeFromString(LinuxSettings.serializer(), linuxSettingsBackup.readText())
            }.getOrDefault(settings)
        }
        if (overrideUser.isFile) {
            val override = runCatching {
                compactJson.decodeFromString(ConfigurationOverride.serializer(), overrideUser.readText())
            }.getOrNull()
            if (override != null) settings = settings.copy(network = override)
        }
        settingsStore.save(settings.normalized())

        // selections
        val selections = LinuxSelectionsSnapshot(
            selections = snapshot.selections.map {
                LinuxSelection(subscriptionId = it.uuid, group = it.proxy, node = it.selected)
            },
        )
        File(stateDir, "selections.json").writeText(
            compactJson.encodeToString(LinuxSelectionsSnapshot.serializer(), selections),
        )
    }

    /** 旧版 Linux 备份：settings/ + state/ + config/。 */
    private fun restoreLegacyFormat(staging: File, configDir: File, stateDir: File) {
        copyIfExists(File(staging, "settings/settings.json"), File(configDir, "settings.json"))
        copyIfExists(File(staging, "state/subscriptions.json"), File(stateDir, "subscriptions.json"))
        File(staging, "state/subscriptions").takeIf { it.isDirectory }?.let { sourceDir ->
            val target = File(stateDir, "subscriptions")
            val old = File(stateDir, "subscriptions.old")
            old.deleteRecursively()
            if (target.exists()) target.renameTo(old)
            sourceDir.renameTo(target)
            old.deleteRecursively()
        }
        copyIfExists(File(staging, "config/config.yaml"), File(configDir, "config.yaml"))
    }

    private fun copyIfExists(source: File, target: File) {
        if (!source.isFile) return
        target.parentFile?.mkdirs()
        target.writeBytes(source.readBytes())
    }

    private fun readLinuxOverrides(configDir: File): List<LinuxOverrideEntry> {
        val index = File(configDir, "overrides/overrides.json")
        if (!index.isFile) return emptyList()
        return runCatching {
            compactJson.decodeFromString(LinuxOverrideIndex.serializer(), index.readText()).profiles
        }.getOrDefault(emptyList())
    }

    private fun readLinuxSelections(stateDir: File): Map<String, Map<String, String>> {
        val file = File(stateDir, "selections.json")
        if (!file.isFile) return emptyMap()
        val snapshot = runCatching {
            compactJson.decodeFromString(LinuxSelectionsSnapshot.serializer(), file.readText())
        }.getOrNull() ?: return emptyMap()
        return snapshot.selections.groupBy { it.subscriptionId }
            .mapValues { (_, values) -> values.associate { it.group to it.node } }
    }

    private fun putBytes(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun addFile(zip: ZipOutputStream, entryName: String, file: File) {
        if (!file.isFile) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addTree(zip: ZipOutputStream, prefix: String, root: File?) {
        if (root == null || !root.exists()) return
        root.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            addFile(zip, "$prefix/$relative", file)
        }
    }

    private fun safeResolve(root: File, entryName: String): File? {
        val target = File(root, entryName).canonicalFile
        val base = root.canonicalFile
        if (!target.path.startsWith(base.path + File.separator)) return null
        return target
    }
}
