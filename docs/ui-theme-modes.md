# 双主题：Miuix / Material 3 Expressive

界面支持 Material 3 Expressive（默认）与 Miuix 两套风格；Miuix 保留与改造前逐像素一致的外观。风格是**用户级设置**，存在
`PlatformStorage` 的 `theme_ui_mode`（`miuix` / `material`），入口在「外观与主题 → 界面与效果 →
界面风格」。参考实现是 KernelSU 的 `UiMode` + `LocalUiMode` + `KernelSUTheme` 三分法。

## 分层

```
ThemeConfig.uiMode ──> MishkaTheme(themeConfig)            ui/theme/Theme.kt
                         ├─ MiuixMishkaTheme              → MiuixTheme + miuix locals
                         └─ MaterialMishkaTheme           → MaterialExpressiveTheme + 嵌套 MiuixTheme
                              （嵌套的 MiuixTheme 只为迁移期残留 miuix 组件供色，两套 local 互不覆盖）
LocalUiMode ─────────> ui/component/design/*               组件适配层（每个组件两个实现）
```

- **唯一真源**：`ThemeConfig.resolveIsDark` 判深浅（不直接读 `isSystemInDarkTheme()`），
  `LocalUiMode` 判风格（不直接读 storage）。页面不感知持久化。
- **持久化**：`StorageKeys.THEME_UI_MODE`，`readThemeConfig`/`writeThemeConfig` 负责读写；
  `MainActivity` 冷启动读一次，切换时经 `onThemeConfigChange` 回传并整体重组。
- **系统栏**：仍由 `MainActivity.updateEdgeToEdge` 单点处理（`resolveIsDark` + `SystemBarStyle`），
  两套主题共用，避免每个页面各写一份 `WindowInsetsControllerCompat`。
- **取色**：Material 侧的种子色与 Miuix 同源（Monet 取系统主色，否则取用户强调色），
  `paletteStyle` 按名字映射到 material-kolor 的 `PaletteStyle`，风格/规范版本不支持的组合回退
  `SPEC_2021`。两套调色板因此不会在切换风格时跳色。

## 组件适配层（`ui/component/design/`）

| 文件 | 内容 |
| --- | --- |
| `Scaffold.kt` | `Scaffold` / `TopAppBar` / `SmallTopAppBar` / `MishkaScrollBehavior`；Material 侧为 M3 Scaffold + `LargeFlexibleTopAppBar` |
| `Preferences.kt` | `SwitchPreference` / `ArrowPreference` / `OverlayDropdownPreference` / `RadioButtonPreference` |
| `Controls.kt` | `TextButton`(→ M3 `Button`/`FilledTonalButton`/对话框内 `TextButton`) / `IconButton` / `Card`(+`CardDefaults`) / `SmallTitle` / `TextField` / `Slider` / `Checkbox` / `HorizontalDivider` / `CircularProgressIndicator` / `BasicComponent` / `MishkaSwitch` / `PullToRefresh` / `TabRow` / `InfiniteProgressIndicator` |
| `Typography.kt` | `Text` / `Icon`（参数面与 miuix 同名组件一致） |
| `Dialogs.kt` | `WindowDialog` / `WindowBottomSheet` → M3 `BasicAlertDialog` / `ModalBottomSheet`；对话框内按钮自动切 M3 `TextButton` |
| `Menus.kt` | `WindowListPopup` / `ListPopupColumn` / `DropdownImpl` / `WindowDropdownDialog` → M3 菜单/单选弹窗 |
| `SegmentedList.kt` | M3E 分段列表原语（移植自 KernelSU），供 Material 专属页面/弹层使用 |
| `NavigationBar.kt` | Material 底栏 `ShortNavigationBar` + M3E 悬浮底栏（Launcher3 `ScrollableFloatingToolbar` 结构）+ 宽屏 `WideNavigationRail`（可展开）+ `MishkaBackButton`（filled tonal） |

**宽屏顶栏固定**：`rememberMishkaScrollBehavior()` 会按 `rememberIsWideScreen()` 选择折叠语义——
窄屏用 `exitUntilCollapsed`（大标题随滚动折叠），宽屏（≥600dp）顶栏是固定的 `SmallTopAppBar`，Material 侧必须改用
`TopAppBarDefaults.pinnedScrollBehavior`。若宽屏仍给固定小顶栏喂 `exitUntilCollapsed`，滚动会把顶栏高度压到 0
（表现是「向下滑动顶栏被顶走」）；Miuix 的小顶栏会自己钉住 behavior 的 state，但内容侧的 nestedScroll 连接也必须换成
pinned，否则 `onPreScroll` 会先消费掉滚动量、页面反而滚不动。

**约定**：页面只 import `design` 层的同名组件；新增组件必须同时给出 Miuix 分支（逐参数委托 miuix，
默认值与被替换组件完全一致）与 Material 分支。默认参数不能写成 `MaterialTheme.*` 的裸读——
那会在 Miuix 模式下拿到 M3 默认浅色调；要么按 `LocalUiMode` 分支，要么像 `CardDefaults` 一样延后解析。

## 页面结构（对照 KernelSU）

