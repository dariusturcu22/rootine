# 0006 - nginx inside the frontend image

## Context
The browser needs the React app and the API on the same origin, to avoid CORS problems. We want one public entry point.

## Decision
The frontend image is built in two stages: Node builds the app, then `nginx:alpine` serves it. nginx serves `/` (with fallback to `index.html` for the single page app) and forwards `/api/*` to the backend at `backend:8080`, removing the `/api` prefix. Upload size limit is 10 MB. SSE works because proxy buffering is off.

## Consequences
- The browser sees one origin, so no CORS setup is needed.
- The backend and AI service are not exposed to users in production.
- In development, Vite proxies `/api` the same way, so the paths are identical.
- nginx config lives in `frontend/nginx/default.conf` and is owned by the frontend team.
