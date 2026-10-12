# Admin access — initial server gate

`GET /v1/admin/session` verifies a Firebase ID token including revocation and checks the current user record's boolean `admin: true` custom claim. It returns `{ "role": "admin" }` only when allowed. The response has `Cache-Control: no-store`.

- Missing/invalid identity: 401.
- Valid identity without current admin permission: 403.
- Missing configuration or identity infrastructure failure: 503, no upstream error text.
- Other methods: 405; there is no API to grant roles.
- Query parameters, headers such as `X-Role`, and client UI state do not grant permission.
- Role removal is evaluated on the next request, even with the same still-valid token. There is an unavoidable in-flight window between checking a user record and returning a response; this gate must not be treated as a permanent session authorization.

The Go composition includes this route, with missing-config behavior matching counter routes. The current emulator server process started before this change must be restarted before testing the new route through that process.

Verification on 12/10/2026: unit tests cover boolean claim handling, disabled user, infrastructure errors and HTTP status/cache behavior; Go race suite and vet passed. A local Auth Emulator integration test creates disposable users, verifies 401/403, grants the claim only on its own test user, verifies 200 and cross-user 403, removes the role and verifies 403 with the original token. Cleanup only removes test-owned accounts/data. See `verification/go-admin-emulator-tests.txt`.

Do not treat a successful call to this endpoint as permission for future admin mutations; each endpoint must authorize independently. No cloud project, real admin account or production role was created.

## Next.js integration in progress — 12/10/2026

- `/admin/login` uses modular Firebase Web Auth and Google popup, with in-memory Firebase persistence. It is disabled without an existing web-app configuration and valid server origins.
- `/api/admin/session` accepts same-origin JSON POST only, bounds streamed input to 4096 bytes, and asks Go to authorize the ID token. The short ID token is placed in a HttpOnly/SameSite=Strict cookie for at most 55 minutes; no refresh token is placed in a cookie. HTTPS uses Secure and a __Host- cookie name. Plain HTTP is limited to explicit local mode and loopback origins.
- `/admin` is dynamic, reads the HttpOnly cookie on the server and rechecks Go on each server request. No token is rendered into HTML. `/api/admin/logout` verifies the origin before clearing the cookie. This removes the browser session, not every Firebase session on other devices.
- Fixed trusted backend origin only; no request-provided URLs, no redirect-following with credentials, no caching, bounded upstream timeout.
- Server role checks are not replaced by route/UI visibility. Client-side navigation history may preserve old UI; future metadata/mutations must reauthorize their own requests.
- Emulator Firebase settings require APP_ENV=local and demo-* project. No cloud project is created. Example settings are in `web/.env.example` and intentionally lack a real Firebase project.
- Firebase Web SDK 13.0.0 is pinned exactly. Three focused web security tests, TypeScript checking, and the Next.js production build pass. The build contains dynamic `/admin`, `/admin/login`, `/api/admin/session`, and `/api/admin/logout` routes.
- Integration test `TestNextAdminSessionAgainstFirebaseEmulator` passed against a loopback Next.js instance, the current Go API, and local Auth/Firestore emulators. It verified unauthenticated redirect, missing/cross-site origin rejection, streamed body limit, unknown JSON field rejection, ordinary-user denial, admin cookie protections, protected page access, immediate role removal with the same cookie, no token in rendered HTML, and logout cookie deletion.
- The Google popup itself remains unverified because no existing Firebase Web app configuration was supplied. Without it, `/admin/login` visibly reports that login is not configured rather than presenting a non-working sign-in control.

## Minimal support metadata

- `GET /v1/admin/users/{uid}/metadata` authorizes the current token and current `admin` claim on every request. It returns account state, normalized provider kinds, Firebase timestamps, whether a counter exists and its version, and the deletion-request state.
- The endpoint does not return email, phone number, display name, provider identifiers, counter payload, relationship names, dates, or other user content.
- Every authorized lookup attempt writes an `adminAudit` event with actor UID, target UID, action, outcome, and server timestamp. A failed audit write fails the lookup closed with 503.
- `/api/admin/lookup` is a same-origin Next.js bridge. It accepts exactly one UID field, reads at most 1024 streamed bytes, uses only the HttpOnly admin cookie, validates the Go response shape, disables caching, and never follows an upstream redirect with credentials.
- The admin page exposes this as a read-only support tool and tells the operator that every lookup is audited. Account mutation, role management, user-content viewing, and data export are outside this phase.
- All current integration verification uses disposable users in local Firebase Emulator Suite under the `demo-couple-app` label. It does not create or modify a Firebase cloud project.
