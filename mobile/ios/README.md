# Royal Reserve Bank for iOS

Native SwiftUI app: Dashboard, Accounts, Transaction, Assets and Settings. No
third-party dependencies, web views or signing required for simulator builds.
English UI with centralized localization under `Resources/Localization`.

## Requirements

- macOS and Xcode 16 or newer, iOS simulator runtime installed
- iOS 17.0 minimum deployment target; Swift 5 language mode
- Python 3 for the localization check
- XcodeGen only when changing `project.yml`: `brew install xcodegen`

The generated project and shared `RoyalReserveBank` scheme are committed. Open
`RoyalReserveBank.xcodeproj` and Run, or from this directory:

```sh
python3 scripts/check_localization.py
xcrun simctl list devices available
# Replace SIMULATOR_UDID with an available iPhone UDID from the list.
xcodebuild -project RoyalReserveBank.xcodeproj -scheme RoyalReserveBank \
  -destination 'platform=iOS Simulator,id=SIMULATOR_UDID' \
  -derivedDataPath build CODE_SIGNING_ALLOWED=NO test
xcrun simctl boot SIMULATOR_UDID # skip if already booted
xcrun simctl install SIMULATOR_UDID build/Build/Products/Debug-iphonesimulator/RoyalReserveBank.app
xcrun simctl launch SIMULATOR_UDID com.royalreservebank.ios
```

Regenerate after project changes with `xcodegen generate`. The project uses the
Xcode 16-compatible project format (objectVersion 77 or earlier).

## Mock and real API modes

Offline mock mode defaults **on**; it never needs network or authentication.
Accounts start from Alice/Bruno/Carla on each process launch, with in-memory
mutations, independent create counters and exact contract messages. Dashboard
and Accounts share an observable store, refetched after successful mutations.

Settings persists the runtime mock toggle, base URL and manually pasted token
in UserDefaults. Switching modes takes effect immediately and reloads accounts.
Base URL defaults to `http://localhost:8080`; Save trims whitespace and trailing
slashes. Use the gateway origin, **without `/api`**. The iOS simulator can use
localhost for a gateway on the Mac. Physical devices need a reachable host.
Arbitrary HTTP loads are allowed for demo gateways. Real API mode requires a
valid JWT for protected routes; there is no login or token refresh flow.

Save token stores trimmed text; Clear removes the saved value. No environment
token fallback exists in this native app. UserDefaults is intentionally used
per the demo contract, not secure credential storage for production banking.
HTTP errors retain status and raw text bodies; transport/decode failures are
ordinary errors. A 201 response containing `Oops` is shown as an error banner.

## Tests

XCTest covers mock seeds/counters/mutations/errors, URLSession request building
with URLProtocol stubs, raw errors, asset key normalization, currency display,
validation/parsing, settings persistence and shared account refresh. CI selects
an installed iPhone simulator dynamically and runs localization checks and tests.
No backend credentials are needed. Real gateway authentication/persistence must
be verified separately with a reachable gateway and valid pasted JWT.
