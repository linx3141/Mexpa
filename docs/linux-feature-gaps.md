# Linux 版功能缺口统计（vs Android）

统计基准：`app/`（Android）、`shared-ui/commonMain`（共享 UI）、`desktop/`（Linux 壳与后端）。
标记：❌ 缺失 / ⚠️ 有壳无核（共享页面在渲染，但后端是桩或不生效）/ 🚫 Android 专属 / ✅ 已实现。

## 总览

- 共享 UI 页面齐全，Linux 后端已接 mihomo external-controller / 订阅 / 覆写 / 备份 / 托盘 / 自动化；不再有「页面是壳、后端全桩」的整层缺口。
- external-controller 客户端已接入全部主要端点（`/proxies`、`/providers`、`/connections`、`/logs`、`/traffic`、`/memory`、`/version`、`/dns/query`、`/configs`）。
- 设置/订阅/主题/窗口状态/覆写/selections 均已落盘。
- 共统计 **101 项**编号条目：❌ 缺失 7 项、⚠️ 部分 15 项、🚫 Android 专属/不做 9 项、✅ 已实现 70 项。**Linux 的实际功能缺口 = 22 项**。
- **2026-10-31 已落地**：TUN 虚拟网卡（第 1 项）、系统代理（KDE/GNOME/MATE + UI 开关 + 快照恢复，第 8/9 项）。
- **2026-10-31 P0 已落地**：mihomo external-controller REST+WS 客户端与真实数据接线（第 12/13/14/21/24/45–55/57/59/72–75 项）。
- **2026-10-31 P1/P2 已落地**：订阅导入/校验/编辑/自动更新/active 持久化（25/26/29–31/33/36/37/39/40，32/35/38/42/43 部分）、设置持久化（79/80/81）、GeoIP 管理（20）、Network 覆写生效（65/76）、运行状态展示（100）、冲突检测（101）、托盘/单实例/登录自启（83/84/85，83 部分）、本地备份恢复（68）。
- 2026-11-01 第二批：分应用 uid、DNS 接管（TUN）、Meta age 密钥生成。
- 2026-11-01 第三批：文件管理、订阅覆写（YAML/JS + 编辑器）、PAC、休眠恢复。
- 2026-11-01 第四批：WebDAV、外部自动化 CLI/socket、三阶段导入、provider prefetch。
- 2026-11-01 第五批：托盘后台/关闭到托盘、第二实例唤起、自动连接/登录恢复、更新后重启、自动更新聚合通知。
- 2026-11-01 第六批：hybrid age 密钥、首页连接数、TUN route-exclude 私网排除、CLI 深链参数。
- 2026-11-01 第八批：M3E/Miuix 悬浮底栏真模糊、Liquid Glass 组件移植、Miuix 关于页分支。
- 2026-11-01 第七批：Android 通用备份、通知异步、主题切换状态应用级、版本动态、托盘依赖提示、远端覆写走代理、Automation 接线、多屏夹取、clash scheme。
- 剩余主要是：真·分应用（93）、Wi-Fi SSID 策略（94）、runtime 沙箱/内核升级（19/22）、P3 打包/自动更新/原生 Wayland/窗口阴影（87–89）、少量 ⚠️ 形态差异。

## 已具备（对照用）

✅ 共享 UI/主题/资源、4 Tab + 12 二级页壳、宽窄屏导航、Miuix/Material 双模式
✅ mihomo 子进程启动/停止（`-d/-f/-ext-ctl 127.0.0.1:9090`）、mixed-port 从 config.yaml 正则读取
✅ 订阅 URL 原始 YAML 下载到 `~/.local/state/mexpa/subscriptions`、本地 YAML 文件登记（无入口）
✅ 本地日志文件一次性读取、DNS 用系统 `InetAddress` 查询
✅ GNOME gsettings 系统代理后端（无 UI 入口）
✅ 无边框窗口、圆角、最小尺寸、窗口/任务栏图标、顶栏拖动窗口、列表拖动滚动、Xft.dpi 缩放
✅ jpackage app-image + tar.gz 便携包（mihomo 二进制随包 `bin/mihomo`）

## A. 代理核心与网络（Android 三种隧道 / 系统级接管）

