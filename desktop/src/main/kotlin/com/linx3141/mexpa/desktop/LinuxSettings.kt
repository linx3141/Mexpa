package com.linx3141.mexpa.desktop

import com.linx3141.mexpa.domain.model.ConfigurationOverride
import com.linx3141.mexpa.ui.theme.BottomBarMode
import com.linx3141.mexpa.ui.theme.FloatingBottomBarStyle
import com.linx3141.mexpa.ui.theme.ThemeAccentColor
import com.linx3141.mexpa.ui.theme.ThemeConfig
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.UiMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

/** 需要跨会话保留的 Linux 接管设置。 */
@Serializable
data class LinuxSettings(
    val systemProxy: Boolean = false,
    /** 非空时系统代理改用 PAC（KDE Proxy Config Script / GNOME autoconfig-url）。 */
    val pacUrl: String = "",
    val tunEnabled: Boolean = false,
    val tunStack: String = TunStack.Gvisor.id,
    /** null = 默认 ~/.config/mexpa/config.yaml / 随包 mihomo / ~/.local/state/mexpa */
    val configPath: String? = null,
    val binaryPath: String? = null,
    val workDir: String? = null,
    /** Network 页的覆写；null = 不覆写。 */
    val network: ConfigurationOverride? = null,
    /** 主题设置（桌面端独立持久化，Android 走 ThemeStorage）。 */
    val theme: ThemeSnapshot? = null,
    val autoUpdateEnabled: Boolean = true,
    /** 订阅下载/更新优先走本机代理（代理运行中时），对齐 Android 的「更新走代理」默认开。 */
    val updateViaProxy: Boolean = true,
    /** TUN 分应用代理：off / include（仅代理选中 uid）/ exclude（绕过选中 uid）。 */
    val perAppMode: String = "off",
    val perAppUids: List<Int> = emptyList(),
    /** TUN 下接管系统 DNS（dns-hijack any:53）；关闭后 DNS 仍走 mihomo 内部但不再劫持 53。 */
    val dnsTakeover: Boolean = true,
    /** WebDAV 备份（密码明文保存在 settings.json；文件权限 600）。 */
    val webDavUrl: String = "",
    val webDavUser: String = "",
    val webDavPassword: String = "",
    /** 托盘/后台：关闭窗口时隐藏到托盘（代理继续运行）。 */
    val closeToTray: Boolean = true,
    /** 打开应用自动连接。 */
    val autoConnectOnLaunch: Boolean = false,
    /** 登录自启（--minimized）时恢复上次运行状态。 */
    val restoreLastState: Boolean = true,
    /** 上次代理是否在运行（用于登录恢复）。 */
    val lastRunActive: Boolean = false,
    /** 允许外部 CLI/socket 控制（Automation 页开关）。 */
    val externalControlAllowed: Boolean = true,
    /** 订阅更新后自动重启代理使新配置生效。 */
    val restartAfterUpdate: Boolean = true,
    /** 窗口尺寸/位置（最大化时不覆盖）。 */
    val windowX: Int? = null,
    val windowY: Int? = null,
    val windowWidth: Int? = null,
    val windowHeight: Int? = null,
) {
    fun normalized(): LinuxSettings = copy(
        tunStack = TunStack.entries.firstOrNull { it.id == tunStack }?.id ?: TunStack.Gvisor.id,
    )
}

enum class TunStack(val id: String) {
    Gvisor("gvisor"),
    System("system"),
    Mixed("mixed"),
}

