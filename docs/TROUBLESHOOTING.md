# Troubleshooting

Start with the health check. It changes nothing:

```sh
./scripts/check.sh
```

Connection problems between the computer and the TV (`unauthorized`, `offline`, timeouts) are in
[INSTALL.md](INSTALL.md#if-the-connection-fails). This page is about the receiver.

| Problem | What to do |
| --- | --- |
| The iPhone does not list the TV in Screen Mirroring | 1. Both devices on the **same** Wi-Fi or router (not a guest network, not one on 2.4 GHz and one on a separate network). 2. Run `./scripts/check.sh`: "receiver running" and "listening on port 7000" must say yes. 3. On the TV, open the app: Overview must say **Ready**; if it says Stopped, choose **Start**. 4. Turn Wi-Fi off and on again on the iPhone. 5. Some routers block device-to-device discovery ("AP isolation" or "client isolation"); switch that off in the router. A VPN on the iPhone can also hide the TV |
| It connects, then drops when YouTube opens | **Advertise AirPlay video support** must be **OFF** (the default; Settings, Advanced, Compatibility). See [SETTINGS.md](SETTINGS.md) |
| Sound plays on the iPhone, not the TV | **Advertise AirPlay audio support** must be **ON** (Settings, Advanced, Compatibility) |
| The AirPlay button in a video app only gives sound | Expected with video support off. Use **Control Center, Screen Mirroring** to show the picture |
| The iPhone sees the TV but connecting fails, and the original Shield AirPlay app is also installed | Both apps use port 7000. Switch the old one off: `adb shell pm disable-user --user 0 no.gior.shieldairplay` (undo with `pm enable`) |
| Netflix, Disney+ or Apple TV+ show a black screen or refuse | Copy-protected video cannot be mirrored. Use the app on the TV itself |
| The TV is not listed after it has been off or asleep | Check **Start server at boot** and **Run in background** are ON in the app, and run `./scripts/install.sh` again (it re-applies the battery and background permissions). Then restart the TV once |
| The receiver does not open on the TV when a phone connects | The app needs **Display over other apps**. `./scripts/check.sh` shows whether it is allowed. If not, run `./scripts/install.sh` again, or allow it in the TV's own Settings, Apps, the receiver, Permissions |
| `check.sh` says "listening on port 7000: no" but the app is running | The port can be changed in the app. If you changed **Server port**, that is expected |
| Checksum mismatch on install | The download was damaged and was deleted. Run `./scripts/install.sh` again. If it keeps failing, download the file from the [release page](https://github.com/AmalDamodar/airplay-tv/releases/tag/v2.2.0) yourself and run `./scripts/install.sh --apk /path/to/file.apk` |
| `INSTALL_FAILED_...` from adb | `INSTALL_FAILED_UPDATE_INCOMPATIBLE` means a copy signed by someone else is installed: run `./scripts/uninstall.sh`, then install again. `INSTALL_FAILED_INSUFFICIENT_STORAGE`: free space on the TV (uninstall unused apps) |
| The picture is jerky | Move the TV and iPhone closer to the router, or put the TV on Ethernet. Mirroring sends the whole screen over the network |

Still stuck? Open an issue on the repository and include the output of `./scripts/check.sh`, your TV model and Android
version (**Settings, Device Preferences, About**). Do not post your Wi-Fi name or password.
