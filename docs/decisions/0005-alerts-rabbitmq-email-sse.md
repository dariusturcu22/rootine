# 0005 - Alerts with RabbitMQ, email and SSE

## Context
When the risk is high, the farmer must get an email and see a live alert in the app. The daily job should not wait for emails to be sent.

## Decision
The daily risk job publishes a message to RabbitMQ when the risk is high. A consumer reads it, sends the email (Spring Mail) and pushes an event to the user over SSE (server-sent events).

## Consequences
- Sending is separate from computing risk; a slow mail server does not block the job.
- Failed messages can be retried.
- RabbitMQ is one more service to run (it is in `docker-compose.yml`).
- SSE goes through nginx, so `/api` has proxy buffering off (see ADR 0006).
