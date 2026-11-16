# Mobile API and behavior contract

This is the implementation reference for the native Android (Kotlin/Jetpack
Compose) and iOS (SwiftUI) demo apps. UI text remains English. This phase adds
documentation only; it does not implement either app or change the backend.

## Sources and scope

Derived from `web/src/api/{client,types,accounts,transactions,assets}.ts`,
`web/src/mocks/{handlers,store,browser,server}.ts`, the five corresponding
`web/src/pages/*Page.tsx` files, `web/src/components/ConfirmDialog.tsx`,
`web/src/lib/format.ts`, `web/src/main.tsx`, `web/vite.config.ts`,
`config-files/api-gateway*.properties`, gateway `SecurityConfig`, and the
controllers, DTOs and services under `account-api`, `transaction-api` and
`asset-management-api`. The public mobile surface is the six operations below,
not discovery/configuration infrastructure routes. Backend and mock differences
are intentional facts to preserve, not assumed equivalence.

## Connection, authentication and errors

- Web concatenates `VITE_API_BASE_URL` (default empty string) and the absolute
  API path verbatim. Its development server proxies `/api` to
  `http://localhost:8080`; the gateway listens on port 8080 and forwards paths
  without stripping `/api`. Native apps need a reachable gateway origin as
  their Settings base URL, without an `/api` suffix (and without a trailing
  slash to avoid double slashes). The browser proxy is not a native API host.
  Android emulator host-loopback is normally `10.0.2.2`, not `localhost`;
  real devices need a reachable host. No deployed base URL is prescribed here.
- Mobile Settings must allow base URL and pasted token configuration. This is
  a mobile requirement: **web Settings only edits the token**, not the base URL.
  No native sign-in or token acquisition flow is specified for this demo.
- Web uses `localStorage["rrb.token"]` if present, otherwise
  `VITE_API_TOKEN`. A nonempty token adds `Authorization: Bearer <token>` to
  every request. An explicitly saved empty token suppresses the environment
  fallback; clearing the saved token restores fallback behavior.
- JSON request bodies add `Content-Type: application/json`. GET has no body;
  there is no explicit Accept header. DELETE deliberately has a JSON body.
- All HTTP 2xx responses count as transport success. Non-2xx reads the entire
  body as text and raises `ApiError(status, body || statusText)`; do not assume
  a JSON error envelope, extract a `message` property, or discard the status.
  Framework/gateway errors may be JSON, text, or empty. Network and JSON decode
  failures are ordinary errors, not HTTP `ApiError`s. There is no refresh-token
  or special 401 retry behavior in `apiFetch`.
- Gateway security permits discovery paths but requires authentication for
  these API paths using JWT resource-server validation; invalid/missing tokens
  can fail before a controller (typically 401). Mock handlers do not validate
  tokens. Other framework/infrastructure failures have no app-specific status
  or body contract; display the raw response as above.

## Wire operations

Examples are JSON unless explicitly marked **plain text**. Monetary values
are JSON numbers, not strings; backend account amounts are `BigDecimal`.
IDs and account numbers are opaque strings, never numeric keys in the UI.
Account response `currency` is a string; create UI offers USD, EUR, GBP, BRL.

### GET `/api/account`

No request body. **200**, array (possibly `[]`):

```json
[{"id":"1","accountNumber":"1001","accountHolderName":"Alice Johnson","balance":12500.5,"currency":"USD"}]
```

No ordering guarantee on the backend. Backend reads a Redis-cached account
list or the repository; mutations invalidate that cache.

### POST `/api/account`

```json
{"accountHolderName":"Jane Doe","balance":0,"currency":"USD"}
```

**201**, **plain text**: `Successfully set up a new bank account for Jane Doe.`
No created account JSON is returned; reload the list. Backend generates an
account number with a country prefix and random digit groups, not the mock's
sequential number. There are no DTO validation annotations or explicit
controller validation/error mapping here; UI restrictions below are not a
promise that malformed requests get a particular 400 body. Mock simply adds
the supplied fields, without create validation or uniqueness checks.

### DELETE `/api/account`

```json
{"accountHolderName":"Jane Doe"}
```

Exact, case-sensitive holder-name match, first matching account, **not ID**.
Backend **200**, **plain text**: `Successfully deleted Jane Doe's account.`
Backend **404**, **plain text** for missing holder:
`The bank account information for Jane Doe was not found.`
The controller maps any runtime exception from deletion to 404 with its message.

Mock **200**: `Successfully deleted the bank account for Jane Doe.`
Mock **404**: `No bank account found for Jane Doe.` Both are plain text.

### POST `/api/account/{id}/deposit`

Percent-encode the ID as one path component.

```json
{"amount":25.5}
```

**200**, updated full account JSON:

