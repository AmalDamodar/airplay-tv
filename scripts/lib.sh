#!/usr/bin/env bash
# Shared helpers. Source this file; do not run it.
#
# Environment:
#   ADB             adb binary to use (default: adb)
#   ANDROID_SERIAL  device to talk to, e.g. 192.168.1.50:5555 (needed when more than one device is connected)
#   AIRPLAY_TV_CACHE  where the downloaded APK is kept (default: ~/.airplay-tv)

ADB="${ADB:-adb}"
CACHE="${AIRPLAY_TV_CACHE:-$HOME/.airplay-tv}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# The receiver app: AirPlay TV (a GPLv3 fork of Shield AirPlay), built from this repository. Pinned to the version this project was tested with.
PKG="com.minimaltv.airplay"
OLD_PKG="no.gior.shieldairplay"   # the original app; it uses the same port, so the two cannot run together
APP_NAME="AirPlay TV"
APP_VERSION="2.1.0"
APK_NAME="AirPlay-TV.apk"
APK_URL="https://github.com/AmalDamodar/airplay-tv/releases/download/v2.1.0/$APK_NAME"
APK_SHA256="6d05dd001765c6c982346c7b72d74727c449956d02a62338bc69053fd8dabd07"

die() { echo "error: $*" >&2; exit 1; }

# adb shell would otherwise read the stdin of any surrounding loop, so it always gets /dev/null.
tv() { "$ADB" shell "$@" </dev/null | tr -d "\r"; }

# If the first argument looks like an address ("192.168.1.50" or "192.168.1.50:5555"), connect to it.
connect_if_address() {
  case "${1:-}" in
    [0-9]*.[0-9]*.[0-9]*.[0-9]*)
      local addr="$1"
      case "$addr" in *:*) ;; *) addr="$addr:5555" ;; esac
      "$ADB" connect "$addr" || true
      ;;
  esac
}

require_device() {
  command -v "$ADB" >/dev/null 2>&1 || die "adb not found. Install it first (docs/INSTALL.md), or set ADB=/path/to/adb."
  local state
  state="$("$ADB" get-state 2>/dev/null || true)"
  [ "$state" = "device" ] || die "no single TV connected. Run: adb connect <tv-ip>:5555 and accept the prompt on the TV (docs/INSTALL.md). If several devices are connected, set ANDROID_SERIAL=<tv-ip>:5555 first."
}

is_installed() { tv pm list packages | grep -qx "package:$PKG"; }

# Hash of a file, on Linux, Git Bash and macOS.
sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d' ' -f1
  else die "no sha256sum or shasum found, cannot check the download"
  fi
}
