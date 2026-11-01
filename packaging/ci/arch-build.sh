#!/usr/bin/env bash
# 在 archlinux 容器里以非 root 用户构建 Arch 包。
set -euo pipefail
pacman -Syu --noconfirm --needed base-devel git >/dev/null
id builder >/dev/null 2>&1 || useradd -m builder
chown -R builder:builder "$PWD"
su builder -c "cd '$PWD' && chmod +x desktop/*.sh && ./desktop/package-arch.sh"
chmod -R a+rX desktop/build/arch-pkg
