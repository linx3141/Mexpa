#!/usr/bin/env bash
# 构建 RPM（需要 rpmbuild；GitHub Actions 的 Fedora/Ubuntu 任务会安装）。
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VERSION="${MEXPA_VERSION:-1.0.0}"
TAR="$ROOT/desktop/build/Mexpa-linux-x64-${VERSION}.tar.gz"
WORK="$ROOT/desktop/build/rpm"
[ -f "$TAR" ] || "$ROOT/desktop/package-linux.sh"
command -v rpmbuild >/dev/null || { echo "需要 rpmbuild（Fedora: rpm-build；Debian/Ubuntu: rpm）" >&2; exit 1; }

rm -rf "$WORK"
mkdir -p "$WORK"/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS}
cp "$TAR" "$WORK/SOURCES/Mexpa-linux-x64-${VERSION}.tar.gz"
cp "$ROOT/packaging/common/mexpa.desktop" "$WORK/SOURCES/mexpa.desktop"
cp "$ROOT/shared-ui/src/commonMain/composeResources/drawable/mexpa_icon.png" "$WORK/SOURCES/mexpa.png"
cp "$ROOT/packaging/common/mexpa-launcher.sh" "$WORK/SOURCES/mexpa-launcher"
cp "$ROOT/LICENSE" "$WORK/SOURCES/LICENSE"
cp "$ROOT/packaging/rpm/mexpa.spec" "$WORK/SPECS/mexpa.spec"
rpmbuild --define "_topdir $WORK" --define "version ${VERSION}" -bb "$WORK/SPECS/mexpa.spec"
OUT=$(find "$WORK/RPMS" -name "mexpa-${VERSION}-*.rpm" | head -1)
echo "built: ${OUT:-<rpm>}"
