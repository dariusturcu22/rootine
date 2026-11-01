# Backend

REST API of Rootine: users and JWT security, parcels (PostGIS geometries), diagnoses (photo upload), the daily risk job that calls the AI service, and alerts (RabbitMQ, email, SSE).

Stack: Java 21, Spring Boot 4, Maven wrapper, PostgreSQL with PostGIS (Hibernate Spatial, Flyway), Spring Security with a JWT resource server, Spring AMQP, Spring Mail, Actuator, springdoc-openapi, MapStruct. Tests: JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo. Formatting: Spotless with google-java-format.

## Structure

Base package `app`, organized by feature:

```
src/main/java/app/
  Application.java
  common/     config, security, exception handling, OpenAPI
  crop/       the crop list served from the AI service
  user/       controller, service, repository, entity, dto
  parcel/     CRUD parcels, PostGIS geometries
  diagnosis/  image upload, results
  risk/       daily job calling AI /v1/risk, stored results, heatmap endpoints
  alert/      RabbitMQ producer/consumer, email, SSE
  aiclient/   HTTP client for the AI service
src/main/resources/
  application.yml          config from environment variables
  db/migration/            Flyway migrations
src/test/java/app/         same packages as main
```

Unit tests are `*Test.java` (surefire). Integration tests are `*IT.java` (failsafe) and extend `AbstractIntegrationTest`, which starts PostGIS and RabbitMQ once.

## Setup, run and test

See the [Backend section of SETUP.md](../SETUP.md#backend). Rules for working here: [AGENTS.md](AGENTS.md).
