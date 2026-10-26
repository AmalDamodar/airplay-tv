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

- Renamed to AirTV Play (app label, release file `AirTV-Play.apk`). The installer scripts and guides that lived in a
  separate repository (`airtv-play`) are now in this one (`scripts/`, `docs/`).
- No functional change to the receiver.

## 2.1.0

- New **Files** tab: receive files from a phone or computer in a browser (`files/FileReceiver.kt`, the page in
  `assets/receive.html`, the screen in `ui/FilesScreen.kt`). QR code made with ZXing core (Apache License 2.0). Files go
  to Downloads/AirTV Play through MediaStore. The server runs only while the tab is open and needs the PIN shown on the TV. The screen has the connection details
  on the left and the received files on the right; each received file can be opened with an app on the TV (adds the
  `REQUEST_INSTALL_PACKAGES` permission so an APK can go to the system installer). Per-file buttons: Open, Show in
  folder (hands the Downloads/AirTV Play folder to a file manager if the TV has one) and Delete (delete the file, or
  only remove it from the list).

### Files tab fixes (still 2.1.0)

- A transfer that died half way, or a file removed outside the app, left an entry in the media database that made the
  next upload with the same name fail ("UNIQUE constraint failed: files._data") before any data was read. The receiver
  now clears half-finished entries and entries whose file is gone, and never overwrites a finished file (it adds
  " (1)" to the name).
- The browser page asks the TV first (`/prepare`) so "not enough space" shows at once, and shows "No answer from the
  TV" if nothing arrives for 20 seconds.
- The accept loop no longer ends when a client resets a connection before it is accepted.
- The receiver stays up while the Files tab is selected even if Android rebuilds the screen, and closes after 30 minutes
  without use.

### Files tab (file manager) and Receive tab

- The receiving screen is now the **Receive** tab. A new **Files** tab (`ui/StorageScreen.kt`) browses internal storage
  and USB drives with the remote: OK opens, Back goes up, trash deletes after asking. "Show in folder" on the Receive
  tab opens it at Download/AirTV Play.
- Manifest: `READ/WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` (for Android 11+) and
  `requestLegacyExternalStorage` (honoured on Android 10). The file provider now covers any path, only to hand a file you
  opened to another app. Deleting also tells the media database, so no stale entries are left.
- The row button is now "..." with Copy, Move, Rename and Delete (last). Copy and Move wait for a destination: open the
  folder, then choose Paste here (a bar at the top shows what is waiting, with progress for big files and Cancel).
  Names that already exist get " (1)". The standard top-level folders can only be copied. The list refreshes when the
  app comes back to the front.

## 2.2.0

- The file manager (the old Files tab, `ui/StorageScreen.kt`) moved out of this app into its own app, **Files**
  (`com.minimaltv.files`, in the MinimalTV project). The rail now has Overview, Receive, Logs and Settings.
- **Show in folder** on the Receive tab starts the Files app at Download/AirTV Play (explicit intent with the extra
  `com.minimaltv.files.PATH`), or shows a message when Files is not installed.
- The app no longer asks for storage permissions (`READ/WRITE/MANAGE_EXTERNAL_STORAGE`, legacy storage). The FileProvider
  only serves the exported log file. Received files are still written and deleted through MediaStore.

## 2.3.0

- Renamed to **AirTV Play** (app label, release file `AirTV-Play.apk`, repository `airtv-play`, the folder received
  files are saved in: `Download/AirTV Play`). The application id `com.minimaltv.airplay` is unchanged so an update
  installs over the old app and keeps its settings. Files already received stay in `Download/AirPlay TV`; nothing is moved.
- New, compact **Home** screen: one slim status card on top (name, status, how to connect, Start/Stop), a wide card with
  the QR code, address and PIN, and the list of received files below it (scrolls). The separate Overview and Receive tabs
  are gone. While a phone is connected, the card area shows the picture or the audio player instead.
- **Logs** moved into Settings (one row). The navigation rail is now two icons, slimmer; text and padding are a little smaller.
- New launcher icon and banner: a screen with a play triangle, flat and minimal.

