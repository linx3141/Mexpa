# Mexpa for Linux

Linux 原生适配使用 Compose Multiplatform，与 Android 共用 `shared-ui` 中的页面、组件、主题和字符串资源。

## UI 架构

- Android 与 Linux 渲染同一批 composable，不再维护桌面自写页面
- `shared-ui` 同时产出 Android / desktop target
- Linux 外壳使用共享导航组件，窗口默认 420×900 手机比例
- 窄屏：Material 浮动底栏 / 标准底栏；Miuix 浮动/标准底栏
- 宽屏（≥600dp）：Material `ExpressiveNavigationRail` / Miuix `NavigationRail` 侧边导航
- 页面切换使用滑动 + 淡入淡出动画
- Miuix 模式已可切换（主题设置 → UI 风格），Linux 使用 Miuix 的 NavigationBar / NavigationRail / FloatingNavigationBar
- 已移除 `HomePage` / `SettingsScreens` / `ToolsScreens` 等自写桌面页面

## 已接入后端

- mihomo 子进程启动 / 停止
- mixed-port / external-controller（`127.0.0.1:9090`）
- 配置文件、mihomo 路径、运行目录
- 订阅 URL 下载 / 本地 YAML 导入 / 切换活跃配置
- **系统代理**：KDE（kwriteconfig6/5 写 kioslaverc + DBus reparse）、GNOME / MATE（gsettings）
- **TUN 虚拟网卡**：mihomo `--override-json` 打开 `tun` + `dns-hijack`
- 日志读取、DNS 查询

## 网络接管（系统代理 / TUN）

「设置 → 覆写设置」顶部新增 Linux 专属的「网络接管」卡片（Android 不显示）：

- **系统代理**开关：参考 FlClash 的后端选择顺序，KDE 优先 `kwriteconfig`，GNOME/MATE 走 `gsettings`。
  应用前会把当前设置快照到 `~/.local/state/mexpa/system-proxy.snapshot`，停止/退出时按快照恢复；
  进程被 `kill -9` 时下次启动会先按快照恢复，不会把系统代理留在已经退出的端口上。
- **TUN 虚拟网卡**开关：通过 mihomo 的 `--override-json` 叠上
  `tun.enable/stack/auto-route/auto-detect-interface/dns-hijack`（config.yaml 本身不改）。
  用户配置里没有 `dns:` 段时自动补默认 nameserver。
  运行中切换会重启 mihomo 生效。

TUN 需要 `CAP_NET_ADMIN`。Linux 上没有 Android 的 VpnService，这里采用「对随包 mihomo 一次性
`setcap`」的轻量方案（FlClash 则是安装 root helper systemd 服务，隔离更严但需要常驻服务）：

```bash
# 便携包（路径按实际解压位置）
sudo setcap cap_net_admin+ep /path/to/mexpa/bin/mihomo
# 或运行 UI 里「TUN 权限 → 授权」，走 pkexec 弹系统授权框
```

- 打包里的 mihomo 用 `-tags with_gvisor` 构建，TUN 默认 `gvisor` 栈；没有该 tag 的内核会自动回退 `system`。
- 重新打包/替换 mihomo 后 capability 会丢，权限行会重新显示「需要一次性 root 授权」。
- 若不想给二进制授 cap：也可以手动把 mihomo 装到 root 拥有的路径并 `setcap`，或用 FlClash 式 helper 服务（未实现）。

## 不可用能力

Linux 设置页通过 `SettingsCapabilities` 隐藏以下 Android 专属入口：

- VPNService / ROOT TUN / ROOT TPROXY / 分应用代理（Linux 统一用 TUN + 系统代理）
- Wi-Fi 策略
- 动态通知、开机自启、Android 通用订阅选项
- 分应用代理、Meta / FileManager / Overrides 的完整后端（尚未实现）

这些能力不会以可点击假按钮的形式出现。

## 订阅 / 设置 / 备份（P1–P2）

- **订阅管理**：URL 导入、本地文件导入（AWT 文件选择器）、mihomo `-t` 校验、编辑（名称/URL/间隔/UA/age 密钥）、
  单条/全部更新、按间隔自动更新（进程内 60s 调度，退出后不更新）、active 切换持久化。
  存储为 `~/.local/state/mexpa/subscriptions.json` + `subscriptions/{id}/config.yaml`。
