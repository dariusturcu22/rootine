# Frontend

The Rootine web app: a map of Romania where farmers will draw parcels, upload leaf photos, see the risk heatmap and get live alerts.

Stack: Vite, React, TypeScript, React Router, TanStack Query, Zustand, Leaflet and react-leaflet (with `leaflet.heat` and Geoman installed for later), Vitest, React Testing Library, MSW, ESLint and Prettier. In Docker it is built and served by nginx (`nginx/default.conf`), which also forwards `/api` to the backend.

## Structure

```
src/
  assets/  components/  config/  hooks/  layouts/  store/  utils/
  features/
    alerts/     components/ hooks/ store/
    auth/       api/ components/ store/
    diagnosis/  api/ components/
    maps/       components/ hooks/ utils/
    weather/    components/ utils/
  lib/          apiClient.ts (fetch wrapper with JWT), queryClient.ts
  test/         setup.ts, msw/handlers.ts, msw/server.ts
  types/        api.ts (generated from the backend OpenAPI)
nginx/default.conf
```

Tests live next to the file they test (`X.test.tsx`).

## Setup, run and test

See the [Frontend section of SETUP.md](../SETUP.md#frontend). Rules for working here: [AGENTS.md](AGENTS.md).
