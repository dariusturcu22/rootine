# 0007 - Spring Boot version

## Context
The backend team chose Java and Spring Boot. A new project should start on a current, supported release.

## Decision
Java 21 and Spring Boot 4.1.2 (the latest stable 4.x when the repo was created). springdoc-openapi 3.2.0, which is the line for Spring Boot 4. Security uses `spring-boot-starter-security-oauth2-resource-server` with a JWT decoder.

## Consequences
- Spring Boot 4 has modular starters and moved some packages. Many older tutorials and Stack Overflow answers show the Spring Boot 3 names.
- Testcontainers 2 is used; its PostgreSQL and RabbitMQ classes are in `org.testcontainers.postgresql` and `org.testcontainers.rabbitmq`.
- Upgrades inside 4.x are small. A move to the next major version needs a new ADR.