```json
{"id":"1","accountNumber":"1001","accountHolderName":"Alice Johnson","balance":12526,"currency":"USD"}
```

**400**, **plain text**: `The deposit amount must be greater than zero.`
Backend rejects missing/null/nonpositive amount; mock rejects non-number or
nonpositive amount. Validation runs before ID lookup. **404**, **plain text**:
`No bank account found with id 1.` (using the requested ID).
Backend adds exact decimal amounts (null current balance is treated as zero);
mock rounds the resulting balance with `Math.round((balance + amount) * 100) / 100`.

### POST `/api/transaction`

```json
{"transactionItemsDtoList":[{"assetCode":"PETR4","assetName":"Petrobras PN","value":1}]}
```

No account ID or currency is sent. `value` is an integer (Java `int`), not an
account debit amount. Backend item DTO also has an optional `id` that web does
not send. **201**, **plain text** on backend success:
`Transaction completed successfully!`
Mock success text: `Transaction placed successfully.`

Backend checks asset availability, saves the transaction and publishes a Kafka
notification event. Unavailable assets throw internally; circuit-breaker/time
limiter/retry fallback returns **201**, **plain text**:
`Oops! Something went wrong, please try again later!`
Mock returns exactly that same 201 fallback if any requested code is unavailable.
Thus HTTP success alone is insufficient: the web displays any returned message
containing case-sensitive `Oops` as an error. Other successful messages are
success notifications. There is no transaction history/read endpoint here.

### GET `/api/asset-management?assetCode=PETR4&assetCode=AAPL`

Repeat URL-encoded `assetCode` query parameters in input order, not a JSON body.
**200**, array:

```json
[{"assetCode":"PETR4","isAssetAvailable":true},{"assetCode":"AAPL","isAssetAvailable":true}]
```

Accept both boolean keys: normalize using
`isAssetAvailable ?? assetAvailable ?? false` (false must not be overridden).
Backend DTO's Lombok boolean accessor can serialize as `assetAvailable`;
mock uses `isAssetAvailable`. Backend queries stored assets whose code is in
the input and marks available when stored `value > 0`; unknown codes can be
omitted entirely, with no promised input order or duplicate rows. Mock returns
one result per supplied code, including duplicates and unknown codes as false,
preserving order. Missing required query parameter can produce a framework 400;
mock accepts no codes and returns `[]`. No custom error schema is defined.

## Screen parity

Use equivalent native navigation and dialogs; do not introduce extra business
rules. Common account-query data is shared by Dashboard and Accounts; successful
create/delete/deposit invalidates/refetches it. Currency display uses en-US
currency formatting, with fallback `CODE 0.00` for an invalid currency.

### Dashboard (`/`)

- Loading: `Loading dashboard…`; failure: `Failed to load accounts: <message>`
  (`unknown error` if not an Error).
- Show account count and sum of balances **per currency**, no FX conversion.
- Top accounts: sort all accounts by raw numeric balance descending, take five;
  columns Holder, Account #, Balance. Even across currencies there is no conversion.
- Empty list shows count zero, no currency totals, empty top table.
- Actions: Manage accounts, New transaction, Check assets.

### Accounts (`/accounts`)

- Loading: `Loading accounts…`; same load-error prefix as Dashboard. Columns:
  Holder, Account #, Balance, Currency, per-row Add funds and Delete. Empty
  successful list renders the table headers without rows; no custom empty copy.
- New account dialog: Account holder name (required), Initial balance (required,
  numeric min 0, step 0.01, initial `0`), Currency (USD default; USD/EUR/GBP/BRL).
  Trim holder before sending; reject blank trimmed name, NaN balance or negative
  balance: `Provide a holder name and a balance of 0 or more.` Browser required/
  numeric constraints also apply. Create account becomes `Creating…` and disabled
  while pending. Cancel closes without resetting fields. Success shows server text,
  closes and resets to blank name, balance 0 and USD; refresh accounts.
- Add funds dialog: show selected holder/current formatted balance; amount in
  account currency, initially blank, required numeric min 0.01 step 0.01 and
  autofocus. Reject NaN or amount <= 0: `Provide an amount greater than 0.`
  Confirm deposit becomes disabled `Adding…` while pending. Cancel clears target
  and amount. Success closes/clears, refreshes and shows
  `Added funds to <holder>. New balance: <formatted balance>.`
- Delete opens **Delete account**, `Delete the account for <holder>?`, buttons
  Delete / Cancel. Cancel initially focused; Escape cancels. Cancel makes no
  request. Confirm sends holder name, immediately closes the confirmation, then
  shows server text on success and refreshes. No balance-zero requirement.
- Mutation failures show raw ApiError message, otherwise respectively
  `Failed to create account`, `Failed to add funds`, `Failed to delete account`.
  Create/deposit errors leave their dialog/data for retry.

