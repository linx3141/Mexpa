# Windows 适配说明

## 已抽象

- `DesktopPlatform`：配置 `%APPDATA%\mexpa`，状态 `%LOCALAPPDATA%\mexpa`，`mihomo.exe` / `mexpa-transform.exe`
- 单实例：`%LOCALAPPDATA%\mexpa\instance.lock`（FileChannel.tryLock）
- 控制口：Linux 用 Unix socket，Windows 用 loopback TCP + `control.port`（CLI/单实例 show 可用）
- 系统代理：`WindowsSystemProxy`，写 HKCU Internet Settings，PowerShell 通知 WinINet；快照恢复
- 托盘：AWT SystemTray 图标；右键使用 Win32 原生 `CreatePopupMenu + TrackPopupMenuEx`（JNA），不依赖 Compose/Swing 菜单
- 通知：Windows 用托盘气泡，Linux 仍优先 notify-send
- 自启动：`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
- `clash://` / `clashmeta://`：`HKCU\Software\Classes`
- TUN：mihomo + wintun；`whoami /groups` 检测管理员；非管理员启动 TUN 时用 ShellExecuteW("runas") 弹出 UAC，提权重启一个 javaw/安装版实例并自动启动，旧实例退出；不经过 PowerShell 控制台
- 窗口标题/DPI：Windows 使用系统 DPI，不走 Xft.dpi
- 窗口透明：Windows 默认透明以保留圆角；若个别显卡/Wine 出现 `X_CopyArea BadMatch`，可设 `MEXPA_OPAQUE_WINDOW=1` 退回不透明

## 限制

- Windows TUN 需要整个 Mexpa 以管理员身份启动；当前不做 FlClash 式常驻 Helper 服务
- 系统代理依赖 Windows PowerShell（WinINet 通知）；注册表写入本身不依赖
- MSI / EXE 需要 WiX Toolset；app-image/zip 不需要
- 尚未在真实 Windows 上跑过构建/界面测试

## 构建

见 `packaging/windows/README.md`，或 `.github/workflows/windows-package.yml`。
