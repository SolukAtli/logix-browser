#!/usr/bin/env bash
# Publish locally built Trichrome/WebView artifacts to mavenLocal() (Faz-4).
#
# Usage:
#   scripts/publish-trichrome-local.sh --out ~/chromium-logix/src/out/logix_arm64 [--version 134.0.0-logix1]
#
# Installs .so files + Java/AAR outputs under com.logix.chromium so the
# Gradle build can consume them without a remote Maven repository.
set -euo pipefail

OUT=""
VERSION="134.0.0-logix1"

usage() {
  echo "Usage: $0 --out <gn-out-dir> [--version <ver>]" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out) OUT="$2"; shift 2 ;;
    --version) VERSION="$2"; shift 2 ;;
    *) usage ;;
  esac
done
[[ -z "$OUT" ]] && usage
[[ -d "$OUT" ]] || { echo "error: out dir not found: $OUT" >&2; exit 1; }

command -v mvn >/dev/null || { echo "error: 'mvn' (Maven) not found on PATH" >&2; exit 1; }

GROUP="com.logix.chromium"
REPO_URL="file://${HOME}/.m2/repository"

install_file() { # file artifactId packaging
  local file="$1" artifact="$2" packaging="$3"
  [[ -f "$file" ]] || { echo "skip (missing): $file"; return 0; }
  mvn -q org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
    -Dfile="$file" -DgroupId="$GROUP" -DartifactId="$artifact" \
    -Dversion="$VERSION" -Dpackaging="$packaging"
  echo "installed $GROUP:$artifact:$VERSION ($packaging)"
}

# Native libraries (per-ABI).
while IFS= read -r -d '' so; do
  abi="$(basename "$(dirname "$so")")"
  install_file "$so" "trichrome-webview-native-$abi" "so"
done < <(find "$OUT" -name "libmonochrome.so" -o -name "libtrichrome*.so" -print0 2>/dev/null || true)

# Java/AAR outputs.
while IFS= read -r -d '' aar; do
  base="$(basename "$aar" .aar)"
  install_file "$aar" "$base" "aar"
done < <(find "$OUT" -maxdepth 4 -name "*.aar" -print0 2>/dev/null || true)

# Gradle consumer snippet:
cat <<EOF

Add to settings.gradle.kts dependencyResolutionManagement:
    mavenLocal()

Depend on e.g.:
    implementation("$GROUP:trichrome-webview-native-arm64:$VERSION@so")
EOF