1. ✅ TUN 全局代理：已通过 mihomo `--override-json` 启用（`tun.enable/stack/auto-route/dns-hijack`），随包 mihomo 用 `-tags with_gvisor` 构建；需要一次性 `setcap cap_net_admin+ep` 授权（FlClash 用 root helper 服务达到同一目的，更严格但需要常驻服务）。
2. 🚫 ROOT TUN 对等物（Android ROOT 专属）：Linux 已用「TUN + setcap」覆盖同用途。
3. 🚫 ROOT TPROXY 对等物（Android ROOT 专属）：Linux 未实现，TUN 已覆盖全局接管。
4. ✅ 进程生命周期：启动后立即退出自动重试一次；停止 TERM→3s→KILL→2s 超时链并上报，错误走系统通知；无自动拉起（Android 也没有）。
5. ⚠️ 系统 DNS 接管：TUN 模式 dns-hijack 已接；纯系统代理模式需要 root/NetworkManager/systemd-resolved 权限，风险高，未做。
6. 🚫 热点/共享网络流量处置（Android ROOT 专属）。
7. ⚠️ 权限模型：Linux 无 FlClash 式常驻 systemd root helper，只有 setcap（换包/换内核后需重新授权，已有自动 pkexec 提示）。
8. ✅ 系统代理：已实现 KDE（kwriteconfig6/5 + DBus reparse）、GNOME/MATE（gsettings），并在「设置 → 覆写设置 → 网络接管」提供开关与状态。
9. ✅ 系统代理可靠性：应用前写快照（`~/.local/state/mexpa/system-proxy.snapshot`），停止/退出按快照恢复；被 `kill -9` 后下次启动先恢复快照。
10. ⚠️ PAC 支持 KDE/GNOME/MATE；环境变量代理仅订阅下载回退，不注入系统/子进程环境（无法在不影响全局会话的前提下做到）。
11. ⚠️ 失败诊断：启动失败/异常退出有状态文案 + 原生通知；TUN 缺权限会自动弹 pkexec。仍无 Android 式 toast/Error 卡片。

## B. mihomo 运行时后端（当前只有「起进程」）

12. ✅ external-controller REST 客户端：`/version`、`/configs`、`/proxies`、`/providers/proxies`、`/providers/rules`、`/connections`、`/dns/query`、`/memory`、`/cache/flush`、`/configs` PATCH。
13. ✅ WebSocket 客户端：`/traffic`、`/logs`、`/connections`，断线 1s→10s 退避重连。
14. ✅ API ready 检查：启动后轮询 `/version`（40×250ms），失败时 UI 提示「外部控制器未就绪」。
15. ⚠️ 生命周期：启动即退重试一次、停止超时强杀已做；仍无崩溃自动拉起（Android 也没有）与代次所有权模型。
16. ✅ 数据层按 config.yaml 的 `external-controller`/`secret` 连接；ExternalControl 页写入 settings.json，下次启动/网络设置变更触发的重启会生效（不再只改内存）。
17. ⚠️ 配置生成：不写回 config.yaml（全走 `--override-json` + 运行时有效配置）；mixed-port/secret 从配置读取，等价但形态不同。
18. ⚠️ override 注入：单一 runtime/override.json + 设置变更自动重启；没有 Android 的 override.user.json / override.run.json 双文件拆分。
19. ❌ runtime 目录沙箱（Android `imported/`、`runtime/`、provider 缓存回写）；Linux 订阅 YAML 散落，无归属校验。
20. ✅ GeoIP/GeoSite/ASN 数据管理：Android 构建期预置 `geoip.metadb`/`geosite.dat`/`ASN.mmdb` 到 assets 并运行时提取；Linux 包内没有，GEOIP/GEOSITE 规则首次运行要靠 mihomo 联网下载，失败无提示。
21. ✅ mihomo 版本读取：About 页显示 `/version` 的结果。
22. ❌ mihomo 二进制升级/校验/回退（只有随包 `bin/mihomo` 或 `MIHOMO_BIN`/PATH）。
23. ✅ 代理模式：Home/Proxy 页展示 `/configs` 的 mode，Home 的模式入口经 `PATCH /configs` 切换 rule/global/direct。
24. ✅ 首页当前连接数：运行中显示「N 条活跃」，数据来自连接 WS 快照。

## C. 订阅管理

