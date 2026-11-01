#!/usr/bin/env bash
# 构建 Linux 端覆写变换器（与 Android 共用 mishka_core/overrides 实现）。
set -euo pipefail
cd "$(dirname "$0")/transform"
GOFLAGS=-mod=mod go build -trimpath -o ../mexpa-transform .
echo "built: $(cd .. && pwd)/mexpa-transform"
