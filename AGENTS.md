# AGENTS.md — Shield AirPlay

This file is the durable development handoff for coding agents and contributors. Read it before changing the app.

## Product contract

Shield AirPlay is a GPLv3 AirPlay receiver for NVIDIA Shield and Android TV. Preserve these behaviors:

- No subscriptions, advertisements, analytics, telemetry, in-app purchases, or feature gates.
- Screen mirroring, URL/HLS video, and audio remain functional.
- The receiver starts at boot and runs as a `connectedDevice` foreground service.
- Explicit user stop remains authoritative. Sticky restart is only for background-enabled process recovery.
- Shield remote pause must keep the video session alive and resumable.
- Complete media URLs, credentials, private paths, query strings, and fragments must never enter logs.
- TV controls must remain directional-pad focusable and legible at distance.
- DRM-protected content is out of scope.

## Source and licence

- Base project: `jqssun/android-airplay-server`
- Upstream source remote: `https://github.com/jqssun/android-airplay-server.git`
- Licence: GPLv3. Preserve notices and provide corresponding source with distributed binaries.
- Native protocol stack: UxPlay submodule plus patches in `app/src/main/cpp/patches/UxPlay/`.

Do not attribute Shield-specific defects to the upstream project without reproducing them upstream.

## Architecture map

| Area | Purpose |
|---|---|
| `app/src/main/AndroidManifest.xml` | TV entry points, boot receiver, foreground service, permissions |
| `AirPlayService.kt` | Service lifecycle, native callbacks, MediaSession, playback state, logging |
| `ServiceRestartPolicy.kt` | Pure sticky-restart policy |
| `VideoStopPolicy.kt` | Narrow compatibility rule for iOS stop-after-local-pause |
| `LogSanitizer.kt` | Safe display/log representation of media locations |
| `MainViewModel.kt` | Compose state, persisted settings, service bridge, transport actions |
| `ui/MainScreen.kt` | Main navigation and active playback presentation |
| `ui/VideoControls.kt` | Shield remote controls and video overlay timeout |
| `ui/SettingsScreen.kt` | TV-focusable settings UI |
| `ui/theme/` | System/Light/Dark theme policy and Material color selection |
| `renderer/` | MediaCodec mirroring and Media3 URL/HLS playback |
| `app/src/main/cpp/android_raop_callbacks.c` | JNI/native callback bridge; never log raw media locations |
| `app/src/main/cpp/patches/UxPlay/` | Auditable patches applied to the pinned UxPlay submodule |
| `docker/Dockerfile` | Pinned Android SDK/NDK/CMake build environment |
| `SHIELD_BUILD.md` | Exact signed release and verification procedure |

## Development workflow

1. **Pin state.** Record `git status`, branch, commit, remotes, and submodule status. Do not overwrite unrelated changes.
2. **Trace behavior end to end.** For AirPlay changes, map native callback → service state → view model → Compose UI → remote input before editing.
3. **Use TDD.** Add a pure policy test first, run it and observe the expected failure, then write the minimum implementation. Avoid constructing the native-backed service in JVM tests.
4. **Run gates separately.** The combined native/test/lint/R8 graph can exceed an 8 GB host. Run unit tests, lint, and assembly as separate Gradle processes.
5. **Verify on Shield.** Exercise the exact user path, inspect filtered logcat and `dumpsys media_session`, then test one adjacent negative case. Do not interrupt an active playback session.

## Build environment

Pinned release toolchain:

- JDK 21 container runtime; Java/Kotlin bytecode target 17
- compile/target SDK 36; minimum SDK 24
- Android Gradle Plugin 8.10.1; Gradle 8.11.1
- NDK `27.0.12077973`
- CMake `3.22.1`
- Build Tools `35.0.0`
- ABIs: `arm64-v8a`, `armeabi-v7a`

Build the container:

```bash
docker build --pull=false -t shield-airplay-builder docker
```

