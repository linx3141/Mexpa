# Linux 共享 UI 迁移

目标：Android 与 Linux 使用同一套 Compose UI、同一套主题 token 和同一套资源；Linux 只替换平台后端，不再维护一份自写桌面页面。

## 当前进度（阶段 1）

- 新增 `shared-ui` KMP 模块：`android` + `desktop` target
- 已迁入 `commonMain`：
  - `ui/theme`：`AppColors`、`AppThemeLocals`、`StatusColors`、`Type`、`UiMode`、`TopBarBlurStyle`
  - `ui/component/design`：`Badge`、`Controls`、`Dialogs`、`Menus`、`Preferences`、`Scaffold`、`SegmentedList`、`Typography`、`NavigationBar`、`SearchAppBar`
  - `ui/component/blur`：`BlurExt`、`ColorBlendToken`
  - 平台返回键 `PlatformBackHandler`（Android actual = Activity BackHandler，desktop actual = 暂空）
- Android app 通过 `implementation(projects.sharedUi)` 消费同一批组件
- 已把 `values/strings.xml`、`values-zh-rCN`、`values-zh-rTW` 复制到 `shared-ui/src/commonMain/composeResources`，并生成公开 `Res` 资源类
- 已迁入页面组件：`GroupedCardItems`、`AdaptiveTopAppBar`、`WindowSize`、`SearchScreenState`、`SearchStatus`、`FooterHint`
- **第一个真实页面已共享**：`AboutScreenMaterial` 已迁入 `commonMain`，Android 与 desktop 调用同一份 About 页面；App 图标/版本通过参数注入
- **第二个真实页面已共享**：`ThemeSettingsScreen` 已迁入 `commonMain`；`ThemeConfig`、`ThemeLabels` 同步迁入，Android 通过 `ThemeStorage` 读写，UI 不再直接依赖 `PlatformStorage`
- **共享主题已落地**：`MishkaTheme` / `MaterialMishkaTheme` / `ThemeExt` 迁入 `commonMain`；平台动态取色走 `platformMonetSeedColor` expect/actual
- Linux 现在通过 `MishkaTheme` 渲染，并实际使用共享 `AboutScreenMaterial` 与 `ThemeSettingsScreen`；自写 About/Theme 页面不再作为入口
- **第三个真实页面已共享**：`SettingsScreen` 的布局/交互迁入 `commonMain`；Android 保留 `SettingsScreen` 适配层写 `PlatformStorage`/`BootStartManager`/`ProxyServiceBridge`，Linux 通过 `SettingsCapabilities` 隐藏不可用能力
- Linux 当前已实际使用共享 Settings 页（VPN/ROOT/App Proxy/WiFi/开机/动态通知等入口隐藏）
- 已共享首页视觉原语：`HomeShared`（Badge/InfoRow/卡片标题）、`TrafficSparkline`、`SubscriptionUsageBar` 与 `TrafficHistory` 模型
- **首页已共享**：`HomeUiState`/snapshots、domain models、Home sections、`HomeContent`、`HomeFabOverlay` 均迁入 `commonMain`
- Android 保留 `HomeScreen` 适配层收集 `HomeViewModel` 的 Flow；Linux 已用共享 `HomeContent` + `HomeFabOverlay` 替换自写 Home
- Linux Home 的 mihomo 启停仍走 `LinuxProxyController`，但页面布局/状态卡/FAB/快捷入口来自共享 UI
- **代理页已共享**：`ProxyUiState`/`ProxyGroupUi`、`ProxyScreenContent` 迁入 `commonMain`；Android 保留 `ProxyScreen` 适配层，Linux 已用共享代理页替换自写 ProxyPage
- 代理组图标走共享 `LocalProxyIconLoader`，Android 注入 IconLoader，Linux 默认不加载
- **订阅页已共享**：`SubscriptionUiState`、`ImportProgress`/`ImportStep`、`ProfileOperation`、`ImportProgressDialog`/`ImportProgressLabel`、`SubscriptionScreenContent` 均迁入 `commonMain`
- Android 保留 `SubscriptionScreen` 适配层；Linux 已用共享订阅页替换自写 SubscriptionPage
- **订阅二级页已共享**：`SubscriptionAddScreenContent`、`SubscriptionAddUrlScreenContent`、`SubscriptionEditScreenContent`
- **Provider 页已共享**：`ProviderUiState`、`ProviderRefreshDialog`、`ProviderScreenContent`；Linux 已用共享 Provider 页替换自写页面
- **Log 页已共享**：`LogMessage`、`LogUiState`、`IndexedLog`、`LogScreenContent`；Linux 日志页使用共享 UI
- **DNS 查询页已共享**：`DnsQuery` 模型、`DnsQueryUiState`、`DnsQueryScreenContent`；Linux 查询仍走本地 `InetAddress` 后端
- **连接页已共享**：`ConnectionInfo`、`ConnectionUiState`、`ConnectionScreenContent`；搜索过滤从 ViewModel 移到共享 UI，Linux 已使用共享连接页
- **搜索组件已共享**：`SearchBar`/`SearchStatus`/`SearchBox`/`SearchPager` 迁入 `commonMain`，返回键走 `PlatformBackHandler`
- **设置二级页已共享**：`AutomationScreenContent`、`ExternalControlScreenContent`、`FileManagerScreenContent`；`RestartRequiredHint` 一并共享
- Linux 的 Automation / ExternalControl / FileManager 二级页已切换到共享 UI
- **Network 设置页已共享**：`ConfigurationOverride`、`NetworkSettingsUiState`、`NetworkSettingsScreenContent`；`TriStatePreference`、`ListEditDialog` 一并共享
- Linux Network 二级页已使用共享 UI
- **覆写设置列表已共享**：`OverrideProfile`、`OverrideProfileUiState`、`OverrideCommon`、`OverrideListScreenContent`；Linux 覆写列表已使用共享 UI
- **备份恢复页已共享**：`BackupUiState`、`BackupRestoreScreenContent`；WebDAV 字段通过 state/callback 传入，Android 适配层保留 SAF 文件选择器
- **Linux 外壳已去自写化**：删除 `HomePage`/`ProxySubscriptionPages`/`SettingsScreens`/`ToolsScreens`/`DesktopComponents`/`DesktopTheme`；桌面壳改为共享 `ExpressiveNavigationBar` 底部导航 + 共享页面，窗口默认手机比例
- 全 Linux 路由均调用共享 composable；不再有自写页面渲染器
- **Android 共享资源打包修复**：AGP 9 KMP Android library 不会自动把 Compose 资源放进 app assets；`app/build.gradle.kts` 现在用 `syncSharedComposeResources` 投到 `assets/composeResources/com.linx3141.mexpa.shared.resources`，否则运行时会 `MissingResourceException`
- Linux 窗口关闭 blur（桌面无 backdrop）并使用共享底栏，默认 420×900 手机比例
- 对比截图：`screenshots/shared-ui/android-home.png`、`screenshots/shared-ui/linux-home.png`
- 调试时可用 `MEXPA_START_PAGE=Theme`（或 `About`）直接进入共享页面验证资源与主题
- `shared-ui` desktop target 已能编译，供后续 Linux 页面复用

