# AirPlay Receiver (glass)

An AirPlay receiver for Android TV with a clean, Apple-style Liquid Glass interface. Show your iPhone, iPad or Mac
screen on the TV, with sound. No Apple TV needed.

<p>
<img src="screenshots/overview.jpg" width="49%" alt="Overview: the status dot glows green when the receiver is ready">
<img src="screenshots/settings.jpg" width="49%" alt="Settings: the focused option is lit like a pane of glass">
</p>

This is a fork of [Shield AirPlay](https://github.com/ebreen/shield-airplay) by ebreen, which is itself a derivative of
[android-airplay-server](https://github.com/jqssun/android-airplay-server) by jqssun and uses
[UxPlay](https://github.com/FDH2/UxPlay) for the AirPlay protocol. The AirPlay engine is unchanged. What this fork
changes is the interface and the defaults. See [CHANGES.md](CHANGES.md).

> Not affiliated with Apple. "AirPlay" is a trademark of Apple Inc.

## What is different

- **New interface.** Frosted glass panels over a blurred wallpaper, Outfit font, light and dark themes. The option you
  have selected is lit like a pane of glass (no hard outline) and is never clipped. The status dot glows green when the
  receiver is ready or streaming, yellow while a device is connecting, and red when stopped or in error.
- **Minimal settings.** The first screen has only what most people need: the receiver name, start at boot, run in
  background, require a PIN, open on connect and the theme. Everything else is under **Advanced**.
- **Stable mirroring by default.** "Advertise AirPlay video support" is **off** out of the box. On the TV this was
  tested on, YouTube on the iPhone tried to hand its video to the TV, that failed and dropped the whole mirror. With
  video support off the iPhone keeps mirroring instead. The trade-off: the small AirPlay button inside a video app
  plays sound only; use **Control Center, Screen Mirroring** for the picture. You can turn it back on under
  **Advanced, Compatibility**.
- **Own app id** (`com.minimaltv.airplay`, label "AirPlay Receiver"), so it installs next to the original.
  Do not run both at once: they use the same port.

## Install

Easiest: use the installer script in [airplay-tv](https://github.com/AmalDamodar/airplay-tv), which also grants the
permissions and has a beginner guide for the TV and computer side.

Or by hand, with the TV's debugging switched on and `adb` connected:

```sh
adb install AirPlay-Receiver-glass.apk        # from the Releases page
adb shell appops set com.minimaltv.airplay SYSTEM_ALERT_WINDOW allow
adb shell dumpsys deviceidle whitelist +com.minimaltv.airplay
```

Open the app once. On the iPhone: **Control Center, Screen Mirroring, pick the TV's name** (the default name is "TV";
change it under Settings, Receiver). Both devices must be on the same Wi-Fi.

Not supported: DRM-protected video (Netflix, Disney+, Apple TV+), AirPlay 2 multi-room. Tested on one Xiaomi Mi TV
(Android 10) with an iPhone, mirroring YouTube, Shorts and other videos with sound.

## Build

You need JDK 17 and the Android SDK (Gradle downloads the rest). The native AirPlay engine is not rebuilt by default;
its prebuilt libraries come from the official upstream release:

```sh
tools/fetch-native-libs.sh          # downloads, checks the checksum, unpacks the libraries
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

For a signed release put `storeFile`, `storePassword`, `keyAlias` and `keyPassword` in `local.properties`
(never commit it) and run `./gradlew :app:assembleRelease`.

To compile the engine from source with the NDK instead, check out the submodules and pass `-PbuildNative`
(`git submodule update --init --recursive`, then `./gradlew :app:assembleRelease -PbuildNative`). I did not test this
path; upstream's [build notes](docs/UPSTREAM-README.md) and `SHIELD_BUILD.md` describe it.

## Legal

- Licence: GNU GPL v3, see [LICENSE](LICENSE). Complete corresponding source: this repository, plus the pinned
  submodules (UxPlay, FFmpeg, libplist, openssl-cmake) at the revisions in `SOURCE_REVISIONS.json`. The prebuilt native
  libraries in releases are the unmodified ones from upstream's `v2.0.1-shield` release.
- Upstream warns that UxPlay uses a reverse-engineered FairPlay compatibility component whose legal status is unclear.
  Check the laws that apply where you live.
- Fonts and images: see [NOTICE](NOTICE).
