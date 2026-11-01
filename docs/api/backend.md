# Backend API

The backend and frontend teams agree on this contract before coding each group (see [README.md](README.md) for the rules).

The live version is in Swagger UI: `http://localhost:8080/swagger-ui.html`.

In the browser all paths are under `/api` (nginx removes the prefix before the backend sees it). For example, the browser requests `/api/parcels` and the backend receives `/parcels`.

## Decisions

The backend team decided these points. The frontend adapts to them.

| Topic | Decision | Why |
|---|---|---|
| JSON field names | camelCase, for example `topPredictions`. The backend converts the AI service's snake_case names. | camelCase is the Java default, so no extra setup is needed. The frontend types are generated, so the style costs nothing there. |
| IDs | UUID, written as strings, for example `3b241101-e2bb-4255-8caf-4136c566a962`. | IDs cannot be guessed or counted, and they can be created without asking the database. JSON has no UUID type, so they are strings. |
| Dates and times | ISO 8601, in UTC, for example `2026-11-01T06:00:00Z`. | One format means one exact moment everywhere, and it sorts correctly as text. The frontend converts it to local time for display. |
| Errors | RFC 9457 Problem Details (`application/problem+json`) with `type`, `title`, `status` and `detail`. 422 means the request body is invalid. | A standard JSON error shape, so one handler covers every error. Spring supports it built in. |
| JWT | Issued by the backend. Claims: `sub` (the user ID), `iat` and `exp`. Valid for 1 hour. No refresh token in Demo 1. Sent as `Authorization: Bearer <token>`. | The backend already validates tokens with the shared secret, so issuing them is the simplest option. One hour limits the damage if a token is stolen. Without a refresh token, the user logs in again after expiry. |
| SSE authentication | The same `Authorization: Bearer` header. The frontend uses a fetch-based SSE client (for example `@microsoft/fetch-event-source`, or `fetch` with a stream reader). No token in the URL. | The browser's `EventSource` cannot send headers. A token in the URL would leak into logs and browser history, and a second login mechanism adds work. |
| Uploads | JPEG and PNG only, up to 10 MB. | These are the formats phones produce and the AI service's image library reads. 10 MB fits a phone photo and keeps huge files out. Other formats, such as HEIC, are rejected with 400. |
| Heatmap | One point per parcel, at the centre of the parcel. | The risk job calls the AI service once per parcel, with the parcel's centre. One point per parcel is the data that exists, and it is easy to draw as a coloured dot. |
| Alerts list | Not in Demo 1. Only the SSE stream and the email. | It saves an endpoint, a table and a screen, in line with the rule against speculative code. An alert sent while the app is closed is not visible in the app later, but the email remains. |

## Notes for the frontend

The points that affect the frontend most:

- **Authentication.** Send `Authorization: Bearer <token>` on every request except register and login. The token expires after 1 hour and there is no refresh, so a 401 means the user must go back to the login page.
- **Live alerts (SSE).** Plain `EventSource` cannot be used, because it cannot send the header. Use a fetch-based SSE client. nginx proxy buffering must stay off for this path.
- **Photo upload.** JPEG or PNG, up to 10 MB. nginx needs `client_max_body_size 10m`, because the default of 1 MB returns 413.
- **Coordinates.** Parcel shapes are GeoJSON polygons, where each position is `[longitude, latitude]`. Leaflet uses `[latitude, longitude]`, so the frontend swaps the order when drawing a parcel and when saving one.
- **Errors.** Every error has the Problem Details shape, so one handler can show `title` and `detail` for all of them.
- **Alerts history.** There is none in Demo 1. Alerts show live while the app is open, and the email is the record.

## Status codes

| Status | Meaning in this API |
|---|---|
| 200 | Request succeeded. Used for reads, login and parcel replacement. |
| 201 | A user, parcel or diagnosis was created. |
| 204 | A parcel was deleted successfully; no response body. |
| 400 | A multipart upload is missing a required part or contains an unsupported image format. |
| 401 | Authentication is missing, invalid or expired, or login credentials are incorrect. |
| 404 | The requested resource does not exist, belongs to another user, or has no stored risk result. |
| 409 | Registration conflicts with an existing email address. |
| 413 | The uploaded file exceeds 10 MB. |
| 422 | A JSON request body or parcel geometry is invalid. |
| 502 | The AI service is unavailable or returned an error while processing a diagnosis or risk request. |

