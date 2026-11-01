package com.linx3141.mexpa.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.linx3141.mexpa.ui.theme.ThemeConfig
import java.io.File

enum class MainTab(val label: String, val icon: ImageVector) {
    Home("主页", Icons.Rounded.Home),
    Proxy("代理", Icons.Rounded.Tune),
    Subscription("订阅", Icons.Rounded.CloudUpload),
    Settings("设置", Icons.Rounded.Settings),
}

enum class SubPage(val title: String, val icon: ImageVector) {
    Logs("日志", Icons.Rounded.Article),
    Connections("连接", Icons.Rounded.History),
    Providers("Provider", Icons.Rounded.WifiTethering),
    Dns("DNS 查询", Icons.Rounded.Dns),
    Theme("外观与主题", Icons.Rounded.Palette),
    Network("网络", Icons.Rounded.Tune),
    ExternalControl("外部控制器", Icons.Rounded.Link),
    Automation("自动化", Icons.Rounded.Settings),
    Backup("备份与恢复", Icons.Rounded.Backup),
    FileManager("文件管理", Icons.Rounded.Folder),
    Overrides("覆写设置", Icons.Rounded.Tune),
    About("关于", Icons.Rounded.Info),
    Meta("Meta 设置", Icons.Rounded.Tune),
    OverrideEdit("编辑覆写", Icons.Rounded.Article),
    FileEditor("编辑文件", Icons.Rounded.Article),
    SubscriptionAdd("新建订阅", Icons.Rounded.CloudUpload),
    SubscriptionAddUrl("从 URL 导入", Icons.Rounded.Link),
    SubscriptionEdit("编辑订阅", Icons.Rounded.CloudUpload),
}

/**
 * 桌面壳的 UI 状态：只放**界面需要且需要跨页面共享**的东西；
 * 运行数据在 [LinuxProxyController]，订阅/GeoIP 在各自 repository。
 */
class DesktopAppState(initialSubPage: SubPage? = null, initialTab: MainTab = MainTab.Home) {
    val home = System.getProperty("user.home")
    val stateDir = DesktopPlatform.stateDir.apply { mkdirs() }
    val defaultConfig = DesktopPlatform.defaultConfigFile()

    private val settingsStore = LinuxSettingsStore(File(DesktopPlatform.configDir, "settings.json"))
    var linuxSettings by mutableStateOf(settingsStore.load())
        private set
    var settingsListener: ((LinuxSettings) -> Unit)? = null
    var subscriptionRepository: SubscriptionRepository? = null
    var geoDataManager: GeoDataManager? = null
    var overridesStore: OverridesStore? = null
    var editingSubscriptionId: String? = null
    var editingOverrideId: String? = null
    var editorPath: String? = null
    var showWindowProvider: (() -> Unit)? = null

    var themeConfig by mutableStateOf(linuxSettings.theme?.toThemeConfig() ?: ThemeSnapshot.defaultDesktopTheme())
    var pendingTheme by mutableStateOf<ThemeConfig?>(null)
    var tab by mutableStateOf(initialTab)
    var subPage by mutableStateOf(initialSubPage)

    var configPath by mutableStateOf(linuxSettings.configPath ?: defaultConfig.path)
    var binaryPath by mutableStateOf(linuxSettings.binaryPath ?: defaultMihomoBinary())
    var workDir by mutableStateOf(linuxSettings.workDir ?: stateDir.path)
    var networkOverride by mutableStateOf(linuxSettings.network ?: com.linx3141.mexpa.domain.model.ConfigurationOverride())
        private set
    var mixedPort by mutableStateOf(linuxSettings.network?.mixedPort ?: 7890)
    var controllerEndpoint by mutableStateOf(linuxSettings.network?.externalController ?: "127.0.0.1:9090")
    var controllerSecret by mutableStateOf(linuxSettings.network?.secret.orEmpty())
    var allowExternalControl by mutableStateOf(false)

    fun open(page: SubPage) {
        subPage = page
    }

    fun back() {
        subPage = null
    }

    fun selectTab(value: MainTab) {
        tab = value
        subPage = null
    }

