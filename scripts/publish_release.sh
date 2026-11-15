#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <version_tag> <version_code> [release_notes]"
  echo "Example: $0 v1.1.0 2 '1-3 年级语数英 + /admin 管理后台 + 选择题确认'"
  exit 1
fi

TAG="$1"
VER_CODE="$2"
NOTES="${3:-基础学习 $TAG Release}"
VER_NAME="${TAG#v}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME_PATH="${JAVA_HOME:-/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home}"

cd "$ROOT_DIR/Learning"
JAVA_HOME="$JAVA_HOME_PATH" ./gradlew assembleRelease \
  -PappVersionCode="$VER_CODE" \
  -PappVersionName="$VER_NAME"

SRC_APK="$ROOT_DIR/Learning/app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$SRC_APK" ]; then
  echo "Release APK not found at $SRC_APK"
  exit 1
fi

OUT_APK="$ROOT_DIR/Learning/app/build/outputs/apk/release/BasicLearning-$TAG-release.apk"
cp "$SRC_APK" "$OUT_APK"
echo "Built signed Release APK: $OUT_APK"

if gh release view "$TAG" -R alphazhou741/Learning >/dev/null 2>&1; then
  gh release upload "$TAG" "$OUT_APK" -R alphazhou741/Learning --clobber
else
  gh release create "$TAG" "$OUT_APK" \
    -R alphazhou741/Learning \
    --target main \
    --title "基础学习 $TAG" \
    --notes "$NOTES"
fi
