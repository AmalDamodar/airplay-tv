# Changes from Shield AirPlay 2.0.1-shield

Base: [ebreen/shield-airplay](https://github.com/ebreen/shield-airplay) tag `v2.0.1-shield`
(revision `b6e2caf`). Changes made in October 2026. The native AirPlay engine and the service, session and decoding
code are unchanged.

- New interface (Jetpack Compose): `ui/Glass.kt` (glass panels, wallpaper, colours), `ui/theme/Theme.kt`
  (monochrome colour schemes, Outfit typography), restyled `MainScreen.kt`, `ReceiverHero.kt`, `LogsScreen.kt`.
- `ui/SettingsScreen.kt` regrouped: Receiver, Connection and Appearance first; Compatibility, Display, Server and
  network, Decode, Audio sync and Diagnostics inside a collapsed Advanced section. The "Developer options" gate is
  gone; all options remain reachable.
- Defaults: "Advertise AirPlay video support" off, receiver name "TV".
- Optional focus scale for D-pad focus rings (`dpadFocus(shape, focusScale)`), so rings inside panels are not clipped.
- App id `com.minimaltv.airplay`, label "AirPlay Receiver", version `2.0.1-glass.1`.
- Build: native libraries can be taken from the upstream release (`tools/fetch-native-libs.sh`) instead of compiling
  them; `-PbuildNative` keeps the original CMake build.
- Upstream README moved to `docs/UPSTREAM-README.md`.

## 2.0.1-glass.2

- Focus style: the selected element is lit like a pane of glass (brighter translucent fill, thin specular edge, faint
  glow) instead of a hard outline (`ui/TvFocus.kt`, colours in `ui/Glass.kt`).
- Status chip: the dot glows green (ready or connected), yellow and breathing (a device is connecting) or red (stopped
  or error); new string `connecting_label`.

## 2.0.1-glass.3

- Simpler selection: a single soft rounded fill behind the focused row or button, no outline, glow or scaling (the
  Start/Stop button grows slightly). The menu icons use only the pill behind the icon.
- The default Android click overlay (a rectangle behind settings rows) is switched off for rows, toggles and the theme
  control.

## 2.0.1-glass.4

- Text settings (server name, port, buffer sizes) are plain rows that show the current value. OK opens a small dialog
  to edit it. Before, an inline text box sat in the list: it took the D-pad keys (the cursor moved instead of the
  focus) and brought up the on-screen keyboard over the list, which made the screen jump.

## 2.0.1-tv.1

- Renamed to AirPlay TV (app label, release file `AirPlay-TV.apk`). The installer scripts and guides that lived in a
  separate repository (`airplay-tv`) are now in this one (`scripts/`, `docs/`).
- No functional change to the receiver.