Use a persistent Gradle cache and 1.5 GB daemon heap. Run these commands separately:

```bash
docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp -e GRADLE_USER_HOME=/gradle-cache \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/shield-airplay-gradle:/gradle-cache" \
  -w /workspace shield-airplay-builder \
  ./gradlew testDebugUnitTest --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'

docker run --rm --user "$(id -u):$(id -g)" \
  -e HOME=/tmp -e GRADLE_USER_HOME=/gradle-cache \
  -v "$PWD:/workspace" \
  -v "$HOME/.cache/shield-airplay-gradle:/gradle-cache" \
  -w /workspace shield-airplay-builder \
  ./gradlew lintRelease --no-daemon --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8'
```

For signed release assembly, follow `SHIELD_BUILD.md`. Signing material belongs outside the repository and is referenced only from ignored `local.properties`.

After any native build, restore generated submodule state:

```bash
git -C app/src/main/cpp/third_party/UxPlay reset --hard HEAD
```

The build task intentionally applies tracked patches inside that submodule; dirty UxPlay files after Gradle are generated state, not source changes.

## Test map

Current JVM policy suites cover:

- Service explicit start, null-intent recreation, opt-out, and sticky restart
- Media URL credential/path/query/fragment redaction
- iOS stop immediately after a Shield-local pause
- Theme mode parsing and System/Light/Dark resolution

Add tests near the pure policy they protect. A feature is not complete because an APK assembled.

## Device acceptance matrix

For playback changes, verify at least:

| Path | Required evidence |
|---|---|
| Direct AirPlay video | Picture and audio; Media3 player active |
| Shield remote pause/resume | Paused view persists >10 seconds; same remote resumes |
| Screen Mirroring | iPhone UI visible; hardware decoder active |
| Audio-only | Playback, metadata, and transport controls |
| Process/reboot recovery | Foreground service, port 7000, and mDNS receiver return |
| Theme change | Live switch, D-pad focus, persistence after app relaunch |

AirPlay discovery uses mDNS and normally requires sender and Shield on the same LAN. Tailscale is useful for authenticated ADB but does not transport multicast DNS by default.

## Release checklist

- [ ] Version code and name increased.
- [ ] Full unit suite passes with zero failures/errors/skips.
- [ ] `lintRelease` passes.
- [ ] Signed release assembles with pinned toolchain.
- [ ] `apksigner verify --verbose --print-certs` passes.
- [ ] `zipalign -c 4` passes.
- [ ] Packaged ABIs are only ARM64 and ARMv7.
- [ ] SHA-256 file matches the release APK.
- [ ] Installed package version and device-side APK checksum match.
- [ ] Exact feature works on NVIDIA Shield.
- [ ] Boot/discovery and pause/resume still work if lifecycle or playback code changed.
- [ ] UxPlay submodule is reset and clean.
- [ ] No key, password, `local.properties`, media URL, device log, screenshot, or private network identifier is committed.

## Known protocol edge case

When the Shield remote pauses URL/HLS video, iOS can send `POST /stop` within about one second. `VideoStopPolicy` ignores that stop only when the local player is paused and only inside a two-second grace window. Do not widen the window casually: later sender stops must still end the session.

## Common failure modes

1. **Dirty UxPlay after build:** reset the submodule; do not commit generated patch application.
2. **Gradle daemon disappears:** split tests, lint, and release assembly; keep `--max-workers=2` and `-Xmx1536m`.
3. **ADB connects but AirPlay is absent:** ADB/Tailscale reachability does not prove LAN mDNS discovery.
4. **Pause returns to app after five seconds:** inspect for an AirPlay stop immediately after `KEYCODE_MEDIA_PLAY_PAUSE`; preserve the narrow `VideoStopPolicy` rule.
5. **Theme looks correct only until relaunch:** ensure `ThemeMode` is persisted through `Prefs` and collected above `AirPlayTheme` in `MainActivity`.
