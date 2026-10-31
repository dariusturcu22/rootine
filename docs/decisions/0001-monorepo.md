# 0001 - One monorepo

## Context
Rootine has a frontend, a backend and an AI service built by 8 people. They change together: an API change touches two parts at once.

## Decision
One Git repository with `frontend/`, `backend/` and `ai/`. `docker-compose.yml` and `docs/` are at the root.

## Consequences
- One PR can change an API and both sides of it, with one review and one CI run.
- CI uses path filters so it only runs the parts that changed.
- `CODEOWNERS` gives each team review rights on its own folder.
- All members see the same repo, issues and board.