25. ✅ URL 导入入口：共享 `SubscriptionAdd`/`SubscriptionAddUrl` 页面存在，desktop 未接线（`onNavigateAdd = {}`、`canAdd = false`）；后端 `addOrUpdateSubscription()` 无调用方。
26. ✅ 本地文件导入入口：`addFileSubscription()` 无调用方，无文件选择器（Android 用 SAF）。
27. ⚠️ 导入入口：CLI --import、CLI 深链参数、系统 clash:// scheme 已注册；剪贴板导入未做。
28. ⚠️ 二维码不做（无相机）；深链参数解析已支持（见 27）。
29. ✅ 订阅编辑：名称、URL、更新间隔、per-profile UA、age 密钥（`canEdit = false`，页面未接线）。
30. ✅ 单条更新 / 全部更新 / 取消更新：`canUpdate = false`、`canRefresh = false`、`onCancelUpdate = {}`。
31. ✅ 自动更新调度：Android `ProfileUpdateScheduler`（AlarmManager 对账 imported 表）+ `ProfileWorker`；Linux 无任何调度。
32. ✅ 三阶段导入：下载/校验写入 `subscriptions/.pending/{id}/`，提交时旧目录移入 `.old/{id}`、pending 换入，失败回滚（无 draft 编辑 UI，pending 仅作导入沙箱）。
33. ✅ 订阅校验：未经过 mihomo Parse / 规则校验，坏 YAML 也会被登记为订阅，启动时才失败。
34. ✅ provider prefetch：导入前对含 `proxy-providers` 的配置跑 `mihomo -prefetch`（需 mishka build tag，Linux 内核已带）；失败不阻塞导入。
35. ✅ age 加密订阅：导入/更新用 `-age-secret-key` 校验，运行时启动命令已带 `--age-secret-key`（密钥来自 active 订阅，切换 active 时热更新并重启）；加密配置保持原样落盘。
36. ✅ 订阅自动命名（Content-Disposition filename → URL host → 默认名兜底链）。
37. ✅ 订阅流量/到期数据（provider `subscriptionInfo` 聚合、Home 流量条、Subscription 卡片信息）。
38. ⚠️ 数据库：Linux 用 JSON（settings/subscriptions/overrides/selections），语义覆盖三表主要能力，但没有 Room 的事务级三表同步。
39. ✅ 订阅切换生效链路：`SubscriptionScreenContent.onSetActive` 只改内存 `active` 标记，未调 `state.activate()` → `configPath` 不更新，切换不生效。
40. ✅ 订阅删除链路：页面直接 `subscriptions.removeAll`，绕过 `deleteSubscription()`（不删文件、不 fallback active）。
41. ✅ per-profile 选择记录：`~/.local/state/mexpa/selections.json`，选择/取消选择即持久化，启动就绪后按 active 订阅回放。
42. ⚠️ 导入进度（下载/预取/校验）+ 取消已接；无 pending 草稿编辑态（pending 仅导入沙箱）。
43. ✅ 文件管理：列出配置文件/日志/运行时覆写/设置/订阅文件/覆写内容，可打开编辑（YAML/JS/文本编辑器）、保存、删除；覆写文件走 OverridesStore 一致更新。
44. ✅ 订阅级覆写 + YAML 编辑器：覆写列表可勾选应用到当前 active 订阅，YAML 深度合并 / JS `function main(c)` 由 `mexa-transform`（与 Android mishka_core/overrides 同一份 Go 实现）在启动前套用；文本编辑器支持保存/删除/远程刷新。

## D. 四个主 Tab 的数据缺口

### Home（全部是桩）
45. ✅ 实时上下行速度/流量曲线（`/traffic` WS + TrafficHistory）。
46. ✅ 内存/CPU/uptime：`/proc/meminfo`、`/proc/stat` 差分、进程启动时间。
47. ✅ IP/接口/地址类型（NetworkInterface 枚举，LAN/TUN 判断）。
48. ✅ 延迟测试：经 mixed-port 发真实 HTTP 请求（走规则引擎，和 Android RuleLatencyTester 同口径）。
49. ✅ 订阅用量：`/providers/proxies` 的 subscriptionInfo 聚合（Total>0 求和、Expire 取最近非零）。
50. ✅ provider 流量刷新（Home 卡片的刷新入口）。
51. ✅ 运行中版本/模式（`/version`、`/configs`）。
52. ✅ FAB「重启/重载」→ `restart()`（运行中重启 mihomo 重新读配置）。

