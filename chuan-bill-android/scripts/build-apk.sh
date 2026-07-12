#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ANDROID_ROOT"

# 从 build.gradle 提取版本号
VERSION=$(grep -m1 'versionName' app/build.gradle | sed 's/.*"\(.*\)".*/\1/')

if [ -z "$VERSION" ]; then
  echo "❌ 无法从 build.gradle 提取 versionName"
  exit 1
fi

echo "📦 开始构建 chuan-bill v${VERSION}..."

# 构建 Release APK
chmod +x gradlew
./gradlew assembleRelease --no-daemon

# 重命名 APK
APK_INPUT="app/build/outputs/apk/release/app-release.apk"
APK_OUTPUT="app/build/outputs/apk/release/chuan-bill-${VERSION}.apk"

if [ ! -f "$APK_INPUT" ]; then
  echo "❌ APK 未找到: $APK_INPUT"
  exit 1
fi

mv "$APK_INPUT" "$APK_OUTPUT"
echo "✅ APK 构建完成: $APK_OUTPUT"
