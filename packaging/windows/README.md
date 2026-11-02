# Windows 打包

前提：Windows 10/11 x64、JDK 21（含 jpackage）、Go 1.25、WiX Toolset（EXE 安装包）。

```powershell
# 构建 mihomo.exe / mexa-transform.exe / 下载 wintun.dll
.\packaging\windows\build-binaries.ps1

# app-image + zip
.\desktop\package-windows.ps1

# 额外构建 EXE 安装包（需要 WiX Toolset）
.\desktop\package-windows.ps1 -Installer
```

产物：

- `desktop/build/Mexpa-windows-x64-1.0.0.zip`
- app-image：`desktop/build/compose/binaries/main/app/mexpa`
- EXE（若安装 WiX）：`desktop/build/compose/binaries/main/exe`

## Windows 行为

- 配置：`%APPDATA%\mexpa`，状态：`%LOCALAPPDATA%\mexpa`
- 系统代理：HKCU Internet Settings（WinINet），退出/崩溃快照恢复
- 托盘：AWT SystemTray；不需要 Ayatana/AppIndicator
- 自启动：`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
- `clash://` / `clashmeta://`：`HKCU\Software\Classes`
- TUN：需要管理员权限；非管理员启动 TUN 时会自动弹出 UAC 并提权重启实例，使用 `bin\wintun.dll`
- 控制通道：loopback TCP（`control.port`），CLI 与单实例仍可用
