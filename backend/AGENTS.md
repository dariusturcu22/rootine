# AGENTS.md - backend

Read the [root AGENTS.md](../AGENTS.md) first. Its rules apply here, especially: no attribution, English only, tests with every change, no API change without `docs/api/`.

## Stack

Java 21, Spring Boot 4.1, Maven wrapper (`./mvnw`), PostgreSQL + PostGIS, Flyway, Spring Security with a JWT resource server, Spring AMQP (RabbitMQ), Spring Mail, springdoc-openapi, MapStruct, JUnit 5, Mockito, Testcontainers, JaCoCo, Spotless (google-java-format).

## Structure

See [README.md](README.md). Base package `app`, one package per feature.

## Commands

All commands are in the [Backend section of SETUP.md](../SETUP.md#backend). Before a PR: `./mvnw spotless:apply` then `./mvnw verify`.

## Local rules

- Organize code by feature package (`user`, `parcel`, `diagnosis`, `risk`, `alert`, `aiclient`). Shared code goes in `common`.
- Never expose JPA entities in controllers. Use DTOs, mapped with MapStruct.
- Flyway migrations are never edited after they are merged. Add a new `V<n>__description.sql` instead.
- Unit tests are named `*Test`, integration tests `*IT`. Integration tests extend `AbstractIntegrationTest` so the containers start once.
- All configuration comes from environment variables through `application.yml`. No secrets in the repo. `JWT_SECRET` has no default on purpose.
- A change to a controller (path, request, response, status) updates `docs/api/backend.md` in the same PR.
- The backend has no weather module. Weather risk comes from the AI service `/v1/risk`.
- Spring Boot 4 renamed many packages and starters. Check the Spring Boot 4 docs before copying older examples.