- **设置持久化**：`~/.config/mexpa/settings.json`（主题快照、Network 覆写、控制端口/secret、配置文件/内核/工作目录、系统代理/TUN、自动更新开关）。
- **GeoIP 管理**：网络页「GeoIP 数据」下载 `geoip.metadb`/`geosite.dat`/`ASN.mmdb` 到 mihomo 工作目录，重启代理后生效。
- **本地备份/恢复**：备份 zip 与 Android 双向兼容（backup.json + files/…），Linux 额外保存 linux/settings.json；本地与 WebDAV 均支持。
- **深链**：打包版自动注册 `clash://` / `clashmeta://` scheme；未运行时启动后自动导入。
- **本地备份/恢复（旧）**：设置 → 备份与恢复 → 本地文件，zip 内含 settings/subscriptions/config；恢复有 zip-slip 校验，完成后提示重启。
- **桌面体验**：托盘菜单（显示/启停/退出）、单实例锁（`MEXPA_ALLOW_MULTI=1` 可绕过）、登录自启（仅打包版，`~/.config/autostart/mexpa.desktop`）。
- **分应用代理 / DNS 接管**：TUN 模式支持 `include-uid`/`exclude-uid`（网络页选择 uid；桌面应用通常共用 uid，属用户级过滤）与 `dns-hijack any:53` 接管系统 DNS。
- **age 密钥**：设置 → Meta 设置 → 生成 X25519 classic age 密钥对（私钥填进订阅 age 字段；运行态自动解密）。
- **窗口记忆 / 通知**：窗口尺寸位置写入 settings.json；启动/停止/异常退出走系统通知。
- **订阅覆写**：设置 → 覆写设置可新建/导入 YAML、JS、远程覆写并应用到当前订阅；启动前由 `mexa-transform`（与 Android 共用的 Go 实现）套用，改完自动重启代理。
- **文件管理**：设置 → 文件管理可打开/编辑/删除配置文件、运行时覆写、订阅文件（内置等宽文本编辑器）。
- **PAC / 休眠恢复**：系统代理支持 PAC URL（KDE/GNOME/MATE）；休眠唤醒后自动重放系统代理与 TUN。
- **外部自动化**：运行中的实例在 `~/.local/state/mexpa/control.sock`（600）提供控制口，CLI 支持 `--status`、`--start`、`--stop`、`--restart`、`--import <url>`、`--quit`。
- **WebDAV 备份**：备份页填 URL/账号/密码即可测试连接、上传/下载 `mexpa-backup.zip`（同主机重定向、Basic 认证）。
- **provider prefetch**：导入含 `proxy-providers` 的配置前跑 `mihomo -prefetch`（mishka tag 内核），失败不阻塞。
- **TUN 授权 / 通知**：TUN 未授权时启动代理会直接弹系统 polkit 密码框（pkexec setcap），成功后继续启动；通知优先走 `notify-send`，不再用 AWT 的 X11 气泡。
- **托盘与后台**：KDE/Wayland 优先用 Ayatana AppIndicator（StatusNotifierItem）托盘（python3-gi + libayatana-appindicator3，缺失时回退 AWT）；关闭窗口默认隐藏到托盘并保持代理运行（可关闭）；托盘菜单显示窗口/启停/退出；第二个实例或 `--show` 唤起已有窗口；支持打开应用自动连接、登录自启恢复上次运行。
- **订阅更新**：更新 active 订阅后自动重启代理（可关），自动更新完成发一条聚合通知（全成功或列出失败订阅）。
- **age hybrid 密钥**：设置 → Meta 设置可生成 mlkem768-x25519（`mihomo age keygen-pq`）与 classic X25519 密钥。
- **首页连接数**：运行中显示当前活跃连接条数。
- **TUN 私网排除**：默认 `route-exclude-address` 排除私网/保留段/多播，避免 LAN/P2P 被劫持。
- **深链参数**：CLI 支持 `clash://install-config?url=...` / `clashmeta://...`（未注册系统 scheme）。
- **内存卡片**：显示 mihomo 核心 `/memory` 的 inuse/oslimit（与 Android 同口径），不再显示整机 /proc/meminfo 占用。
- **延迟测试**：三个探针经本机 mixed-port 发真实 HTTP 请求（规则引擎决定出口），不会用本地 /version 探测冒充延迟。
- **版本/托盘依赖/多屏**：版本号来自 jar manifest；AppIndicator 缺失依赖会提示；窗口位置多屏夹取。
- **悬浮底栏**：M3E 与 Miuix 使用 miuix backdrop 真模糊；Miuix 的 `IosLike` 样式使用移植到桌面的 Liquid Glass（lens/折射 shader）。
- **关于页**：Miuix 下使用 Miuix 风格关于页，M3E 下使用 M3E 关于页。
- **状态/冲突提示**：网络页展示代理进程/系统代理/TUN 的最近状态，并检测其他 TUN 设备、系统代理指向、mixed-port 占用。

## 平台层自检

不打开窗口，直接验证系统代理写入/恢复与 TUN 配置生成（发给用户的排障命令）：

