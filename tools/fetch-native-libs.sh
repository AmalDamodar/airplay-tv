#!/usr/bin/env bash
# Fetches the native AirPlay engine (unchanged from upstream) into app/src/main/jniLibs, so the app can be built
# without the Android NDK. It downloads the official Shield AirPlay release, checks its checksum and unpacks the
# four native libraries per CPU type. To compile the engine from source instead, see the README.
set -eu
VERSION="v2.0.1-shield"
APK="Shield-AirPlay-2.0.1-shield.apk"
URL="https://github.com/ebreen/shield-airplay/releases/download/$VERSION/$APK"
SHA256="7e3a7365dd98a84531d9872856aedcbdbd4e80d5c9970481b1184826a05efbc1"

root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT

echo "Downloading $APK ..."
curl -fL# -o "$work/$APK" "$URL"
if command -v sha256sum >/dev/null 2>&1; then got="$(sha256sum "$work/$APK" | cut -d' ' -f1)"; else got="$(shasum -a 256 "$work/$APK" | cut -d' ' -f1)"; fi
[ "$got" = "$SHA256" ] || { echo "error: checksum mismatch, refusing to use the download" >&2; exit 1; }

for abi in arm64-v8a armeabi-v7a; do
  mkdir -p "$root/app/src/main/jniLibs/$abi"
  for lib in libairplay_native.so libc++_shared.so libcrypto.so liboboe.so; do
    unzip -o -q -j "$work/$APK" "lib/$abi/$lib" -d "$root/app/src/main/jniLibs/$abi"
  done
done
echo "Done: native libraries are in app/src/main/jniLibs."
