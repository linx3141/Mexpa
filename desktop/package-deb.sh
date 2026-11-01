#!/usr/bin/env bash
# 手工构建 Debian/Ubuntu .deb（不依赖 dpkg-deb，Ar/deb 格式稳定）。
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VERSION="${MEXPA_VERSION:-1.0.0}"
TAR="$ROOT/desktop/build/Mexpa-linux-x64-${VERSION}.tar.gz"
WORK="$ROOT/desktop/build/deb-pkg"
[ -f "$TAR" ] || "$ROOT/desktop/package-linux.sh"

rm -rf "$WORK"
mkdir -p "$WORK/root/DEBIAN" "$WORK/root/opt" "$WORK/root/usr/bin" \
  "$WORK/root/usr/share/applications" "$WORK/root/usr/share/icons/hicolor/512x512/apps" \
  "$WORK/root/usr/share/doc/mexpa" "$WORK/data"
tar -xzf "$TAR" -C "$WORK/data"
cp -a "$WORK/data/mexpa" "$WORK/root/opt/mexpa"
install -m755 "$ROOT/packaging/common/mexpa-launcher.sh" "$WORK/root/usr/bin/mexpa"
install -m644 "$ROOT/packaging/common/mexpa.desktop" "$WORK/root/usr/share/applications/mexpa.desktop"
install -m644 "$ROOT/shared-ui/src/commonMain/composeResources/drawable/mexpa_icon.png" "$WORK/root/usr/share/icons/hicolor/512x512/apps/mexpa.png"
install -m644 "$ROOT/LICENSE" "$WORK/root/usr/share/doc/mexpa/copyright"

cat > "$WORK/root/DEBIAN/control" <<CONTROL
Package: mexpa
Version: $VERSION
Architecture: amd64
Maintainer: linx3141 <linx3141@users.noreply.github.com>
Installed-Size: $(du -sk "$WORK/root" | cut -f1)
Depends: libgl1, libx11-6, libxext6, libxrender1, libxi6, libxtst6, libfontconfig1, libfreetype6, libstdc++6, zlib1g, libpng16-16, libcap2-bin
Recommends: python3-gi, gir1.2-ayatanaappindicator3-0.1, xdg-utils
Section: net
Priority: optional
Description: mihomo proxy client for Linux (Compose Desktop)
 Mexpa is a mihomo-based proxy client with system proxy, TUN,
 subscriptions and a Compose Desktop UI.
CONTROL
cat > "$WORK/root/DEBIAN/postinst" <<'POSTINST'
#!/bin/sh
set -e
if command -v setcap >/dev/null 2>&1; then
  setcap cap_net_admin+ep /opt/mexpa/bin/mihomo || true
fi
update-desktop-database -q /usr/share/applications 2>/dev/null || true
gtk-update-icon-cache -q /usr/share/icons/hicolor 2>/dev/null || true
exit 0
POSTINST
cat > "$WORK/root/DEBIAN/postrm" <<'POSTRM'
#!/bin/sh
set -e
update-desktop-database -q /usr/share/applications 2>/dev/null || true
exit 0
POSTRM
chmod 755 "$WORK/root/DEBIAN/postinst" "$WORK/root/DEBIAN/postrm"

( cd "$WORK/root" && tar --owner=0 --group=0 --numeric-owner -cf - opt usr | xz -9 > "$WORK/data.tar.xz" )
( cd "$WORK/root/DEBIAN" && tar --owner=0 --group=0 --numeric-owner -cf - control postinst postrm | gzip -9 > "$WORK/control.tar.gz" )
printf '2.0\n' > "$WORK/debian-binary"
( cd "$WORK" && ar rcs "mexpa_${VERSION}_amd64.deb" debian-binary control.tar.gz data.tar.xz )
echo "built: $WORK/mexpa_${VERSION}_amd64.deb"