## 剩余工作

1. 把现有 Android UI 的 `R.string.*` 调用切换到共享 `Res.string.*`，并迁移图片资源（当前 Android 页面仍读自己的 `R.*`，但共享资源类已经就绪）
2. 把屏幕 composable 从 Android ViewModel/Room 依赖改为 UI state + callbacks
3. 迁移 Home、Proxy、Subscription、Settings 与全部二级页到 `shared-ui/commonMain`
4. Linux 实现平台适配器：文件选择、剪贴板、mihomo backend、系统代理
5. 用并排截图验证 Android 与 Linux 像素级一致性，再移除自写 `desktop` 页面

在阶段 2 完成前，`desktop` 中现有页面只是临时原型，不作为最终 UI。


## Linux 鼠标拖动滚动（2026-10-31）

- 共享 UI 的 `LazyColumn` 在桌面端额外挂 `Modifier.mouseDragScroll`：标准 `detectVerticalDragGestures` 收鼠标位移，经 `NestedScrollDispatcher` 走 pre/self/post 三段 nested scroll（与 foundation `scrollable` 的 performScroll 同口径），拖动结束补 `postFling` 让顶部大标题吸附
- Android 该 modifier 为空实现，触摸拖动仍由 `LazyColumn` 自带 `scrollable` 处理；不再用 AWT 事件注入或手写 connection 调用
- Miuix 的 `overScrollVertical()` / `scrollEndHaptic()` 收进 `platformOverScrollVertical()` / `platformScrollEndHaptic()`：Android 保持原样，桌面端为空（无橡皮筋回弹与触觉，也避免其按窗口尺寸取模时在桌面端产生 NaN 布局偏移）

## Linux 导航与动效（2026-10-31）

- Material 模式：窄屏 `FloatingExpressiveNavigationBar`（受 `materialFloatingBottomBar` 控制）或标准底栏；宽屏 `ExpressiveNavigationRail`
- Miuix 模式：窄屏 `NavigationBar` / `FloatingNavigationBar`；宽屏 `NavigationRail`
- 页面切换：Tab 横向滑动 + 淡入淡出；二级页滑入/滑出
- 宽屏阈值与 Android 对齐为 600dp
- 桌面仍禁用 blur/毛玻璃，Miuix 的 liquid-glass 浮动样式未在 Linux 实现


## XWayland 缩放

- Compose Desktop 走 AWT/XWayland，不读取 Wayland 的逐显示器分数缩放
- Linux 入口读取 `Xft.dpi`（当前 144dpi）并把 Compose density 与初始窗口尺寸按 1.5 放大
- 可用 `MEXPA_UI_SCALE` 覆盖；截图：`screenshots/shared-ui/linux-xwayland-scaled.png`
