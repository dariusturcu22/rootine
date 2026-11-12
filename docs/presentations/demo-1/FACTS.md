# Demo 1 deck: facts and evidence

Every claim on the slides, with its evidence. Checked on 2026-11-11 against `origin/main` at `98375a5` (merge of PR #72), unless a row says otherwise. Status: **verified** (checked by command or by reading code), **documented** (stated in a repo document, not re-measured), **draft** (needs your confirmation).

## Slide 1: Rootine (10 s)

| Claim | Evidence | Status |
|---|---|---|
| Bolile plantelor se observă greu la început și costă mult dacă sunt găsite târziu | `README.md`, section "The problem" ("easy to miss at the start and expensive to fix late") | verified |
| Rootine găsește boala dintr-o fotografie și din vreme (risc meteo) | `README.md`, "Main features": leaf photo diagnosis and weather-based disease risk | verified |
| Work in Progress (WIP), Demo 1 | `README.md` "Team"; `docs/sprints/sprint-1.md` | verified |
| Notes only: 8 people, 3 teams (ML, backend, frontend) | `README.md` "Team" table (8 rows: 2 frontend, 3 backend, 3 AI) | verified |

## Slide 2: ML, diagnosis from a photo (15 s)

| Claim | Evidence | Status |
|---|---|---|
| Input: photo and crop; output: top 3 diagnoses, or an "incert" (unsure) answer | `docs/api/ai-service.md`; `docs/sprints/sprint-1.md` ("confidence_threshold = 0.60: ... returns status = unsure"); frontend shows `topPredictions` in `ImageUpload.tsx` | verified |
| One model for all crops, with a crop mask (notes only) | `docs/decisions/0002-one-model-crop-mask.md`; sprint-1.md ("crop logit masking (ADR 0002)") | verified |
| 13 classes in Demo 1 (tomato and potato: 3 potato, 10 tomato). The slide calls them "diagnostice"; the 13 include the healthy class of each crop, and so do the 38 | `ai-service/models/plant-v1/` (`labels.csv`, `crops.json`, `metadata.json`); sprint-1.md per-class table has 13 rows | verified |
| 38-class target | `labels.csv` has 38 classes; `docs/sprints/sprint-1.md` ("38-class PlantVillage catalog"); `WIP_Plan_ML.pdf` ("Până la Demo 2") | verified |
| 99,67% accuracy on 3.064 test images (macro-F1 also 99,67%) | `docs/sprints/sprint-1.md` ("Model Evaluation Summary": accuracy 0.996736, macro F1 0.996740, 3,064 held-out test images); PR #65 and PR #77 | documented, not re-measured (see "Metrics" below) |
| On field photos the numbers will be lower (said in the slide text, not as a footnote) | `docs/sprints/sprint-1.md` retrospective (Grad-CAM shows background shortcut learning on external photos); no field accuracy number exists | verified |
| Dataset credit: PlantVillage (Mohanty et al., 2016), CC BY-SA 3.0 | `ai-service/models/plant-v1/labels.csv` (license column); `docs/sprints/sprint-1.md` | verified |

### Metrics: source, date and caveats

- Source: `docs/sprints/sprint-1.md` (PR #77, merged 2026-11-11) and PR #65 (merged 2026-11-10). Split: 14,102 train, 3,146 validation, 3,064 test images; deduplicated and grouped (dHash), stratified; best epoch 7 of 7.
- I did **not** re-run the evaluation. The training data and the split files are not on this machine, and no `metrics.json` is committed. No per-crop numbers were computed. The per-class table is in sprint-1.md.
- Caveats: PlantVillage images are lab images; the numbers do not describe field photos. The temperature scaling factor is 1.0 (not calibrated).
- Model file: the Release `v0.1.0-demo1` is a **Draft** with one asset, `model.onnx` (16,864,750 bytes). Its SHA-256 (`a26fd1e2ebc956d5828d1b2b22aa7e3c859876ec4d6ae4be04f2b419445acca4`) was computed on 2026-11-11 and matches sprint-1.md, `metadata.json` and the Release digest. That `train.py` produced it is plausible (opset 17, same preprocessing) but not provable: no checkpoint or log is committed.

## Slide 3: ML, weather risk (15 s)

| Claim | Evidence | Status |
|---|---|---|
| Rules from published models: Hutton (late blight, potato and tomato) and TOMCAST (early blight and septoria on tomato) | `ai-service/app` (`hutton.py`, `tomcast.py`); PR #72 merged; sprint-1.md | verified |
| Hourly Open-Meteo data | `OpenMeteoClient` requests `temperature_2m`, `relative_humidity_2m`, `dew_point_2m`, `precipitation` hourly | verified |
| Risk level and reason per parcel (low, medium, high) | `POST /v1/risk` response has `level`, `risk_index`, `reason`; `docs/api/ai-service.md` matches code; backend `GET /parcels/{id}/risk` implemented in `RiskController` | verified; also called live through the full stack on 2026-11-11: both parcels returned level low, risk index 10 (14-day window) |
| Hutton rule (notes only): day counts if min temperature is at least 10 C and at least 6 hours with humidity of 90% or more; 2 such days in a row give high | `hutton.py`, sprint-1.md | verified |
| The "near" levels and TOMCAST thresholds are the project's own adaptation, not part of the published models | `hutton.py`, `tomcast.py` | verified (keep out of the voice-over) |
| Not shown as done: risk heatmap for all parcels (`GET /risk/heatmap` returns 501) | backend `RiskController` | verified |

## Slide 4: Backend (25 s)

| Claim | Evidence | Status |
|---|---|---|
| Spring Boot 4 (4.1.2), Java 21 | `backend/pom.xml` | verified |
| PostgreSQL with PostGIS, Flyway (4 migrations) | `docker-compose.yml` (`postgis/postgis:18-3.6`), `backend/src/main/resources/db/migration` | verified |
| nginx + React web container | `docker-compose.yml` (service `web`), ADR 0006 | verified |
| AI client to the AI service (FastAPI + ONNX Runtime) | package `app.aiclient` in the backend; `ai-service/` | verified |
| Authentication: stateless JWT (HS256). The only token issuer is `POST /auth/demo-token`, enabled only with `DEMO_TOKEN_ENABLED=true`. Register, login and `/me` are 501 stubs | `SecurityConfig`, `DemoTokenService`, `AuthController` | verified |
| 10 active endpoints: `GET /crops`, `POST/GET /parcels`, `PUT /parcels/{id}`, `POST/GET /diagnoses`, `GET /diagnoses/{id}`, `GET /parcels/{id}/risk`, `POST /auth/demo-token` | controllers, counted 2026-11-11 (16 contract endpoints plus the demo token; 7 are 501 stubs) | verified |
| 45 tests, all passing | `./mvnw -B verify` on a copy of `backend/` with Docker running, 2026-11-11: 26 unit and slice tests plus 19 integration tests (Testcontainers), `BUILD SUCCESS`. (Counting `@Test` annotations gives 46; 45 ran.) | verified |
| CI is green on main | `.github/workflows/ci.yml` (jobs `changes`, `frontend`, `backend`, `ai-service`, `no-attribution`, `ci-result`); latest runs on main for #72, #77, #79 succeeded (`gh run list`) | verified |
| Not shown as done: email registration, heatmap, alerts (RabbitMQ, email, SSE) | no scheduled job, no `RabbitTemplate`, no `SseEmitter` in the code; ADR 0005 and `docs/architecture/overview.md` describe them as design | verified |

## Slide 5: Frontend (25 s)

| Claim | Evidence | Status |
|---|---|---|
| Map with parcels; draw and edit parcels | `frontend/src/features/maps/components/MapView.tsx` (leaflet-draw, create and update parcel hooks); PRs #30, #79 | verified |
| Photo upload and diagnosis result with top predictions (notes) | `ImageUpload.tsx`, `DiagnosisPage.tsx`; PRs #36, #78 | verified |
| Risk view with legend and summary counts | `RiskView.tsx` (levels high, medium, low, not modelled) | verified |
| 30 frontend tests (notes) | `npm test` in `frontend/`: 8 files, 30 tests passed, 2026-11-11 | verified |
| Screenshots (map, risk, diagnosis result) | Real UI of the frontend served by nginx from the full stack: `docker compose -p rootine-demo up --build` on `origin/main` `98375a5` with `AI_MOCK=false`, the real `model.onnx` (SHA-256 `a26fd1e2...acca4`), `DEMO_TOKEN_ENABLED=true`. Parcels "Lotul din deal" (potato) and "Solarul de roșii" (tomato) were created with `POST /parcels`, 2026-11-11. Map imagery: Esri World Imagery (credit on the slide). No tokens or personal data visible | verified |
| Diagnosis result on the slide: late blight, 100% (0.9990), status confident, on a tomato parcel | Real run through the UI: `POST /diagnoses` with a PlantVillage sample photo (`raw/color/Tomato___Late_blight/005e3b43-9050-47da-9498-f9ecdcc703b3___RS_Late.B 5104.JPG` from github.com/spMohanty/PlantVillage-Dataset, CC BY-SA 3.0, 256x256, about 19 KB, not committed to the repo; only the screenshot is). Two more photos checked by API on 2026-11-11: potato early blight gave early_blight 0.9996, tomato healthy gave healthy 0.99999. Three photos are an anecdote, not a metric | verified (3 photos) |
| Alerts page is a placeholder ("No alerts in demo1") | `AlertsPage.tsx` | verified (not on the slide) |

## Slide 6: Demo

No claims. A cut card, 2 seconds.

## Slide 7: Until Demo 2 (45 s, spoken by Darius): DRAFT, TO CONFIRM

The list is a draft built from `WIP_Plan_ML.pdf` ("Până la Demo 2", Demo 2 on Saturday 12 December 2026) and from `WIP_Plan_Demo1.pdf` ("Nu facem înainte de Demo 1": email registration, heatmap, alerts, training on all 38 classes). It has 5 items; the plan has more (deploy with measured latency, a warning when the photo does not match the chosen crop, a replay mode for weather). Edit the `<li>` elements in `index.html`.

| Item on the slide | Source | Status |
|---|---|---|
| Toate cele 38 de clase din PlantVillage | `WIP_Plan_ML.pdf` ("reantrenăm pe toate cele 38 de clase imediat după Demo 1"); `WIP_Plan_Demo1.pdf` | draft |
| Test pe poze din câmp, nu doar de laborator | `WIP_Plan_ML.pdf` (field test set, at least 50 photos per crop, frozen about 29 November) | draft |
| Model mai bun pe poze reale | `WIP_Plan_ML.pdf` (background replacement, strong augmentation, fine-tuning on PlantDoc) | draft |
| Harta de risc pe date din /v1/risk | `WIP_Plan_ML.pdf` (risk map in the app); backend heatmap is a 501 stub today | draft |
| Înregistrare cu email și alerte | `WIP_Plan_Demo1.pdf` ("Nu facem înainte de Demo 1"); backend stubs are 501 today. Not in `WIP_Plan_ML.pdf`: this item is my addition for backend and frontend | draft |
| Date of Demo 2: 12 decembrie | `WIP_Plan_ML.pdf` | draft |

## Other facts I checked but did not put on a slide

- ai-service tests: 63 passed, including `test_real_model` with the Release model; `ruff check` and `ruff format --check` clean (run on a copy, Python 3.14, 2026-11-11).
- ONNX CPU latency 3.77 ms per image: from sprint-1.md, not re-measured.
- Merged PRs on main: 32; commits on main: 133 (2026-11-11).
- Inconsistencies found: PR #65 says `download_model.py` checks a SHA-256 and that `test_preprocess_parity.py` exists; neither is true in the code. PR #65 still has the placeholder `<device from metrics.json>`. The Draft Release is not downloadable without authentication.
