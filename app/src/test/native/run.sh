#!/usr/bin/env bash
# Run with JAVA_HOME pointing to a JDK, and TMPDIR outside the source tree.
set -euo pipefail
ulimit -c 0
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to a JDK containing include/jni.h}"
BUILD=$(mktemp -d "${TMPDIR:-${HOME}/.cache}/airplay-native-test.XXXXXX")
trap 'rm -rf "$BUILD"' EXIT
mkdir "$BUILD/uxplay"
git -C "$ROOT/app/src/main/cpp/third_party/UxPlay" archive HEAD | tar -x -C "$BUILD/uxplay"
for patch in "$ROOT"/app/src/main/cpp/patches/UxPlay/*.patch; do
    git -C "$BUILD/uxplay" apply --unidiff-zero "$patch"
done
"${CC:-cc}" ${CFLAGS:-} -std=gnu11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function \
    -Wno-incompatible-pointer-types -pthread \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    -I"$ROOT/app/src/test/native/stubs" -I"$BUILD/uxplay/lib" \
    -I"$ROOT/app/src/main/cpp" \
    "$ROOT/app/src/main/cpp/android_raop_callbacks.c" \
    "$ROOT/app/src/test/native/video_callbacks_test.c" -o "$BUILD/video_callbacks_test"
"$BUILD/video_callbacks_test" "$@"
