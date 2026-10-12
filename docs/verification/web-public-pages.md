# Public web pages — 12/10/2026

- Source: `web/src/app`, `web/src/features/landing`, `web/src/components/site-shell.tsx`.
- TypeScript: passed. Next.js production build: passed; `/`, `/help`, `/privacy` static.
- Local server: http://127.0.0.1:13000, not deployed.
- Desktop landing: visually inspected; hero, counter illustration and navigation visible.
- Mobile 390×844: landing and privacy inspected. DOM document clientWidth/scrollWidth both 375, no horizontal overflow.
- Landing help link opened `/help`; backup FAQ expanded via click and collapsed via Return; header data link opened `/privacy` with correct title and content.
- Temporary viewport override reset. Preview returned to home.
- Not a full accessibility audit. No screen-reader/device Safari verification. No admin authentication or production policy verification represented by these checks.
