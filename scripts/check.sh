#!/usr/bin/env bash
# Read-only health check. Changes nothing on the TV.
#   ./scripts/check.sh [tv-ip]
set -eu
. "$(dirname "$0")/lib.sh"
[ $# -gt 0 ] && connect_if_address "$1"
require_device

ok=1
row() { printf '  %-34s %s\n' "$1" "$2"; }
bad() { ok=0; row "$1" "$2"; }

echo "AirPlay receiver check"
if ! is_installed; then
  bad "installed" "NO (run ./scripts/install.sh)"
  exit 1
fi
ver="$(tv dumpsys package "$PKG" | sed -n 's/.*versionName=//p' | head -1)"
row "installed" "yes, version ${ver:-unknown}"

overlay="$(tv appops get "$PKG" SYSTEM_ALERT_WINDOW | head -1)"
case "$overlay" in
  *allow*) row "display over other apps" "allowed" ;;
  *) bad "display over other apps" "not allowed ($overlay)" ;;
esac

if tv dumpsys deviceidle whitelist | grep -q "$PKG"; then
  row "battery whitelist" "yes"
else
  bad "battery whitelist" "no (run ./scripts/install.sh again)"
fi

if [ -n "$(tv pidof "$PKG" 2>/dev/null)" ]; then
  row "receiver running" "yes"
else
  bad "receiver running" "no (open the app once on the TV)"
fi

# The receiver listens on TCP 7000 by default.
if tv "cat /proc/net/tcp /proc/net/tcp6 2>/dev/null" | awk '$4=="0A" {print $2}' | grep -qi ':1B58$'; then
  row "listening on port 7000" "yes"
else
  bad "listening on port 7000" "no (open the app; check the port in its Settings if you changed it)"
fi

echo
echo "Cannot be checked from here: in the app, Settings, Advanced, Compatibility, 'Advertise AirPlay video support'"
echo "should be OFF (the default) and 'Advertise AirPlay audio support' ON (docs/SETTINGS.md)."
if tv pm list packages -e | grep -qx "package:$OLD_PKG"; then
  bad "original Shield AirPlay" "still enabled: it uses the same port (see ./scripts/install.sh output for how to switch it off)"
fi
[ "$ok" = 1 ] && echo "All checks passed." || { echo "Some checks failed, see above."; exit 1; }
