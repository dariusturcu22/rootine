# 0003 - Weather risk lives in the AI service

## Context
The risk map needs weather data and disease risk models. We could put this in the backend or in the AI service.

## Decision
The AI service fetches weather from Open-Meteo (1-hour cache) and computes disease risk with published rule models (Hutton criteria, TOMCAST). There is no ML on weather. The backend has no weather module. It only calls `POST /v1/risk`, stores the result and serves heatmap data.

## Consequences
- All disease knowledge (photos and weather) is in one place, owned by the AI team.
- Each rule is a pure function with a test and a link to its source, so it is easy to check.
- The backend stays simple: a daily job, storage and alerts.
- The AI service needs internet access to Open-Meteo.