Miuix 页保持 `Scaffold + BlurredBar(TopAppBar) + LazyColumn + Card/CardSegment + *Preference`。
Material 页在同一份页面代码下变成 `M3 Scaffold(surfaceContainer) + LargeFlexibleTopAppBar(surfaceContainer，
滚动前后同色) + LazyColumn + segmented card（surfaceBright，16dp 外圆/4dp 内圆/2dp 间隙，页边距 16dp，提示/分组标题/顶栏大标题 24dp）+
M3 ListItem 行`，即 KernelSU `*Material.kt` 的页面骨架；`CardSegment`（`ui/component/GroupedCardItems.kt`）
负责两套分角/底色切换，`groupedCardItems` 的「一行一个 lazy item」性能结构保持不变。

Material 模式仍提供模糊（复用 miuix `LayerBackdrop`/`textureBlur`，顶栏、停靠底栏和悬浮底栏都会跟随
全局 `blurEnabled`）与 M3E 悬浮底栏（`ScrollableFloatingToolbar` 结构，选中项显示图标+文字，可单独开关）。
Miuix 专属的浮动底栏样式、底栏图标模式及其设置行仍只对 Miuix 显示；顶栏模糊样式在 Material 下也可选。
主题页在 Material 下额外显示 UI 风格、悬浮底栏、节点网格分段、色彩标准等 M3E 选项。
动画速度（50–200%）通过 Material `MotionScheme`、主题颜色 spring、pager 切换 spring 与 NavDisplay `NavMotion` 缩放；点击切页、二级页 push/pop、横滑返回松手后的 settle 都会跟随，两种 UI 风格一致。Miuix 组件内部的动画节奏仍由 miuix 自身/系统控制。
状态语义色在 Material 下映射到 M3 容器（运行=secondaryContainer、进行中=tertiaryContainer、
停止=errorContainer），停止态首页状态卡使用普通 `surfaceBright` 卡片色；纯黑会连同
`surfaceContainer*` 一起刷黑，颜色切换带 spring 过渡。

Mishka 没有像 KernelSU 那样把每个页面复制成 `*Miuix.kt`/`*Material.kt`：页面有 25+ 个、1.3 万行，
而共享骨架已经集中在 `groupedCardItems`/`AdaptiveTopAppBar`/`design` 层。重复页面会让两套主题
长期漂移；这里把「两个实现」下沉到组件层，页面结构仍与 KernelSU 的 Material 页一致。

## 新增一个页面/组件的检查表

1. 页面：只 import `design.*`；滚动行为用 `rememberMishkaScrollBehavior()`；顶栏用
   `AdaptiveTopAppBar`；卡片组用 `groupedCardItems`；不要直接 `miuix.basic.*` / `miuix.preference.*`。
2. 组件：在 `design/` 下加同名函数，Miuix 分支逐参数委托 miuix（含默认值），Material 分支用
   M3/`SegmentedList` 原语；参数类型若引入 M3 类型，需同时保留 Miuix 可表达的类型。
3. 颜色：语义色走 `MishkaColors` / `StatusColors`，不要新增 `Color(0xFF...)`。
4. 文案：新字符串进 `values/`、`values-zh-rCN/`、`values-zh-rTW/` 三处；枚举可见名进
   `ui/theme/ThemeLabels.kt`。
5. 跑 `./gradlew :app:compileDebugKotlin -x buildMihomo`。

## 验证

见根目录 AGENTS.md「双主题验证」一节；核心是**同一组操作在两套风格下各做一遍**：

- 冷启动：默认 Material 3 Expressive；Miuix 外观与改造前一致（颜色、间距、圆角、模糊、底栏、折叠顶栏）。
- 外观与主题 → 界面风格 → Material：整棵树立刻换成 M3E，无需重启；杀进程重开仍是 Material。
- 两套风格下分别过 4 个 Tab、二级页（外观/网络/订阅/代理/日志/连接/关于）、弹窗与底部面板，
  检查文字/图标可读、无 miuix 与 M3 混排导致的错色、无崩溃。
- 深色/浅色/纯黑/Monet 切换后两套风格都跟随；系统栏图标明暗正确。
- 模糊开启/关闭、顶栏高斯/渐进、M3E 悬浮底栏开关分别验证；悬浮底栏在 4 个 tab 下不横向滚动。
- 悬浮底栏开启模糊后，滚动内容应透过 pill 可见，关闭后回退为 `surfaceBright` 实色。
- 宽屏（≥600dp）：Miuix 是可展开侧栏，Material 是 M3 `WideNavigationRail`（可展开/收起）；底栏分别正常。

## 已知迁移余量

- Material 分支的 12–20sp 遗留字号由 [Typography.kt](app/src/main/kotlin/com/linx3141/mexpa/ui/component/design/Typography.kt) 集中映射到 M3 type scale；9sp 小徽章改用 `labelSmall`。
- 搜索框已按 KernelSU 的 `SearchAppBar` 形态给出 M3 分支（M3 `SearchBarDefaults.InputField` +
  全屏结果层），Miuix 分支保持原实现。
- 速度折线/订阅用量水印、Proxy 节点行等 miuix squircle 自绘图形仍用 miuix 绘制 API
  （颜色已走 `StatusColors`/`MishkaColors`）。
- `LiquidGlassNavigationBar` 与其 `BadgedBox` 只服务 Miuix 的浮动底栏，不参与 M3。