/** ThemeConfig 的平台无关快照：只存共享 UI 定义的枚举/值，不引 materialkolor 类型。 */
@Serializable
data class ThemeSnapshot(
    val colorMode: Int = 2,
    val uiMode: String = UiMode.Material.storageValue,
    val pureBlack: Boolean = false,
    val useMonet: Boolean = false,
    val accentColor: String = ThemeAccentColor.Blue.storageValue,
    val blurEnabled: Boolean = false,
    val topBarBlurStyle: String = TopBarBlurStyle.Gaussian.storageValue,
    val floatingBottomBar: Boolean = false,
    val materialFloatingBottomBar: Boolean = true,
    val floatingBottomBarStyle: String = FloatingBottomBarStyle.Miuix.storageValue,
    val bottomBarMode: String = BottomBarMode.IconAndText.storageValue,
    val densityScale: Float = 1f,
    val animationSpeed: Float = 1f,
    val proxyGridSegmented: Boolean = false,
) {
    fun toThemeConfig(base: ThemeConfig = defaultDesktopTheme()): ThemeConfig = base.copy(
        colorMode = colorMode,
        uiMode = UiMode.fromStorage(uiMode),
        pureBlack = pureBlack,
        useMonet = useMonet,
        accentColor = ThemeAccentColor.entries.firstOrNull { it.storageValue == accentColor } ?: ThemeAccentColor.Blue,
        blurEnabled = blurEnabled,
        topBarBlurStyle = TopBarBlurStyle.fromStorage(topBarBlurStyle),
        floatingBottomBar = floatingBottomBar,
        materialFloatingBottomBar = materialFloatingBottomBar,
        floatingBottomBarStyle = FloatingBottomBarStyle.fromStorage(floatingBottomBarStyle),
        bottomBarMode = BottomBarMode.fromStorage(bottomBarMode),
        densityScale = densityScale,
        animationSpeed = animationSpeed,
        proxyGridSegmented = proxyGridSegmented,
    )

    companion object {
        fun defaultDesktopTheme(): ThemeConfig = ThemeConfig(
            uiMode = UiMode.Material,
            colorMode = 2,
            accentColor = ThemeAccentColor.Blue,
            blurEnabled = false,
        )

        fun from(config: ThemeConfig): ThemeSnapshot = ThemeSnapshot(
            colorMode = config.colorMode,
            uiMode = config.uiMode.storageValue,
            pureBlack = config.pureBlack,
            useMonet = config.useMonet,
            accentColor = config.accentColor.storageValue,
            blurEnabled = config.blurEnabled,
            topBarBlurStyle = config.topBarBlurStyle.storageValue,
            floatingBottomBar = config.floatingBottomBar,
            materialFloatingBottomBar = config.materialFloatingBottomBar,
            floatingBottomBarStyle = config.floatingBottomBarStyle.storageValue,
            bottomBarMode = config.bottomBarMode.storageValue,
            densityScale = config.densityScale,
            animationSpeed = config.animationSpeed,
            proxyGridSegmented = config.proxyGridSegmented,
        )
    }
}

/**
 * `~/.config/mexpa/settings.json`。
 *
 * 早期只有 3 个键的 `settings.properties`（系统代理/TUN），这里保留一次性迁移：
 * 读到旧文件就转成 JSON，随后删除旧文件。
 */
class LinuxSettingsStore(private val file: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val legacyFile = File(file.parentFile, "settings.properties")

    fun load(): LinuxSettings {
        val fromJson = runCatching {
            if (file.isFile) json.decodeFromString(LinuxSettings.serializer(), file.readText()) else null
        }.getOrNull()
        if (fromJson != null) return fromJson.normalized()
        val legacy = runCatching {
            if (!legacyFile.isFile) return@runCatching null
            val properties = Properties().apply { legacyFile.inputStream().use { load(it) } }
            LinuxSettings(
                systemProxy = properties.getProperty("systemProxy")?.toBooleanStrictOrNull() ?: false,
                tunEnabled = properties.getProperty("tunEnabled")?.toBooleanStrictOrNull() ?: false,
                tunStack = properties.getProperty("tunStack") ?: TunStack.Gvisor.id,
            )
        }.getOrNull()
        if (legacy != null) {
            save(legacy)
            legacyFile.delete()
            return legacy.normalized()
        }
        return LinuxSettings()
    }

    fun save(settings: LinuxSettings) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(LinuxSettings.serializer(), settings.normalized()))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }
}
