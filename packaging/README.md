# Linux 打包

当前支持 x86_64：

| 目标 | 命令 | 产物 |
|---|---|---|
| 通用 app-image + tar | `./desktop/package-linux.sh` | `desktop/build/Mexpa-linux-x64-1.0.0.tar.gz` |
| Arch Linux | `./desktop/package-arch.sh` | `desktop/build/arch-pkg/mexpa-1.0.0-1-x86_64.pkg.tar.zst` |
| Debian/Ubuntu | `./desktop/package-deb.sh` | `desktop/build/deb-pkg/mexpa_1.0.0_amd64.deb` |
| AppImage | `./desktop/package-appimage.sh` | `desktop/build/Mexpa-1.0.0-x86_64.AppImage` |
| RPM | `./desktop/package-rpm.sh`（需 rpmbuild） | `desktop/build/rpm/RPMS/x86_64/mexpa-1.0.0-1.*.rpm` |

## 说明

- **TUN**：AppImage 的挂载是只读 squashfs，无法对内部 `mihomo` 执行 `setcap`；`AppRun` 会把 `mihomo` / `mexpa-transform` 复制到 `~/.local/share/mexpa/bin` 并通过环境变量指给应用，首次启用 TUN 时仍会弹 pkexec 授权。deb/rpm/Arch 包的 post-install 会直接对 `/opt/mexpa/bin/mihomo` 执行 `setcap cap_net_admin+ep`。
- **AppIndicator 托盘**：需要宿主安装 `python3-gi` + `libayatana-appindicator3`；deb/rpm/Arch 已作为推荐/可选依赖声明，缺失时回退 AWT。
- **glibc**：`package-linux.sh` 现在优先用 Adoptium 21 打包（runtime 最高 GLIBC_2.15），兼容 Ubuntu 22.04 / Debian 12 / RHEL 9 等；不要用发行版自带的新 JDK（Arch JDK 27 需要 GLIBC_2.38）。
- **架构**：当前只打 x86_64；arm64 需要单独 JDK/Skiko/mihomo 构建环境。
- **GitHub Actions**：`.github/workflows/linux-packages.yml` 已加入 AppImage、deb、rpm、Arch 四个构建任务（未在本机验证）。
