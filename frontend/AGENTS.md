# AGENTS.md - frontend

Read the [root AGENTS.md](../AGENTS.md) first. Its rules apply here, especially: no attribution, English only, tests with every change.

## Stack

Vite, React, TypeScript (6.0, pinned: typescript-eslint does not support 7 yet), React Router, TanStack Query, Zustand, Leaflet and react-leaflet, Vitest (jsdom), React Testing Library, MSW, ESLint (flat config) and Prettier. No UI component library yet; the team chooses it with the branding.

## Structure

See [README.md](README.md). Code is organized by feature in `src/features/<feature>/`. Shared code is in `components/`, `hooks/`, `lib/`, `utils/`.

## Commands

All commands are in the [Frontend section of SETUP.md](../SETUP.md#frontend). Before a PR: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.

## Local rules

- API calls go only through TanStack Query hooks in `features/*/api`, which use `lib/apiClient.ts`. No `fetch` in components.
- API types come from `src/types/api.ts`, generated with `npm run gen:api`. Never write or edit them by hand. If a type is missing, the backend contract must change first (see `docs/api/`).
- Use Zustand only for state that is really global (for example the auth token). Server data belongs to TanStack Query. Local UI state stays in components.
- Tests sit next to the file: `X.test.tsx`. Mock the network with MSW (`src/test/msw`); unhandled requests fail the test on purpose.
- Keep `.gitkeep` files in empty folders until they get real files.
- The browser only knows the path `/api`. Never hardcode `localhost:8080`.