```bash
MEXPA_LINUX_DIAG=1 \
MEXPA_LINUX_DIAG_BIN=$PWD/mihomo-linux \
MEXPA_LINUX_DIAG_CONFIG=/path/to/config.yaml \
./gradlew :desktop:run            # 打印 PASS/FAIL；退出码非 0 表示失败

# 会真的写入并恢复系统代理（执行前后不要有别的程序改代理设置）
MEXPA_LINUX_DIAG=1 MEXPA_LINUX_DIAG_PROXY=1 \
MEXPA_LINUX_DIAG_BIN=$PWD/mihomo-linux \
MEXPA_LINUX_DIAG_CONFIG=/path/to/config.yaml \
./gradlew :desktop:run

# external-controller 数据链路自检（起真实 mihomo 子进程，验证 /proxies、WS 日志/流量、连接、DNS 等）
MEXPA_LINUX_DIAG=1 MEXPA_LINUX_DIAG_DATA=1 \
MEXPA_LINUX_DIAG_BIN=$PWD/mihomo-linux \
MEXPA_LINUX_DIAG_CONFIG=/path/to/config.yaml \
./gradlew :desktop:run

# P1 自检：设置持久化、订阅导入/校验/更新/active、override 生成、GeoIP 下载逻辑
MEXPA_LINUX_DIAG=1 MEXPA_LINUX_DIAG_P1=1 MEXPA_LINUX_DIAG_GEO=1 \
MEXPA_LINUX_DIAG_BIN=$PWD/mihomo-linux ./gradlew :desktop:run

# P2 自检：单实例锁、冲突检测、本地备份/恢复、自启可用性
MEXPA_LINUX_DIAG=1 MEXPA_LINUX_DIAG_P2=1 ./gradlew :desktop:run
```

## 窗口装饰

KDE/XWayland 默认会给 AWT 窗口画服务端标题栏和白色外框。Linux 入口现在使用 `WindowDecoration.Undecorated`，不再绘制独立标题栏。
窗口三键（最小化/最大化/关闭）通过共享 `LocalTopBarActions` 注入到各页面现有 TopAppBar 的右侧：

- 没有独立标题栏和 `Mexpa for Linux` 标题
- 没有 KDE 白色边框和系统标题栏
- 左键在列表上按住拖动即可滚动页面（与移动端触摸拖动同一条 nested scroll 链路，不注入 AWT 事件）
- 拖动通过共享 UI 的 `Modifier.mouseDragScroll` 实现：标准 `draggable` 手势收集鼠标位移，再经 `NestedScrollDispatcher` 分发 pre/post scroll，顶部大标题与 Miuix overscroll 都按 Compose 标准流程联动
- 移动端由 `LazyColumn` 自带的 `scrollable` 处理触摸拖动，Android 侧该 modifier 是空实现
- 首页底部内容按悬浮底栏高度 + 80dp FAB 安全区留白，最后两张卡不会被底栏遮住
- 按住顶栏的固定条（收起后的标题栏高度）左键拖动即可移动窗口，和普通应用一致；展开的大标题区域拖动是滚动页面，不会拖走窗口；KDE `Alt + 左键拖动` 仍可作为备用方式
- 窗口/任务栏图标来自共享 Compose 资源 `mexpa_icon.png`（`Window(icon = ...)` 同时写入 `_NET_WM_ICON`），jpackage 的 `linux.iconFile` 复用同一份 PNG；关于页的 App 图标用 `mexpa_icon_foreground.png`（白色前景，沿用 Android 的白底 + 深色 tint 逻辑）
- 窗口带最小尺寸（360×600 dp，按 `Xft.dpi` 缩放），不会再被 KDE 拉到 1×1

## XWayland 缩放

Compose Desktop 当前没有原生 Wayland 窗口后端，AWT 在 XWayland 下仍按 96dpi 报告，
导致窗口明显小于同屏 Wayland 应用。desktop 入口现在会：

- 读取 `xrdb -query` 的 `Xft.dpi`
- 按 `dpi / 96` 同时放大 Compose density 和初始窗口尺寸
- 可用 `MEXPA_UI_SCALE=1.5` 显式覆盖

## 构建 / 运行

```bash
# 构建 mihomo
cd mihomo
CGO_ENABLED=0 go build -trimpath -buildvcs=false -o ../mihomo-linux .
cd ..

# 运行
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :desktop:run

# 调试：直接打开共享页面
MEXPA_START_TAB=Home   ./gradlew :desktop:run
MEXPA_START_PAGE=About ./gradlew :desktop:run

# 调试：直接以 Miuix / 浮动底栏启动
MEXPA_UI_MODE=Miuix MEXPA_FLOATING_BOTTOM=true ./gradlew :desktop:run
```

## 便携包

```bash
./desktop/package-linux.sh
```

产物：

```text
desktop/build/Mexpa-linux-x64-1.0.0.tar.gz
```

解压运行：

```bash
tar -xzf desktop/build/Mexpa-linux-x64-1.0.0.tar.gz -C /tmp
/tmp/mexpa/bin/mexpa
```
