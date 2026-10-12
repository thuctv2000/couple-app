# Web admin session — 12/10/2026

- Firebase Web SDK: exact 13.0.0.
- `npm run test`: 3/3 pass.
- `npm run typecheck`: pass.
- `npm run build`: pass. Public pages remain static; admin login/session/logout routes and protected admin page are dynamic.
- Live local integration: Next.js `127.0.0.1:13001` → Go `127.0.0.1:18083` → Firebase Auth/Firestore Emulator.
- Go race integration `TestNextAdminSessionAgainstFirebaseEmulator`: pass. Full output: `next-admin-emulator-tests.txt`.
- Covered: unauthenticated redirect, origin validation, body limit, strict JSON shape, ordinary-user denial, role grant, protected server render, no ID token in HTML, role revocation with existing cookie, and logout cookie deletion.
- Live no-config UX: `/admin` returns 307 to `/admin/login`; login page says authentication is not configured.

Limits: no existing Firebase Web app configuration was provided, so the Google popup was not opened or authenticated. No cloud project or real admin account was created. Metadata lookup, support actions, per-action audit records, production deployment, and browser accessibility review of the admin UI remain.
