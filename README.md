# XLine — Android SIP softphone (UDP, Kotlin)

Minimal native Android skeleton for calling over your hosted SIP trunk using
PJSIP's `pjsua2` API, with a fixed **UDP** transport.

## What's included
- `SipManager` — owns the PJSIP `Endpoint`, creates the UDP transport, registers the account
- `SipAccount` — registration + incoming-call callbacks
- `SipCall` — answer/decline/hangup/hold/DTMF + audio routing
- `SipCallService` — foreground service so the UDP registration + NAT keep-alive survive backgrounding
- `MainActivity` — enter SIP domain/username/password, register, dial a number
- `CallActivity` — in-call screen (answer/decline/hold/hangup)

## 1. Get a pjsua2 library for Android
This project does not bundle the PJSIP native binaries (they're a few hundred MB
of compiled C, and licensing/build flags vary by provider needs). You have two
options — pick one before building:

**Option A — quick start, community AAR** (already wired into `app/build.gradle`):
```gradle
implementation 'com.pjdroid:pjdroid:2.2.4'
```
This is a third-party build (not published by the PJSIP project). Check its repo
before shipping to production — verify it's still maintained and matches the
architectures your test devices use (armeabi-v7a / arm64-v8a).

**Option B — build PJSIP from source yourself (recommended for production):**
Follow https://trac.pjsip.org/repos/wiki/Getting-Started/Android to produce
`pjsua2.aar`, then:
1. Copy it to `app/libs/pjsua2.aar`
2. In `app/build.gradle`, remove the `pjdroid` line and uncomment:
   `implementation files('libs/pjsua2.aar')`

## 2. Fill in your SIP trunk details
Run the app, and on the main screen enter:
- **Domain** — your provider's SIP domain/IP (e.g. `sip.yourprovider.com`)
- **Username** — SIP auth username / extension
- **Password** — SIP auth password

If your provider needs a specific port (not 5060) or an outbound proxy/SBC,
edit `SipManager.SipCredentials` defaults or extend the UI to collect them.

## 3. Known UDP-specific caveats
- **No push wake-up.** Unlike TCP/TLS setups that some providers pair with a
  push notification service, plain UDP has no way to wake a killed app to
  signal an incoming call. `SipCallService` keeps the process (and the SIP
  registration) alive in the foreground — ask the user to **disable battery
  optimization** for the app, or Doze mode will eventually suspend it anyway.
- **NAT keep-alive** is set to 15s (`natConfig.udpKaIntervalSec`) to stop
  routers from dropping the UDP mapping. Lower it if calls stop arriving after
  a few minutes on a particular network; raise it to save battery once you've
  confirmed reliability.
- **Firewalls/carrier NAT**: some mobile carriers aggressively NAT UDP and can
  still drop it despite keep-alives. If incoming calls are unreliable on
  cellular data, that's the usual suspect — test on WiFi first to isolate it.

## 4. Next steps you'll likely want
- Persist SIP credentials (e.g. EncryptedSharedPreferences) instead of
  re-entering them every launch
- Wire `SipCallService` to actually hold a reference to the account/endpoint
  rather than just keeping the process alive (currently a minimal stub)
- Add ringtone + vibration on `onIncomingCall`
- Add ProGuard rules if you enable `minifyEnabled true` (PJSIP JNI bindings
  need explicit `-keep` rules or calls will crash in release builds)
