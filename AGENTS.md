# AGENTS.md

Instructions for AI coding agents and humans working in this repository.

## Project

Rootine is a web app for farmers. They draw their parcels on a map, upload leaf photos and get a disease diagnosis from an ML model. They also see a weather-based disease risk map and get alerts (email and live in the app) when the risk is high. It supports all crops in the long run; Demo 1 covers only tomato and potato.

Team "Work in Progress (WIP)", course Proiect Colectiv, UBB FMI, 2026-27.

## Repository map

| Path | What it is | Details |
|---|---|---|
| `frontend/` | React + TypeScript + Vite, served by nginx | [frontend/AGENTS.md](frontend/AGENTS.md) |
| `backend/` | Java 21 + Spring Boot 4 REST API | [backend/AGENTS.md](backend/AGENTS.md) |
| `ai/` | Python FastAPI service (ONNX Runtime) | [ai/AGENTS.md](ai/AGENTS.md) |
| `docs/` | Architecture, ADRs, API contract, sprints, meetings | [docs/README.md](docs/README.md) |
| `docker-compose.yml` | Runs the whole stack | [SETUP.md](SETUP.md) |

All setup, run, test and lint commands are in [SETUP.md](SETUP.md). Do not copy them into other files; link to the section.

## Rules

1. **No attribution, ever.** No `Co-Authored-By` trailers, no "Generated with ..." lines, no emoji robot lines, no `Claude-Session` lines, and no mention of AI tools in commit messages, PR titles, PR bodies, issues or code. CI fails a PR that contains them.
2. Use the latest stable version of every tool and library, checked against official sources when you add it. Pin versions (`pom.xml`, `package-lock.json`, `requirements*.txt`).
3. Keep changes minimal. No fake features, no speculative code.
4. Code, comments, commit messages, PRs, issues and **all documentation in `docs/` are written in English**, in simple words.
5. Ask before any destructive or irreversible action (deleting files, force push, changing repository visibility).
6. Never change an API (endpoint, request or response) without updating `docs/api/` in the same PR.
7. No datasets, trained models or secrets in Git. Models are attached to GitHub Releases and downloaded by a script.
8. Every PR includes tests for what it changes.

## Architecture in short

- Browser talks to `web` (nginx in the frontend image). `/` serves the React app, `/api/*` goes to the backend. SSE also goes through `/api` with proxy buffering off.
- The AI service is internal only. The backend calls it: `/v1/predict`, `/v1/risk`, `/v1/crops`.
- The AI service fetches weather from Open-Meteo itself (1-hour cache) and computes disease risk with published rule models (no ML on weather). The backend has no weather module.
- Backend daily job: for each parcel it calls AI `/v1/risk`, stores the result and serves heatmap data. If the risk is high it publishes a message to RabbitMQ; a consumer sends an email and an SSE event.
- Uploaded photos are stored on a Docker volume; the database stores only the path.
- One image model for all crops (MobileNetV3-Large). The crop mask comes from the parcel's crop. Classes are read from `labels.csv`, never hardcoded. Serving uses ONNX Runtime (no PyTorch in the serving image).

## Git conventions

- Branches: `feat/<issue>-<short-name>`, `fix/...`, `docs/...`, `chore/...`, `test/...`.
- Commits: Conventional Commits with the folder as scope, for example `feat(backend): add parcel CRUD`.
- PRs: normal merge commit (no squash, no rebase), 1 approval, CI green, `Closes #<issue>` in the body.
- Release tags at demos: `v0.1.0-demo1`, `v0.2.0-demo2`, `v1.0.0`.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full workflow.
