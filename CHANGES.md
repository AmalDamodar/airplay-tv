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
