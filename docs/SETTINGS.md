# The settings, and why

The app keeps its settings inside itself, so they are changed on the TV with the remote: open the app, choose
**Settings** in the left menu. The first screen is deliberately short; everything else is under **Advanced**.

## The ones you may want to change

| Setting | Default | Where | Why |
| --- | --- | --- | --- |
| Server name | TV | Receiver | The name the iPhone shows in Screen Mirroring, for example "Living Room" |
| Start server at boot | on | Receiver | The receiver is ready after the TV starts |
| Run in background | on | Receiver | Keeps it reachable after you leave the app |
| Require PIN | off | Connection | Asks for a 4-digit PIN on every new connection |
| Open this application on connect | on | Connection | Shows the app when an iPhone connects (needs "Display over other apps", which the installer grants) |
| App theme | System | Appearance | System, Light or Dark |

## The fix that is built in: AirPlay video support is off

**Advanced, Compatibility, Advertise AirPlay video support** is **off** by default. **Advertise AirPlay audio support**
stays **on**.

When an iPhone sees a receiver that supports AirPlay **video**, apps like YouTube try a "hand-off": instead of mirroring
the screen, the app asks the TV to play the video itself. On the TV this was built on, that hand-off failed, and the
failure ended the whole session, so the picture on the TV froze or disappeared as soon as YouTube was opened.

With video support switched off, the iPhone does not try the hand-off. It keeps mirroring the screen, and YouTube, Shorts
and other videos play with sound on the TV.

**The trade-off:** the small AirPlay button inside a video app now sends audio only. To put the picture on the TV, use
**Control Center, Screen Mirroring** on the iPhone instead.

If you prefer the hand-off and your iPhone and TV handle it, turn video support back on there, then choose **Stop** and
**Start** on the Home screen so the receiver announces itself again.

## Things that were tried and were not the cause

On the test TV these did not change the problem, so you can leave them alone:

- Turning H.265 (HEVC) off.
- Turning "Allow new connections" off.

## If sound plays on the iPhone and not on the TV

Check that **Advertise AirPlay audio support** is **on**. With it off, the sound was sent to the phone in testing.

## What was verified

On the test TV the engine's settings were read from the app's own screen: video support off, audio support on, ALAC off,
AAC on, and mirroring of YouTube, Shorts and videos with sound from an iPhone was confirmed working by the TV's owner
with the original Shield AirPlay app. AirTV Play has the same engine and the same defaults, and mirroring from an iPhone
(including YouTube with sound) was confirmed with it on the same TV. Other TVs and other iPhone, iPad or Mac models were not tested.
