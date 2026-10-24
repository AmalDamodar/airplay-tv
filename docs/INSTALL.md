# Complete install guide

For someone who has never used a terminal. Allow about 15 minutes. Every step says what you should see, so you know it
worked before you go on.

You need: the TV (Android TV or Google TV), a computer on the **same Wi-Fi or router** (Windows, Mac or Linux), and
an iPhone, iPad or Mac to send the picture.

Contents: [1. TV](#1-on-the-tv) · [2. Computer](#2-on-the-computer) · [3. Connect](#3-connect-the-computer-to-the-tv) ·
[4. Install](#4-install-the-receiver) · [5. Settings](#5-set-the-receiver-up-on-the-tv) ·
[6. First mirror](#6-mirror-your-iphone) · [If the connection fails](#if-the-connection-fails) ·
[Undo](#undo-everything)

## 1. On the TV

Use the remote. Menu names differ a little between brands; the idea is the same.

1. **Connect to the network.** **Settings** (gear icon), **Network & Internet**, choose your Wi-Fi, type the password.
   The TV, the computer and the iPhone must all be on the **same** Wi-Fi or router. A "guest" Wi-Fi often blocks devices
   from seeing each other, so avoid it.
2. **Find the TV's IP address.** **Settings, Network & Internet**, press OK on your network. Write down the four
   numbers with dots, like `192.168.1.50`. If you do not see them: **Settings, Device Preferences, About, Status**.
3. **Open Developer options.** **Settings, Device Preferences, About** (Google TV: **Settings, System, About**). Scroll
   to **Build** (or **Build number**, **OS build**, **Android TV OS build**) and press **OK 7 times**. You should see
   "You are now a developer!"
4. Press **Back** once. **Developer options** is now in the list. Open it.
5. Turn **USB debugging** **ON**. If a switch called **Enable developer options** is shown, make sure it is ON too.
   If your TV has **Network debugging** or **Wireless debugging**, turn that ON as well.

Fire TV: **Settings, My Fire TV, About**, press OK on the device name 7 times, then **Settings, My Fire TV, Developer
options, ADB debugging** ON. (Fire TV is untested with this project.)

## 2. On the computer

You need two programs: **adb** (Google's tool for talking to a TV) and **git** (to download this project). Pick your
computer.

### Windows 10 / 11

1. Press the Windows key, type `PowerShell`, press Enter.
2. Paste these two lines (right-click in the window pastes), pressing Enter after each. Click **Yes** if Windows asks:
   ```powershell
   winget install --id Git.Git -e --source winget
   winget install --id Google.PlatformTools -e --source winget
   ```
   If it says `winget` is not recognised, install **App Installer** from the Microsoft Store and try again.
3. Close PowerShell. Press the Windows key, type `Git Bash`, press Enter. **Use Git Bash for everything below**:
   the scripts do not run in PowerShell.
4. Check adb is found:
   ```sh
   adb version
   ```
   You should see "Android Debug Bridge version ...". If it says command not found, close Git Bash and open it again
   (it only learns about new programs when it starts).

The Windows steps were not tested on a Windows PC.

### Mac

1. Press Cmd + Space, type `Terminal`, press Enter.
2. Install Homebrew (skip if `brew --version` already works). Paste this line, press Enter, type your Mac password if
   asked (nothing shows as you type), wait. At the end it prints **Next steps**: run the lines it shows, then close and
   reopen Terminal:
   ```sh
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
3. Install the tools:
   ```sh
   brew install git
   brew install --cask android-platform-tools
   adb version
   ```
   You should see "Android Debug Bridge version ...".

### Linux

Debian and Ubuntu: `sudo apt install adb git curl`. Fedora: `sudo dnf install android-tools git curl`. Arch:
`sudo pacman -S android-tools git curl`.

### Download this project (all computers)

```sh
git clone https://github.com/AmalDamodar/airplay-tv.git
cd airplay-tv
```

## 3. Connect the computer to the TV

Replace `YOUR_TV_IP` with the address from step 1.2:

```sh
adb connect YOUR_TV_IP:5555
```

Look at the **TV screen**. A box asks **Allow USB debugging?** Tick **Always allow from this computer** and choose
**OK**. (Mac: if macOS asks to let Terminal find devices on your local network, click **Allow**.)

Check:

```sh
adb devices
```

You must see your TV with the word **`device`** after it:

```
192.168.1.50:5555    device
```

If it says `unauthorized`, accept the box on the TV. If it shows nothing or an error, go to
[If the connection fails](#if-the-connection-fails).

## 4. Install the receiver

```sh
./scripts/install.sh
```

What you will see:

1. "Downloading ... from its official release page" and a progress bar (about 16 MB), then "Checksum OK". The script
   compares the download with a fixed fingerprint. If it does not match, the file is deleted and nothing is installed.
2. "Installing on the TV" then `Success`.
3. "Giving it the permissions it needs".
4. The app opens on the TV. If the original Shield AirPlay app is also installed, the script tells you how to switch it
   off, because both use the same port.

Safe to run again at any time. It keeps the receiver's settings.

## 5. Set the receiver up on the TV

Nothing is required: the app ships with the settings that keep iPhone mirroring stable ([SETTINGS.md](SETTINGS.md)).
Optional, with the remote:

1. In the app, move left to the menu and choose **Settings**.
2. At the top, change **Server name** to what your iPhone should show, for example "Living Room". The default is "TV".
3. Go back to **Overview**. It should say **Ready**. If it says Stopped, choose **Start**.

Careful with the remote on the Overview screen: the big button there is **Stop** while the receiver runs, so pressing OK
twice in a row stops it.

Then check from the computer:

```sh
./scripts/check.sh
```

Every line should say yes, allowed or listening.

## 6. Mirror your iPhone

1. iPhone on the **same Wi-Fi** as the TV.
2. Open **Control Center** (swipe down from the top right corner, or up from the bottom on older iPhones).
3. Tap **Screen Mirroring** and choose the TV's name.
4. The TV shows your screen. Open YouTube on the iPhone and play a video: the picture and sound should be on the TV.

To stop: Control Center, Screen Mirroring, **Stop Mirroring**.

## If the connection fails

| What you see | What it means and what to do |
| --- | --- |
| `failed to connect ... Operation timed out` or `Connection refused` | Wrong IP, TV off, or debugging not on. Check the IP again (the TV's address can change after a restart), confirm **USB debugging** (and **Network debugging**, if present) is ON, and that the computer is on the same Wi-Fi as the TV |
| `adb devices` shows `unauthorized` | Look at the TV and accept the **Allow USB debugging?** box. If it never appeared: in Developer options choose **Revoke USB debugging authorizations**, then run `adb disconnect` and `adb connect` again |
| `adb devices` shows `offline` | Run `adb disconnect` then `adb connect YOUR_TV_IP:5555` again. If it stays offline, switch the TV's USB debugging off and on |
| `adb: command not found` (or `'adb' is not recognized`) | adb is not installed or the window is old. Close the window, open a new one (on Windows: **Git Bash**), run `adb version` |
| Port 5555 refused although debugging is ON | Some TVs only offer wireless debugging with a pairing code: **Developer options, Wireless debugging, Pair device with pairing code**. Run `adb pair IP:PAIRING_PORT`, enter the code, then `adb connect IP:PORT` using the port shown on that screen |
| `more than one device/emulator` | Run `adb disconnect` to drop all, then connect only the TV, or set `ANDROID_SERIAL=YOUR_TV_IP:5555` before the script |

More problems with the receiver itself: [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Undo everything

```sh
./scripts/uninstall.sh
```

This removes the receiver and the permission changes. You can then switch **USB debugging** off again in Developer
options on the TV, which is a good idea once you are done, because anyone on your network could otherwise try to
connect to the TV.
