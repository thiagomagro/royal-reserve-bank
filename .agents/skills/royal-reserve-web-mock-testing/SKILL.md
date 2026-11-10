---
name: royal-reserve-web-mock-testing
description: Run Royal Reserve Bank frontend browser regression testing locally with the MSW mock backend, without Java services or authentication.
---

# Local frontend browser testing

## Devin Secrets Needed
None for MSW-backed frontend testing. Do not use a real JWT for localStorage tests.

## Setup
- From `web/`, use `source ~/.nvm/nvm.sh && nvm use 24`.
- Dependencies are declared in `package-lock.json`; install with `npm ci` if absent.
- Start `npm run dev -- --host 0.0.0.0`. Default port is 5173.
- `web/.env.development` sets `VITE_API_MOCK=true`; use `npm run dev`, not `dev:api`, for isolated UI tests.
- Settings must show `API mock mode: enabled`. No login or backend services are needed.

## State and expectations
- Mock account state is in-memory in the page, so full reloads reset fixtures. Use client-side navigation to verify mutation/query invalidation.
- Default fixtures: Alice Johnson (1001, USD 12500.50), Bruno Souza (1002, BRL 8400), Carla Mendes (1003, EUR 3200).
- Create and delete a uniquely named test account rather than deleting fixtures.
- PETR4 is available; an unknown code such as FOO42 is unavailable.
- A transaction needs an asset code and a positive integer value; successful mock submission shows `Transaction placed successfully.`.
- Settings stores token under localStorage `rrb.token`. Use a dummy string, check persistence after reload, then Clear token and verify null.
- Inspect console after every route for React warnings/errors; Vite connection, React DevTools informational, and MSW request logs are normal.

## Responsive checks
- Use Chrome responsive mode at exactly 390 CSS pixels and verify `innerWidth`, document scrollWidth, and body scrollWidth.
- Accounts may scroll horizontally inside its table container; distinguish that from whole-document overflow.
- Verify the mobile drawer opens, all routes are present, and selecting a route closes it.
- In touch emulation, input focus can lag tool-generated clicks. Confirm the field value before submitting; keyboard Tab focus is a useful fallback.
