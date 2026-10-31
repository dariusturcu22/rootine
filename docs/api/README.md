# API contract

This folder is the agreement between the parts (frontend, backend, AI).

## Rules

1. The contract is agreed in a PR that **both sides review before anyone codes**.
2. Any change to an endpoint (path, request, response, status codes) updates this folder **in the same PR** as the code.
3. After the code exists, the live version is generated from the code:
   - Backend: Swagger UI at `http://localhost:8080/swagger-ui.html` and the OpenAPI document at `http://localhost:8080/v3/api-docs`.
   - AI service: FastAPI docs at `http://localhost:8000/docs`.
4. The frontend never writes API types by hand. It generates them with `npm run gen:api` into `frontend/src/types/api.ts`.

## Files

| File | Contract between | Status |
|---|---|---|
| [ai-service.md](ai-service.md) | backend and AI service | First draft |
| [backend.md](backend.md) | frontend and backend | Empty structure, the backend team fills it in |

## Conventions

- JSON field names: `snake_case` in the AI service. The backend team decides for its own API and writes it in `backend.md`.
- Dates and times are ISO 8601, in UTC.
- Errors use normal HTTP status codes. `422` means the request body is invalid.
