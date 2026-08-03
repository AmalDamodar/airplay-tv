# Shield AirPlay

[![CI](https://github.com/ebreen/shield-airplay/actions/workflows/ci.yml/badge.svg)](https://github.com/ebreen/shield-airplay/actions/workflows/ci.yml)
[![GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/ebreen/shield-airplay)](https://github.com/ebreen/shield-airplay/releases/latest)

A free, open-source AirPlay receiver tuned for NVIDIA Shield and Android TV. It has no subscriptions, advertisements, analytics, in-app purchases, or artificial feature gates.

Shield AirPlay is an independently maintained GPLv3 derivative of [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server), which uses [UxPlay](https://github.com/FDH2/UxPlay) for AirPlay/RAOP protocol support.

## Features

- iPhone, iPad, and Mac screen mirroring
- AirPlay URL/HLS video playback with Shield remote controls
- AAC-ELD, AAC-LC, and ALAC audio playback
- H.264 and H.265/HEVC hardware decoding
- Music metadata, artwork, and Android media-session controls
- Android TV directional-pad navigation and Picture-in-Picture
- **System, Light, and Dark app themes** with live switching
- Optional four-digit AirPlay PIN
- Automatic startup after boot
- Foreground-service operation and process-death recovery
- ARM64 and ARMv7 native libraries
- Privacy-safe media URL logging

## Install on NVIDIA Shield

1. Download `Shield-AirPlay-*.apk` from the [latest release](https://github.com/ebreen/shield-airplay/releases/latest).
2. Sideload it with a file manager or ADB:

   ```bash
   adb install -r Shield-AirPlay-1.1.0-shield.apk
   ```

3. Open **Shield AirPlay** once. Allow **Display over other apps** if you want incoming AirPlay sessions to open automatically.
4. Keep the iPhone and Shield on the same LAN. AirPlay discovery uses multicast DNS; ordinary Tailscale connections do not transport mDNS.
5. On the iPhone, use **Control Center → Screen Mirroring → Shield AirPlay**.

The receiver starts at boot by default. Its settings let you change the receiver name, PIN requirement, codecs, display mode, theme, and background behavior.

## Limitations

- DRM-protected video, including protected Apple TV content, is not supported.
- AirPlay 2 multi-room audio is not implemented.
- The sender and receiver normally need the same multicast-capable LAN.
- UxPlay uses a reverse-engineered FairPlay compatibility component. UxPlay describes its legal status as unclear. Review the laws that apply where you use or distribute this software.

## Build from source

Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/ebreen/shield-airplay.git
cd shield-airplay
```

### Reproducible Docker builder

```bash
docker build --pull=false -t shield-airplay-builder docker

docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e GRADLE_USER_HOME=/gradle-cache \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/shield-airplay-gradle:/gradle-cache" \
  -w /workspace \
  shield-airplay-builder \
  ./gradlew testDebugUnitTest lintRelease assembleDebug \
    --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release, create a keystore outside the repository and add these ignored entries to `local.properties`:

```properties
storeFile=/absolute/path/outside/repository/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run the separate test, lint, and release commands in [SHIELD_BUILD.md](SHIELD_BUILD.md). Never commit the keystore or `local.properties`.

> The Gradle native build temporarily applies patches inside the UxPlay submodule. Reset that generated worktree state after a build with `git -C app/src/main/cpp/third_party/UxPlay reset --hard HEAD`.

## Development

Read [AGENTS.md](AGENTS.md) before making changes. It documents the architecture, privacy and lifecycle invariants, test commands, release checks, and device acceptance matrix.

Important source areas:

- `AirPlayService.kt`: foreground service, callbacks, media session, receiver lifecycle
- `MainViewModel.kt`: UI state, persistent settings, player controls
- `ui/`: Compose UI, TV focus, video controls, themes
- `renderer/`: MediaCodec and Media3 playback
- `app/src/main/cpp/`: JNI bridge, UxPlay patches, native protocol integration

## Licence and credits

Shield AirPlay is licensed under the [GNU General Public License v3.0](LICENSE). Modified versions must remain GPL-compatible and make corresponding source available.

Major upstream work belongs to the contributors of:

- [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server)
- [UxPlay](https://github.com/FDH2/UxPlay)
- [FFmpeg](https://ffmpeg.org)
- [libplist](https://github.com/libimobiledevice/libplist)
- [Next Player](https://github.com/anilbeesetti/nextplayer)

This project is not affiliated with Apple Inc. AirPlay, iPhone, iPad, Mac, Apple TV, and related marks belong to Apple Inc.
