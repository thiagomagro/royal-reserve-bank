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

This foundation contains documentation only. Android/iOS projects, dependency
versions, build commands, signing and platform runtime verification will be
added with their respective implementations. No shared web wrapper is planned.