### Proxy（数据来自 config.yaml 正则，不是 API）
53. ✅ 真实代理组/节点（`/proxies`，隐藏组过滤、GLOBAL 在非 global 模式沉底）。
54. ✅ 节点/组切换：`PUT /proxies/{group}`（Selector/URLTest/Fallback 通用）。
55. ✅ 组测速 / 节点测速 / unfix（`/proxies/{name}/delay` + fixed 清空）。
56. ✅ provider 节点合并：`/providers/proxies` 节点按 runtime 优先补缺合并进组列表（见 57）。
57. ✅ provider 节点合并：`/providers/proxies` 节点按 runtime 优先补缺合并进组列表；节点测速路由到 provider healthcheck。
58. ✅ 节点/组图标加载：`ProxyIconLoader` 注入实现（HttpClient 下载 + Skia 解码 + 内存/负缓存）。
59. ✅ 节点搜索直接过滤真实组/节点。

### Subscription（见 C 节，展示层已共享）
60. ✅ 订阅页 add/edit/update/refresh/delete 全部接到 SubscriptionRepository（导入/校验/更新/编辑/active）。

### Settings
61. ⚠️ 设置入口：Linux 隐藏 VPN/ROOT/隧道模式选择（不适用）；Meta 设置入口已启用。
62. ⚠️ 设置入口：分应用代理（uid 版在 TUN 段）/登录自启/Meta 已启用；Wi-Fi 策略、动态通知、隐藏任务卡片是 Android 专属仍隐藏。
63. ✅ 通用项：打开应用自动连接、登录自启恢复上次运行状态、订阅更新后自动重启、更新走代理、窗口状态均已接入并持久化。
64. ✅ 文件管理、覆写设置入口已启用（后端完成）。
65. ✅ Network 页只改内存：mixedPort/端点/secret 不写 config/override，控制器仍读 config.yaml；`onUpdateDns` 空实现（DNS 覆写不可用）。
66. ✅ ExternalControl 页的端点/secret 写入 settings.json（Network 覆写），运行层启动/重启时生效。
67. ✅ 外部自动化：CLI/socket 可开关（Automation 页持久化），支持 status/start/stop/restart/import/show/hide/quit；clash:// / clashmeta:// scheme 已在打包版注册。
68. ✅ 备份恢复：zip 与 Android 双向兼容（backup.json + files/imported + files/overrides + files/override.user.json + selections，额外 linux/settings.json）；本地与 WebDAV 均可；兼容旧 Linux 备份；恢复提示重启。
69. ✅ 覆写设置：列表/新建 YAML/JS/文件导入/远程 URL/编辑/删除/应用到当前订阅；修改后自动重启代理。
70. ✅ 主题持久化；M3E 悬浮底栏使用与 Android 相同的 `rememberBlurBackdrop + layerBackdrop + BlurredBar` 真模糊；Miuix 悬浮底栏使用 `textureBlur` + GlassStroke 高光；`IosLike` 样式已移植桌面版 Liquid Glass（miuix runtime shader lens），而非半透明近似。
71. ✅ 版本号从 jar manifest 读取（Implementation-Version / Implementation-Build），关于页与设置页不再写死。

## E. 二级页后端缺口汇总

72. ✅ Logs：`/logs` WS 实时流 + 120ms 合并刷新 + 清空；日志风暴不再逐行触发重组。
73. ✅ Connections：`/connections` WS + REST 兜底、按相邻快照差分速率、关闭单条/全部。
74. ✅ Providers：代理/规则 provider 列表、更新单条/全部、错误行内展示（healthcheck API 已封装，UI 入口未接）。
75. ✅ DNS 查询走 mihomo `/dns/query`，并支持 flush cache / fake-ip。
76. ✅ Network 设置只有 mixedPort 的内存值；DNS 覆写/端口/监听项均未接。
77. ✅ Meta 设置：classic X25519（Kotlin/bech32）与 hybrid mlkem768-x25519（调用 `mihomo age keygen-pq`）密钥生成 + 复制。
78. ✅ Backup/FileManager/Overrides 三个二级页后端均已完成（本地+WebDAV、文件编辑、覆写编排）。

## F. 设置与状态持久化

79. ✅ 设置持久化覆盖主题/Network/路径/控制端口/系统代理/TUN/PAC/WebDAV/订阅更新/Wi-Fi?（托盘、自动连接、恢复、重启开关、窗口尺寸位置）。
80. ✅ 已有 `LinuxSettingsStore`（Properties，仅 systemProxy/tunEnabled/tunStack 三个键）；主题/网络等仍无 PlatformStorage/ThemeStorage 对等物。
81. ✅ 配置文件路径 / mihomo 二进制路径 / 工作目录没有 UI（只有 `DesktopAppState` 默认值与 `MIHOMO_BIN` 环境变量；README 声称支持，实际不可配）。
82. ✅ 窗口尺寸/位置记忆 + 多屏边界夹取；窗口完全落在屏外时回退系统默认位置。

