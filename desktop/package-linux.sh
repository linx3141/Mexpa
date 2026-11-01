#!/usr/bin/env bash
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

# 打包用的 JDK 决定最终 runtime 的 glibc 下限。优先使用 Adoptium 21（GLIBC_2.15），
# 不要用发行版自带的新 JDK（Arch 的 JDK 27 需要 GLIBC_2.38，旧 Ubuntu/Debian 直接跑不起来）。
if [ -z "${JAVA_HOME_FOR_JPACKAGE:-}" ]; then
  for candidate in "$HOME"/.gradle/jdks/eclipse_adoptium-21-*/                    "$HOME"/.gradle/jdks/*21*/                    /usr/lib/jvm/temurin-21-* /usr/lib/jvm/java-21-*; do
    [ -x "${candidate%/}/bin/jpackage" ] || continue
    JAVA_HOME_FOR_JPACKAGE="${candidate%/}"
    break
  done
fi
if [ -z "${JAVA_HOME_FOR_JPACKAGE:-}" ] && [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
  JAVA_HOME_FOR_JPACKAGE="$JAVA_HOME"
fi
if [ -z "${JAVA_HOME_FOR_JPACKAGE:-}" ] || [ ! -x "$JAVA_HOME_FOR_JPACKAGE/bin/jpackage" ]; then
  echo "需要 JDK 21+（含 jpackage）；可通过 JAVA_HOME_FOR_JPACKAGE 指定（建议 Adoptium 21）" >&2
  exit 1
fi

MIHOMO_SRC="$ROOT/mihomo"
MIHOMO_BIN="$ROOT/mihomo-linux"
# with_gvisor：Linux TUN 的 gvisor/mixed 协议栈依赖该 build tag；mishka：订阅导入的 provider prefetch（与 Android 对齐）
if [ ! -x "$MIHOMO_BIN" ]; then
  (cd "$MIHOMO_SRC" && CGO_ENABLED=0 go build -tags "with_gvisor mishka" -trimpath -buildvcs=false -o "$MIHOMO_BIN" .)
fi

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}" \
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}" \
./gradlew :desktop:createDistributable \
  -Pdesktop.javaHome="$JAVA_HOME_FOR_JPACKAGE"

APP="$ROOT/desktop/build/compose/binaries/main/app/mexpa"
cp "$MIHOMO_BIN" "$APP/bin/mihomo"
chmod +x "$APP/bin/mihomo"

# 覆写变换器与 Android mishka_core/overrides 共用实现
TRANSFORM_BIN="$ROOT/desktop/mexpa-transform"
if [ ! -x "$TRANSFORM_BIN" ]; then
  (cd "$ROOT/desktop/transform" && GOFLAGS=-mod=mod go build -trimpath -o "$TRANSFORM_BIN" .)
fi
cp "$TRANSFORM_BIN" "$APP/bin/mexpa-transform"
chmod +x "$APP/bin/mexpa-transform"

rm -rf "$ROOT/desktop/build/linux-portable"
mkdir -p "$ROOT/desktop/build/linux-portable"
cp -a "$APP" "$ROOT/desktop/build/linux-portable/mexpa"
tar -C "$ROOT/desktop/build/linux-portable" -czf \
  "$ROOT/desktop/build/Mexpa-linux-x64-1.0.0.tar.gz" mexpa

echo "built: $ROOT/desktop/build/Mexpa-linux-x64-1.0.0.tar.gz"