Errors use `application/problem+json` and this shape:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "An account with this email already exists."
}
```

## Auth

Register and login are the only endpoints that do not need a token. Every other endpoint needs `Authorization: Bearer <token>` and returns 401 without it.

### POST /auth/register

Creates an account. It does not log the user in, so login comes next.

Request:

```json
{ "email": "ana@example.com", "password": "at-least-8-characters" }
```

The email must be a valid address. The password must have at least 8 characters.

Response 201:

```json
{ "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "email": "ana@example.com" }
```

Errors:

- 409 if an account with this email already exists.
- 422 if the email or the password is invalid.

### POST /auth/login

Request: the same body as register.

Response 200:

```json
{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 3600 }
```

`expiresIn` is in seconds. After that time the token is rejected with 401 and the user logs in again.

Errors:

- 401 for a wrong email or a wrong password. Both give the same answer, so nobody can learn which emails have an account.
- 422 if the body is invalid.

### GET /me

Returns the user the token belongs to.

Response 200:

```json
{ "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "email": "ana@example.com" }
```

Error: 401 for a missing, invalid or expired token.

## Crops

The frontend never hardcodes the crop list. The backend returns the list from the AI service (`GET /v1/crops`).

### GET /crops

Response 200: the enabled crops. In Demo 1: tomato and potato.

| Field | Meaning |
|---|---|
| crop | Crop name, lowercase |
| diagnosisSupported | `true` if photo diagnosis works for this crop |
| weatherSupported | `true` if at least one weather rule exists for this crop |
| supportLevel | `lab_and_field_tested`, `lab_only` or `not_supported` |
| weatherRules | Names of the weather rules used, for example `hutton` |
| classes | Disease names the model can output for this crop (`healthy` is a class) |

```json
[
  {
    "crop": "potato",
    "diagnosisSupported": true,
    "weatherSupported": true,
    "supportLevel": "lab_only",
    "weatherRules": ["hutton"],
    "classes": ["early_blight", "healthy", "late_blight"]
  }
]
```

If the AI service cannot provide the list, the backend returns 502.

## Parcels

A parcel has one crop and one polygon. The crop is used for the diagnosis and the risk. Every endpoint here needs a token. A user sees only their own parcels.

Parcel object:

```json
{
  "id": "3b241101-e2bb-4255-8caf-4136c566a962",
  "name": "North field",
  "crop": "potato",
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[23.59, 46.77], [23.60, 46.77], [23.60, 46.78], [23.59, 46.78], [23.59, 46.77]]]
  },
  "createdAt": "2026-11-01T08:00:00Z"
}
```

| Field | Meaning |
|---|---|
| id | UUID, set by the backend |
| name | Non-blank text, up to 100 characters |
| crop | A lowercase crop name, for example `potato`. The list comes from `GET /crops` |
| geometry | A GeoJSON `Polygon`, in WGS 84. Each position is `[longitude, latitude]` |
| createdAt | ISO 8601, in UTC, set by the backend |

The backend does not check `crop` against the AI service, so a parcel can be saved with a crop that is not supported yet. Its diagnosis then returns `crop_not_supported` and its risk returns `not_modelled`.

The polygon must be valid: a closed ring (the first and the last position are the same), at least 4 positions, edges that do not cross, and every position inside longitude -180 to 180 and latitude -90 to 90. Only `Polygon` is accepted, not `MultiPolygon`.

| Method and path | Request | Response |
|---|---|---|
| `POST /parcels` | `name`, `crop`, `geometry` | 201, the parcel |
| `GET /parcels` | | 200, a list of the user's parcels, newest first |
| `GET /parcels/{id}` | | 200, the parcel |
| `PUT /parcels/{id}` | `name`, `crop`, `geometry` | 200, the parcel |
| `DELETE /parcels/{id}` | | 204 |

`PUT` replaces all three fields, so none can be left out. The list has no pagination in Demo 1. Deleting a parcel also deletes its diagnoses and its stored risk results.

Errors:

- 401 for a missing, invalid or expired token.
- 404 if the parcel does not exist or belongs to another user. Both give the same answer, so nobody can learn which IDs exist.
- 422 if the body is invalid: a blank or too long `name`, a missing `crop`, or an invalid polygon.

## Diagnoses

A diagnosis is the result of one photo sent to the AI service for one parcel. Every endpoint here needs a token. A user sees only their own diagnoses.

### POST /diagnoses

Request: `multipart/form-data`

| Part | Required | Meaning |
|---|---|---|
| `image` | yes | The leaf photo. JPEG or PNG, up to 10 MB |
| `parcelId` | yes | The parcel the photo belongs to. Its crop is sent to the AI service |

The backend checks the file by its content, not only by its name or declared type. It saves the photo on the uploads volume under a generated name, keeps only the path in the database, and calls `POST /v1/predict` with the parcel's crop. Demo 1 has no endpoint that returns the photo.

Response 201: the diagnosis (see below). Every answer from the AI service is saved and returned, including `invalid_image` and `crop_not_supported`, because they are valid results and not errors.

Errors:

- 400 if `image` or `parcelId` is missing, or the file is not a JPEG or PNG image.
- 401 for a missing, invalid or expired token.
- 404 if the parcel does not exist or belongs to another user.
- 413 if the file is larger than 10 MB.
- 502 if the AI service is not available or returns an error, including its 501. Nothing is saved.

### The diagnosis object

| Field | Values and meaning |
|---|---|
| id | UUID, set by the backend |
| parcelId | The parcel the photo belongs to |
| status | `confident`, `unsure`, `invalid_image`, `crop_not_supported` |
| crop | The crop used |
| cropCheck | `ok`, `mismatch`, or `null` when no check was possible (`invalid_image`, `crop_not_supported`) |
| topPredictions | 3 items: `label` (`<crop>_<disease>`), `disease`, `probability` (0 to 1). Empty list when `status` is `invalid_image` or `crop_not_supported` |
| supportLevel | `lab_and_field_tested`, `lab_only`, `not_supported` |
| modelVersion | For example `plant-v1` |
| createdAt | ISO 8601, in UTC, set by the backend |

The values of `status`, `cropCheck`, `topPredictions` and `supportLevel` are the same as in the AI service. Only the field names change, from snake_case to camelCase.

```json
{
  "id": "9a1c7f52-6b3e-4c1a-9d0e-1f2a3b4c5d6e",
  "parcelId": "3b241101-e2bb-4255-8caf-4136c566a962",
  "status": "confident",
  "crop": "potato",
  "cropCheck": "ok",
  "topPredictions": [
    { "label": "potato_early_blight", "disease": "early_blight", "probability": 0.92 },
    { "label": "potato_late_blight", "disease": "late_blight", "probability": 0.05 },
    { "label": "potato_healthy", "disease": "healthy", "probability": 0.03 }
  ],
  "supportLevel": "lab_only",
  "modelVersion": "plant-v1",
  "createdAt": "2026-11-01T08:05:00Z"
}
```

### GET /diagnoses

Optional query: `parcelId`, to list only the diagnoses of one parcel.

Response 200: a list of diagnosis objects, newest first. There is no pagination in Demo 1.

Errors:

- 401 for a missing, invalid or expired token.
- 404 if `parcelId` is given and the parcel does not exist or belongs to another user.

### GET /diagnoses/{id}

Response 200: one diagnosis object.

Errors:

- 401 for a missing, invalid or expired token.
- 404 if the diagnosis does not exist or belongs to another user.

Demo 1 has no endpoint to change or delete a diagnosis. Diagnoses are deleted together with their parcel.

## Risk and heatmap

The backend stores the latest risk result returned by the AI service (`POST /v1/risk`) for each parcel. A daily job refreshes these results. The backend does not compute weather risk itself.

### GET /parcels/{id}/risk

Returns the latest stored risk result for the parcel.

Response 200:

```json
{
  "parcelId": "3b241101-e2bb-4255-8caf-4136c566a962",
  "crop": "potato",
  "level": "medium",
  "riskIndex": 42,
  "reason": "Mock response: fixed medium risk, real weather rules are not implemented yet.",
  "diseases": [
    { "disease": "late_blight", "model": "hutton", "level": "medium", "riskIndex": 42 }
  ],
  "generatedAt": "2026-11-01T06:00:00Z"
}
```

For a crop without a weather model, `level` is `not_modelled`, `riskIndex` is 0 and `diseases` is empty.

Errors:

- 401 for a missing, invalid or expired token.
- 404 if the parcel does not exist, belongs to another user, or has no stored risk result yet.

### GET /risk/heatmap

Returns one point for each of the user's parcels that has a stored risk result, using the parcel centre and its latest result. Parcels not yet processed by the daily job are omitted.

Response 200:

```json
[
  {
    "parcelId": "3b241101-e2bb-4255-8caf-4136c566a962",
    "name": "North field",
    "crop": "potato",
    "latitude": 46.775,
    "longitude": 23.595,
    "level": "medium",
    "riskIndex": 42,
    "generatedAt": "2026-11-01T06:00:00Z"
  }
]
```

An empty list is returned when the user has no parcels with stored risk results.

Errors:

- 401 for a missing, invalid or expired token.

If the AI service is unavailable or returns an error while a risk result is being generated, no new result is stored for that parcel. The existing result, if any, remains available. A risk-generation failure does not change the response from this read endpoint.

## Alerts (SSE stream)

There is no alerts list in Demo 1. When a parcel's risk is high, the daily risk job publishes an alert message to RabbitMQ. A consumer sends an email and pushes a live event to the user over SSE. This stream does not replay events sent while the user was disconnected.

### GET /alerts/stream

Opens an authenticated server-sent events stream. The request must include `Authorization: Bearer <token>`. The response content type is `text/event-stream`. The frontend must use a fetch-based SSE client because browser `EventSource` cannot send the authorization header.

Each alert is sent as an event named `risk-alert`. Its `data` is a JSON object:

```text
event: risk-alert
data: {"parcelId":"3b241101-e2bb-4255-8caf-4136c566a962","parcelName":"North field","crop":"potato","level":"high","riskIndex":78,"reason":"High risk conditions were detected.","generatedAt":"2026-11-01T06:00:00Z"}
```

The `data` object contains:

| Field | Meaning |
|---|---|
| parcelId | UUID of the affected parcel |
| parcelName | Name of the parcel |
| crop | Crop of the parcel |
| level | `high` |
| riskIndex | Risk score from 0 to 100 |
| reason | Plain text explanation from the risk result |
| generatedAt | ISO 8601, in UTC |

The stream remains open until the client disconnects. Reconnecting opens a new stream; previously sent alerts are not returned.

Errors:

- 401 for a missing, invalid or expired token.
