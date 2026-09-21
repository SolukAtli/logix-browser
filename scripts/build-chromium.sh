#!/usr/bin/env bash
# Build the Logix Chromium targets for Android (Faz-4).
#
# Usage:
#   scripts/build-chromium.sh --src ~/chromium-logix/src [--target trichrome_webview] [--cpu arm64]
#
# Produces GN args: target_os="android", target_cpu="arm64",
# is_component_build=false, plus a $OUT_DIR symlinked for publish step.
set -euo pipefail

SRC=""
TARGET="trichrome_webview"   # or: chrome_public_apk
CPU="arm64"
OUT_NAME=""

usage() {
  echo "Usage: $0 --src <chromium/src> [--target trichrome_webview|chrome_public_apk] [--cpu arm64|x64]" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --src) SRC="$2"; shift 2 ;;
    --target) TARGET="$2"; shift 2 ;;
    --cpu) CPU="$2"; shift 2 ;;
    *) usage ;;
  esac
done
[[ -z "$SRC" ]] && usage
[[ "$TARGET" == "trichrome_webview" || "$TARGET" == "chrome_public_apk" ]] || usage

OUT_NAME="out/logix_${CPU}"
OUT_DIR="$SRC/$OUT_NAME"

if [[ ! -x "$SRC/buildtools/linux64/gn" && ! -x "$SRC/tools/gn" ]]; then
  echo "error: GN not found under $SRC (run sync-chromium.py with hooks first)" >&2
  exit 1
fi

# shellcheck disable=SC2086
GN_ARGS=$(cat <<EOF
target_os="android"
target_cpu="$CPU"
is_component_build=false
is_debug=false
is_official_build=false
symbol_level=1
android_static_analysis="off"
use_goma=false
EOF
)

echo "GN args for $OUT_DIR:"
echo "$GN_ARGS"
"$SRC/tools/mb/mb.py" gen -m logix_android -b "$TARGET" "$OUT_DIR" --config-file /dev/null 2>/dev/null \
  || gn gen "$OUT_DIR" --args="$GN_ARGS"

autoninja -C "$OUT_DIR" "$TARGET"

echo "Build done: $OUT_DIR/$TARGET"
