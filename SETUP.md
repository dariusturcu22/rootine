# Setup

All setup, run, test and lint commands for Rootine are in this file. Every step is an exact command. Run commands from the repository root unless a step says `cd`.

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Git | 2.40 or newer | `git --version` |
| Docker Desktop (Compose v2) | running | `docker compose version` |
| JDK (Temurin) | 21 | `java -version` |
| Node.js | 24 LTS (npm 11) | `node --version` |
| Python | 3.11 (not 3.12 or newer, to match Docker and CI) | `py -3.11 --version` (Windows) or `python3.11 --version` |
| GitHub CLI (optional) | latest | `gh --version` |

You only need the tools of the part you work on, plus Docker Desktop. To run only the whole app, Docker Desktop is enough.

## Clone the repository

```bash
git clone https://github.com/DariusTurcu22/rootine.git
cd rootine
```

## Environment file

Copy the example. The values in it are safe development values. Never commit `.env`.

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

## Run everything with Docker

```bash
docker compose up --build
```

Wait until the containers are healthy. Check:

```bash
docker compose ps
```

| What | URL |
|---|---|
| App (map) | http://localhost |
| Backend health through nginx | http://localhost/api/actuator/health |
| Backend Swagger UI | http://localhost:8080/swagger-ui.html |
| AI service docs | http://localhost:8000/docs |
| RabbitMQ management (user and password in `.env`) | http://localhost:15672 |

Ports: web 80, backend 8080, ai-service 8000, db 5432, rabbitmq 5672 and 15672.

Stop everything (data in volumes is kept):

```bash
docker compose down
```

Stop and also delete the database and uploads volumes:

```bash
docker compose down -v
```

## Frontend

Folder: `frontend/`. Needs Node 24.

Install:

```bash
cd frontend
npm ci
```

Run (http://localhost:5173, `/api` is proxied to http://localhost:8080 with the `/api` prefix removed):

```bash
npm run dev
```

Test, lint, typecheck, build, format:

```bash
npm test
npm run lint
npm run typecheck
npm run build
npm run format
```

Watch mode for tests:

```bash
npm run test:watch
```

Generate API types from the backend OpenAPI document (the backend must be running on port 8080, for example with Docker). Never edit `src/types/api.ts` by hand:

```bash
npm run gen:api
```

## Backend

Folder: `backend/`. Needs JDK 21. The Maven wrapper is included; do not install Maven.

Start the database and the message broker:

```bash
docker compose up -d db rabbitmq
```

Run the backend. `JWT_SECRET` is required (at least 32 characters) and the app does not start without it. Other settings have development defaults that match `.env.example`.

Bash:

```bash
cd backend
export JWT_SECRET="dev-secret-for-local-use-only-0123456789"
./mvnw spring-boot:run
```

PowerShell:

```powershell
cd backend
$env:JWT_SECRET = "dev-secret-for-local-use-only-0123456789"
.\mvnw.cmd spring-boot:run
```

Test. This runs unit tests (`*Test`, surefire) and integration tests (`*IT`, failsafe). Integration tests start PostGIS and RabbitMQ with Testcontainers, so Docker must be running:

```bash
./mvnw verify
```

Format the code (CI fails if the code is not formatted):

```bash
./mvnw spotless:apply
./mvnw spotless:check
```

Useful URLs while it runs: http://localhost:8080/actuator/health, http://localhost:8080/swagger-ui.html, http://localhost:8080/v3/api-docs.

## AI service

Folder: `ai-service/`. Needs Python 3.11.

Create a virtual environment and install (Windows PowerShell):

```powershell
cd ai-service
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements-dev.txt
```

Linux or macOS:

```bash
cd ai-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
```

Run in mock mode (http://localhost:8000, docs at http://localhost:8000/docs). Without `model.onnx` the service is in mock mode anyway.

Bash:

```bash
AI_MOCK=true uvicorn app.main:app --reload --port 8000
```

PowerShell:

```powershell
$env:AI_MOCK = "true"
uvicorn app.main:app --reload --port 8000
```

Test and lint:

```bash
pytest
ruff check .
ruff format --check .
```

Fix formatting:

```bash
ruff format .
```

Download a trained model from a GitHub Release (not needed for mock mode):

```bash
python scripts/download_model.py --tag v0.1.0-demo1
```

Training dependencies (torch and others) are separate and are not needed to run or test the service:

```bash
pip install -r training/requirements-train.txt
```

## Common problems on Windows

| Problem | Fix |
|---|---|
| `docker compose` says the daemon is not running | Start Docker Desktop and wait until it says it is running. |
| Port 80 is already in use | Stop IIS or other web servers using the port, or change the web port mapping in `docker-compose.yml` to `"8081:80"` and open http://localhost:8081. |
| `.\.venv\Scripts\Activate.ps1` is blocked | Run once: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`. |
| `./mvnw` says `JAVA_HOME` is not defined correctly | Set `JAVA_HOME` to the JDK 21 folder (no extra quotes). Check with `echo $JAVA_HOME`. |
| Integration tests fail with a Docker or Testcontainers error | Docker Desktop must be running. The first run downloads images and can take minutes. |
| `mvnw: Permission denied` (Linux or macOS) | Run `chmod +x backend/mvnw`. |
| Scripts fail with strange `\r` errors | A file was checked out with Windows line endings. The repo forces LF in `.gitattributes`; run `git add --renormalize .` and check out again. |
| `pip install` fails for `onnxruntime` or `numpy` | You are probably on Python 3.12 or newer. Use Python 3.11. |
| `npm ci` is very slow or fails inside OneDrive | Keep the repository outside OneDrive-synced folders. |
| Path too long errors | Run once, as administrator: `git config --system core.longpaths true`. |