## G. 桌面端体验与分发（Android 不需要、Linux 应有但目前没有）

83. ✅ 托盘：KDE/Wayland 优先 Ayatana AppIndicator（SNI）原生菜单，缺失依赖时提示并回退 AWT；关闭到托盘/后台常驻/第二实例唤起均已接。
84. ✅ 登录自启（`.desktop` autostart / systemd user unit）。
85. ✅ 单实例锁 / DBus 激活：可多开，多个 mihomo 抢同一端口。
86. ✅ 通知：启停/异常/失败 + 自动更新聚合通知；notify-send 改为后台线程发送，不再阻塞 UI。
87. ❌ `.desktop` 菜单项、hicolor 图标安装、deb/rpm/Flatpak/AppImage 打包、自动更新（目前只有 jpackage app-image + tar.gz）。
88. ❌ 原生 Wayland 后端（Compose Desktop 限制，只能 XWayland）+ 分数缩放精度问题（靠 `Xft.dpi` 近似）。
89. ❌ 窗口阴影（KWin 不给 undecorated XWayland 画阴影；只能应用侧设 `_KDE_NET_WM_SHADOW` 或自绘，未做）。
90. ⚠️ 休眠恢复：30s 墙钟跳变检测，唤醒后重放系统代理/TUN；锁屏无专门处理（不影响代理）。

## H. Android 专属能力（Linux 无对等需求，但替代方案也未做）

91. 🚫 VPN `VpnService`/VPN 授权对话框。
92. 🚫 ROOT TUN / ROOT TPROXY / iptables / `su` / attach-only 重连。
93. ❌ 真·分应用代理（cgroup/nftables per-process）：当前只有 TUN include/exclude-uid，同用户桌面应用无法逐应用区分。
94. ❌ Wi-Fi SSID 自动切换：FlClash Linux 有 SystemNetwork/SSID 插件，Linux 端未实现（NetworkManager D-Bus 可行）。
95. ✅ 登录自启已实现（XDG autostart；见 84）。
96. 🚫 QS 磁贴、动态流量通知（用户明确不做动态通知；托盘通知已有）。
97. 🚫 隐藏最近任务卡片（Linux 无此概念）。
98. 🚫 SAF / Monet 动态取色 / 预测性返回 / Baseline Profile / 通知渠道分区（平台专属）；clash:// 深链已注册。
99. 🚫 系统 DNS 观察与跨用户 loopback（`INTERACT_ACROSS_USERS`）等 Android 平台机制。

## 补充（2026-10-31 晚，本轮实现后复查）

100. ✅ 运行时状态/错误消息没有展示位：`LinuxProxyState.message`（启动失败、mihomo 立即退出、TUN 授权失败、系统代理写入失败等）没有任何 UI 渲染，只有 FAB 状态变化；Network 页面只显示「期望设置」不显示「实际生效状态/最近错误」。
101. ✅ 与其他代理客户端的冲突检测：本机已跑 FlClash（TUN 设备 `FlClash` + 系统代理 7890）时，Mexpa 启动 TUN/mihomo 只会在日志里报端口/TUN 冲突，没有「检测到其他 TUN/系统代理客户端」的提示或选项。

## I. 文档与实现不一致（需要修正的声明）

- `desktop/README.md`：「配置文件、mihomo 路径、运行目录可配置」——实际无 UI（见 81）。
- `desktop/README.md`：「GNOME 系统代理」——后端存在但无开关、KDE 不支持（见 8/9）。
- `README.md` Linux 小节列出的能力比实际多，建议以本文件为准同步。

## J. 建议优先级

已完成：P0（API 客户端 + 真实数据 + 节点切换/测速 + 日志/连接/Provider/DNS/首页统计）。

剩余：
- **P1**：订阅导入/校验/编辑/自动更新/active 持久化（25–39）、设置持久化（79–82）、GeoIP 数据管理（20）、配置生成/override 注入（17–19）。
- **P2**：运行时状态展示位（100）、与 FlClash 等客户端的冲突检测（101）、本地备份恢复（68）、覆写/YAML 编辑器（69/44）、托盘/单实例/登录自启（83–85）、Network 页生效（65/76）。
- **P3（本轮不做）**：安装包与自动更新（87）、原生 Wayland（88）、窗口阴影（89）。
