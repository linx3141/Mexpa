#!/usr/bin/env bash
# 构建 AppImage（x86_64）。需要 appimagetool；缺失时自动下载到 desktop/build/tools。
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VERSION="${MEXPA_VERSION:-1.0.0}"
TAR="$ROOT/desktop/build/Mexpa-linux-x64-${VERSION}.tar.gz"
WORK="$ROOT/desktop/build/appimage"
TOOLS="$ROOT/desktop/build/tools"
APPIMAGE_TOOL="${APPIMAGETOOL:-$TOOLS/appimagetool-x86_64.AppImage}"
[ -f "$TAR" ] || "$ROOT/desktop/package-linux.sh"

if [ ! -x "$APPIMAGE_TOOL" ]; then
  mkdir -p "$TOOLS"
  echo "下载 appimagetool…"
  curl -fL --retry 3 -o "$APPIMAGE_TOOL" \
    https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x "$APPIMAGE_TOOL"
fi

rm -rf "$WORK"
mkdir -p "$WORK/AppDir/usr/lib" "$WORK/AppDir/usr/share/applications" \
  "$WORK/AppDir/usr/share/icons/hicolor/512x512/apps"
tar -xzf "$TAR" -C "$WORK"
mv "$WORK/mexpa" "$WORK/AppDir/usr/lib/mexpa"
cp "$ROOT/packaging/common/AppRun" "$WORK/AppDir/AppRun"
chmod +x "$WORK/AppDir/AppRun"
install -m644 "$ROOT/packaging/common/mexpa.desktop" "$WORK/AppDir/mexpa.desktop"
sed -i 's#^Exec=.*#Exec=AppRun %u#' "$WORK/AppDir/mexpa.desktop"
install -m644 "$ROOT/shared-ui/src/commonMain/composeResources/drawable/mexpa_icon.png" "$WORK/AppDir/mexpa.png"
cp "$WORK/AppDir/mexpa.png" "$WORK/AppDir/.DirIcon"
# 标准 AppDir 内容：桌面文件/图标放到 AppDir 根即可
OUT="$ROOT/desktop/build/Mexpa-${VERSION}-x86_64.AppImage"
ARCH=x86_64 "$APPIMAGE_TOOL" --appimage-extract-and-run --no-appstream "$WORK/AppDir" "$OUT" 2>/dev/null \
  || ARCH=x86_64 "$APPIMAGE_TOOL" --no-appstream "$WORK/AppDir" "$OUT"
chmod +x "$OUT"
echo "built: $OUT"
