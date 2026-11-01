#!/usr/bin/env bash
# 构建 Arch Linux 包（mexpa-<ver>-1-x86_64.pkg.tar.zst）。
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VERSION="${MEXPA_VERSION:-1.0.0}"
TAR="$ROOT/desktop/build/Mexpa-linux-x64-${VERSION}.tar.gz"
WORK="$ROOT/desktop/build/arch-pkg"
PKGDIR="$ROOT/packaging/arch"

command -v makepkg >/dev/null || { echo "需要 makepkg（Arch Linux）" >&2; exit 1; }
[ -f "$TAR" ] || "$ROOT/desktop/package-linux.sh"

rm -rf "$WORK"
mkdir -p "$WORK"
cp "$PKGDIR/PKGBUILD" "$PKGDIR/mexpa.install" "$WORK/"
cp "$ROOT/packaging/common/mexpa.desktop" "$ROOT/packaging/common/mexpa-launcher.sh" "$WORK/"
cp "$ROOT/shared-ui/src/commonMain/composeResources/drawable/mexpa_icon.png" "$WORK/mexpa.png"
cp "$TAR" "$WORK/mexpa-${VERSION}-linux-x64.tar.gz"
cp "$ROOT/LICENSE" "$WORK/LICENSE"
cp "$ROOT/packaging/common/mexpa-launcher.sh" "$WORK/mexpa-launcher"

cd "$WORK"
makepkg -f --nodeps --skipinteg --noconfirm
OUT=$(find "$WORK" -maxdepth 1 -name "${pkgname:-mexpa}-${VERSION}-*.pkg.tar.*" -printf '%f\n' | tail -1)
echo "built: $WORK/${OUT:-<package>}"
