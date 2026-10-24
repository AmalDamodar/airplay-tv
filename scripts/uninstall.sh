#!/usr/bin/env bash
# Removes the receiver from the TV and undoes what install.sh changed.
#   ./scripts/uninstall.sh [tv-ip]
set -eu
. "$(dirname "$0")/lib.sh"
[ $# -gt 0 ] && connect_if_address "$1"
require_device

if ! is_installed; then echo "$APP_NAME is not installed. Nothing to do."; exit 0; fi
tv dumpsys deviceidle whitelist "-$PKG" >/dev/null || true
"$ADB" uninstall "$PKG"
echo "Removed. Its settings went with it."
