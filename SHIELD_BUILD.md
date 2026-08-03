# Shield AirPlay build

`Shield AirPlay` is a personal NVIDIA Shield build of the GPLv3 project
[`jqssun/android-airplay-server`](https://github.com/jqssun/android-airplay-server).
It contains no subscriptions, advertisements, analytics, or in-app purchases.

## Source provenance

- Upstream commit: `3a19f474f965a3994d9c5ba0170cb4d111006779`
- UxPlay: `21eef8df25d91e12635c36d8176ad192725baca2` (`v1.73.6`)
- FFmpeg: `38b88335f99e76ed89ff3c93f877fdefce736c13` (`n8.1.2`)
- libplist: `f41b1ea67045e0c09339974d83e389972d84f166`
- openssl-cmake: `4edd36a8dab5f85a8f92650b5bdf6e0cab13aab8`
- Source date epoch: `1785758500`
- Licence: GPLv3. Preserve this source and licence when distributing the APK.

## Shield-specific changes

- Unique application ID: `no.gior.shieldairplay`
- App and default receiver name: `Shield AirPlay`
- ARM64 and ARMv7 native libraries for Shield TV variants
- Boot auto-start and background operation enabled by default
- Sticky service recovery when Android kills the process, only while **Run in background** is enabled
- Thirteen unit tests for service restart, opt-out behavior, log sanitization, and local-pause stop handling
- Media URLs are sanitized before logging; credentials, paths, queries, and fragments are removed
- Shield-remote pause remains resumable even when iOS immediately closes its AirPlay video connection

## Features

- iPhone, iPad, and Mac screen mirroring
- H.264 and H.265/HEVC hardware decoding
- AAC-ELD, AAC-LC, and ALAC audio
- AirPlay URL/HLS video playback and remote controls
- Music metadata, artwork, and transport controls
- Android TV remote navigation and Picture-in-Picture
- Optional four-digit PIN
- Resolution, frame-rate, overscan, latency, decoder, and debug controls
- Foreground service, multicast lock, wake lock, boot receiver, and automatic server start

## Important limits

- The sender and Shield must normally be on the same LAN/subnet because discovery uses mDNS.
  Tailscale does not carry multicast DNS by default. It is useful for ADB installation, not automatic AirPlay discovery.
- DRM video, including protected Apple TV content, is not supported.
- AirPlay 2 multi-room audio is not implemented.
- UxPlay uses a reverse-engineered FairPlay compatibility component. Its upstream project describes the legal status as unclear.

## Reproducible builder

The Dockerfile pins the base image by digest and installs these Android components:

- JDK 21
- Android SDK/API 36 and API-compatible Android Gradle Plugin 8.10.1
- Android Build Tools 35 and 36
- Android NDK `27.0.12077973`
- CMake `3.22.1`
- Gradle `8.11.1` from the checked-in wrapper

```bash
docker build --pull=false -t byte-airplay-builder:api36-ndk27 docker
```

Create a private signing keystore outside the repository and put its absolute path and credentials in ignored `local.properties` keys named `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.

Run tests, lint, and assembly as separate memory-limited Gradle processes. This avoids retaining the native compile, lint, and R8 graphs at the same time on an 8 GB build host.

```bash
export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e GRADLE_USER_HOME=/gradle-cache \
  -e SOURCE_DATE_EPOCH \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/byte-airplay-gradle:/gradle-cache" \
  -v "$HOME/.hermes/keys:$HOME/.hermes/keys:ro" \
  -w /workspace \
  byte-airplay-builder:api36-ndk27 \
  ./gradlew clean testDebugUnitTest --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'

docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp -e GRADLE_USER_HOME=/gradle-cache -e SOURCE_DATE_EPOCH \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/byte-airplay-gradle:/gradle-cache" \
  -v "$HOME/.hermes/keys:$HOME/.hermes/keys:ro" \
  -w /workspace byte-airplay-builder:api36-ndk27 \
  ./gradlew lintRelease --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'

docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp -e GRADLE_USER_HOME=/gradle-cache -e SOURCE_DATE_EPOCH \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/byte-airplay-gradle:/gradle-cache" \
  -v "$HOME/.hermes/keys:$HOME/.hermes/keys:ro" \
  -w /workspace byte-airplay-builder:api36-ndk27 \
  ./gradlew assembleRelease --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`.