### New transaction (`/transactions/new`)

- Start with one row: Asset code blank (required), Asset name blank (optional),
  Value `1` (required number, min 1, step 1), Availability initially unset.
- Add item appends that row. Remove is disabled for the only remaining row.
  Editing any row field clears that row's availability indicator.
- Check availability trims codes and removes blanks; if none, show
  `Enter at least one asset code first.` No uppercasing or deduplication.
  Disable check button as `Checking…` while pending. Map results by exact code
  to current rows (the row lookup uses the untrimmed field); missing results
  leave availability unset. Show Available / Unavailable badges for true/false.
- Submit is not gated on availability checks or badges. Browser requires each
  code/value and integer input constraints; submit handler filters blank trimmed
  codes, trims code/name, parses value with `parseInt(value, 10)`, rejects no
  parsed rows or nonpositive/noninteger parsed values with
  `Each item needs an asset code and a positive integer value.`
  This describes the actual handler, not stricter DTO validation.
- Disable submit as `Submitting…` while pending. Display returned message,
  treating `Oops` as error as above. Rows are not reset after success.
- Check errors: ApiError text or `Failed to check asset availability`;
  submit errors: ApiError text or `Transaction failed`.

### Assets (`/assets`)

- One text field, placeholder `PETR4, VALE3, AAPL`. Split input on whitespace
  or commas, trim, remove blanks. Do not uppercase/deduplicate. Empty input:
  `Enter at least one asset code.`
- Check availability disabled as `Checking…` while pending. Successful results
  replace prior results. Table Asset code / Status uses Available / Unavailable.
  Initially and for `[]`, no results table. Error preserves old results and
  shows ApiError text or `Failed to check asset availability`.

### Settings (`/settings`)

- Web shows `API mock mode: enabled/disabled`, computed from build environment
  `VITE_API_MOCK === "true"`, not a runtime toggle.
- API token (Auth0 JWT) multiline field starts from saved browser token, not
  environment fallback. Save token persists trimmed text (`API token saved.`).
  Clear token removes storage entry/clears field (`API token cleared.`).
  No token syntax validation or automatic authentication is performed.
- Native must additionally expose base URL configuration and provide an offline
  mock mode. Selection/persistence mechanics are implementation work, not existing
  web behavior. Keep token paste support; do not require interactive login.

## Equivalent offline mock

Implement these operations locally without network or authentication when mock
mode is active. Web starts MSW before rendering only when `VITE_API_MOCK=true`
(`web/.env.development`); `.env.api` disables it. Browser worker bypasses
unhandled requests. Native does not need a service worker.

Initial accounts, in this order:

```json
[
  {"id":"1","accountNumber":"1001","accountHolderName":"Alice Johnson","balance":12500.5,"currency":"USD"},
  {"id":"2","accountNumber":"1002","accountHolderName":"Bruno Souza","balance":8400,"currency":"BRL"},
  {"id":"3","accountNumber":"1003","accountHolderName":"Carla Mendes","balance":3200,"currency":"EUR"}
]
```

- Mutable in-memory store; web does not persist mock accounts to local storage.
  New process/module initialization restores seeds. `resetStore()` restores the
  three seeds and counters; it is a test helper, not a Settings action.
- Next ID `4`, next account number `1004`, both returned/stored as decimal
  strings. Increment independent counters on create, append to list; no reuse
  after delete. GET returns current list, including `[]` after all deletions.
- Create, deposit, delete statuses/messages and rounding are specified above.
  Removal selects the first exact holder match; duplicate holders are allowed.
- Available code set is exactly `PETR4`, `VALE3`, `AAPL`, `BOND-BR-2030`;
  matching is case-sensitive. Other codes are unavailable.
- Transaction checks `every` item code against that set. An empty item list
  therefore succeeds if called directly, though the UI blocks empty submission.
  No mock transaction history, account-balance mutation, event dispatch, or
  asset depletion occurs. Asset names/values do not affect mock availability.
- Mock handlers assume typed JSON for create/delete/transaction; they do not
  define a stable malformed-JSON error envelope or simulate outages/latency.
  Do not invent backend-like validation or network errors as mock parity.

## Implementation acceptance checklist

For each native app exercise normal, empty and failure states: seed Dashboard;
create account, cancel/confirm delete, valid/invalid/missing-ID deposit; empty
account list; available/unavailable assets and empty input/results; multi-item
transaction, bad values and HTTP-201 fallback; token save/clear and configured
base URL against a real gateway. Check raw non-2xx bodies and network failures.
Use offline mode to prove no backend dependency; use real mode separately to
cover JWT authentication and backend persistence. No native runtime checks have
been performed by this documentation-only phase.
