#!/usr/bin/env bash
# Installs the AirPlay receiver on the TV and gives it the permissions it needs.
#   ./scripts/install.sh                   TV already connected with adb
#   ./scripts/install.sh 192.168.1.50      connect to that address first
#   ./scripts/install.sh --apk file.apk    install that file instead of downloading (its hash is not checked)
# Safe to run again: it reinstalls over the top and keeps the receiver's settings.
set -eu
. "$(dirname "$0")/lib.sh"

apk=""
while [ $# -gt 0 ]; do
  case "$1" in
    --apk) [ $# -ge 2 ] || die "--apk needs a file path"; apk="$2"; shift 2 ;;
    -h|--help) sed -n '2,6p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) connect_if_address "$1"; shift ;;
  esac
done
require_device

if [ -z "$apk" ]; then
  mkdir -p "$CACHE"
  apk="$CACHE/$APK_NAME"
  if [ ! -f "$apk" ] || [ "$(sha256_of "$apk")" != "$APK_SHA256" ]; then
    echo "1/4 Downloading $APP_NAME $APP_VERSION from its official release page..."
    curl -fL# -o "$apk" "$APK_URL" || die "download failed. Download $APK_NAME yourself from $APK_URL and run: ./scripts/install.sh --apk /path/to/$APK_NAME"
  else
    echo "1/4 Download already in $CACHE."
  fi
  [ "$(sha256_of "$apk")" = "$APK_SHA256" ] || { rm -f "$apk"; die "the downloaded file does not match the expected checksum, so it was deleted. Try again; if it keeps failing, see docs/TROUBLESHOOTING.md."; }
  echo "    Checksum OK."
else
  [ -f "$apk" ] || die "$apk not found"
  echo "1/4 Using $apk (checksum not checked)."
fi

echo "2/4 Installing on the TV (takes up to a minute)..."
"$ADB" install -r "$apk"

echo "3/4 Giving it the permissions it needs..."
# Draw over other apps: lets the receiver open itself when an iPhone connects.
tv appops set "$PKG" SYSTEM_ALERT_WINDOW allow || echo "    could not grant 'Display over other apps' (you can allow it in the app, see docs/INSTALL.md)"
# Not battery-limited, and allowed to run in the background: keeps the receiver reachable while the TV is on.
tv dumpsys deviceidle whitelist "+$PKG" >/dev/null || echo "    could not add it to the battery whitelist (not needed on every TV)"
tv cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND allow || true
# Android 13+ asks before showing notifications; older versions do not have this permission.
tv pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true

echo "4/4 Opening the app once, so it starts and registers itself..."
tv monkey -p "$PKG" -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1 \
  || tv monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

if tv pm list packages -e | grep -qx "package:$OLD_PKG"; then
  echo
  echo "Note: the original Shield AirPlay app is also installed. It uses the same port, so only one of the two can run."
  echo "To switch it off (undoable): adb shell pm disable-user --user 0 $OLD_PKG"
  echo "To switch it back on:        adb shell pm enable $OLD_PKG"
fi

cat <<EOF

Installed. The defaults already include the fix for iPhone mirroring (AirPlay video support off), so there is
nothing else to set. Optional, on the TV with the remote: open the app, Settings, Receiver, and change the
name your iPhone will show (the default is "TV").

On the iPhone: Control Center, Screen Mirroring, pick the TV's name.
Check everything with: ./scripts/check.sh   (settings explained: docs/SETTINGS.md)
EOF
