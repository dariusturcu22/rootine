FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --uid 10001 rootine \
    && mkdir -p /data/uploads \
    && chown -R rootine /data

COPY --from=build /build/target/backend-*.jar app.jar

USER rootine
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