    /** 系统代理 / TUN 等需要跨会话保留的设置：先落盘，再通知运行层即时应用。 */
    fun updateLinuxSettings(next: LinuxSettings) {
        val normalized = next.normalized()
        if (normalized == linuxSettings) return
        linuxSettings = normalized
        settingsStore.save(normalized)
        settingsListener?.invoke(normalized)
    }

    /** Miuix/Material 切换延迟到窗口级协程应用，避免主题页退出导致协程被取消。 */
    fun requestTheme(next: ThemeConfig) {
        if (next.uiMode == themeConfig.uiMode) {
            updateTheme(next)
        } else {
            pendingTheme = next
        }
    }

    fun commitPendingTheme() {
        val next = pendingTheme ?: return
        pendingTheme = null
        updateTheme(next)
    }

    fun updateTheme(next: ThemeConfig) {
        themeConfig = next
        updateLinuxSettings(linuxSettings.copy(theme = ThemeSnapshot.from(next)))
    }

    /** Network 页的覆写：同时更新运行层可见的 mixedPort/controller 值。 */
    fun updateNetworkOverride(next: com.linx3141.mexpa.domain.model.ConfigurationOverride) {
        networkOverride = next
        mixedPort = next.mixedPort ?: mixedPort
        controllerEndpoint = next.externalController ?: controllerEndpoint
        controllerSecret = next.secret ?: controllerSecret
        updateLinuxSettings(linuxSettings.copy(network = next))
    }

    fun updateControllerEndpoint(endpoint: String?, secret: String?) {
        updateNetworkOverride(
            networkOverride.copy(
                externalController = endpoint?.trim()?.takeIf { it.isNotEmpty() },
                secret = secret?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    fun updatePathSettings(configPath: String? = null, binaryPath: String? = null, workDir: String? = null) {
        configPath?.takeIf { it.isNotBlank() }?.let { this.configPath = it }
        binaryPath?.takeIf { it.isNotBlank() }?.let { this.binaryPath = it }
        workDir?.takeIf { it.isNotBlank() }?.let { this.workDir = it }
        updateLinuxSettings(
            linuxSettings.copy(
                configPath = this.configPath,
                binaryPath = this.binaryPath,
                workDir = this.workDir,
            ),
        )
    }

    fun setActiveConfigPath(path: String?) {
        if (!path.isNullOrBlank()) configPath = path
    }

    fun updateWindowBounds(x: Int, y: Int, width: Int, height: Int) {
        val current = linuxSettings
        if (current.windowX == x && current.windowY == y &&
            current.windowWidth == width && current.windowHeight == height
        ) {
            return
        }
        updateLinuxSettings(current.copy(windowX = x, windowY = y, windowWidth = width, windowHeight = height))
    }

    fun updateAutoUpdate(enabled: Boolean) {
        updateLinuxSettings(linuxSettings.copy(autoUpdateEnabled = enabled))
    }
}

private fun defaultMihomoBinary(): String {
    System.getProperty("mexpa.binary")?.takeIf { it.isNotBlank() }?.let { return it }
    System.getenv("MIHOMO_BIN")?.takeIf { it.isNotBlank() }?.let { return it }
    // jpackage 启动器一定带这个 property；AppImage 用 APPIMAGE。优先于 ProcessHandle，
    // 因为部分 Windows/JDK 组合拿不到 launcher 的 command()。
    val launcher = System.getProperty("jpackage.app-path")
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() }
        ?: ProcessHandle.current().info().command().orElse(null)
    if (launcher != null) {
        val bundled = File(File(launcher).parentFile, DesktopPlatform.binaryName)
        if (bundled.canExecute()) return bundled.path
    }
    // 开发态（gradle run）没有随包内核，向上找仓库根目录的开发二进制
    val cwd = File(System.getProperty("user.dir"))
    listOf(cwd, cwd.parentFile).filterNotNull().forEach { dir ->
        listOf(DesktopPlatform.binaryName, "mihomo-linux", "mihomo.exe").forEach { name ->
            val candidate = File(dir, name)
            if (candidate.canExecute()) return candidate.path
        }
        File(dir, "desktop/build/windows-bin/${DesktopPlatform.binaryName}").takeIf { it.canExecute() }?.let { return it.path }
    }
    return DesktopPlatform.binaryName
}
