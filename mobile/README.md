# Native mobile apps

Planned layout (applications are not scaffolded yet):

```text
mobile/
  android/   # Kotlin, Jetpack Compose
  ios/       # SwiftUI
```

Both applications use an English UI and reproduce the web's Dashboard,
Accounts (create, add funds, confirmed delete), New transaction, Assets and
Settings flows. Both must include an offline mock mode and Settings for a
reachable API base URL and a manually pasted Bearer token. Token paste is
intentional for this demonstration; an interactive login flow is not required.

The source of truth for endpoints, payloads, error handling, screen validation,
seed data and offline behavior is [the mobile API contract](../docs/mobile-api-contract.md).
It explicitly distinguishes existing web behavior from mobile configuration
requirements and backend behavior from mock behavior.

## Android

`android/` is a native Kotlin/Jetpack Compose app (Android 8+, JDK 17,
Android SDK 35). With `ANDROID_HOME` pointing to your SDK:

```sh
cd mobile/android
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on a device or emulator.
Offline mock mode defaults on; Settings can switch to a gateway (emulator host:
`http://10.0.2.2:8080`) and save a pasted Bearer token. Mock accounts are
in-memory and reset when the process restarts. Tokens are stored in private app
preferences for this demo; do not use production credentials. Debug builds use
the Android debug signing key. No shared web wrapper is used.
