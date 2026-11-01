#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "package-macos.sh must run on macOS" >&2
  exit 1
fi

JAVA_HOME="${JAVA_HOME:-$HOME/mexpa-build/jdk21/Contents/Home}"
export JAVA_HOME
export MEXPA_SKIP_ANDROID=1
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk-dummy}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

"$ROOT/gradlew" :desktop:createDistributable --console=plain
APP="$(find "$ROOT/desktop/build/compose/binaries/main/app" -maxdepth 1 -type d -name '*.app' | head -n 1)"
if [[ -z "${APP:-}" ]]; then
  echo "jpackage .app not found" >&2
  exit 1
fi
MACOS_DIR="$APP/Contents/MacOS"
cp "$ROOT/desktop/build/macos-bin/mihomo" "$MACOS_DIR/mihomo"
cp "$ROOT/desktop/build/macos-bin/mexpa-transform" "$MACOS_DIR/mexpa-transform"
chmod +x "$MACOS_DIR/mihomo" "$MACOS_DIR/mexpa-transform"

mkdir -p "$ROOT/desktop/build/dist"
ZIP="$ROOT/desktop/build/dist/Mexpa-macos-x64-portable.zip"
rm -f "$ZIP"
ditto -c -k --sequesterRsrc --keepParent "$APP" "$ZIP"
echo "portable: $ZIP"

"$ROOT/gradlew" :desktop:packageDmg --console=plain
DMG="$(find "$ROOT/desktop/build/compose/binaries/main/dmg" -maxdepth 1 -type f -name '*.dmg' | head -n 1)"
if [[ -n "${DMG:-}" ]]; then
  cp "$DMG" "$ROOT/desktop/build/dist/Mexpa-macos-x64.dmg"
  echo "dmg: $ROOT/desktop/build/dist/Mexpa-macos-x64.dmg"
fi
