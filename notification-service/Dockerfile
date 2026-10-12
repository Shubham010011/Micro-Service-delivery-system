# Build context is the repository root (see docker-compose.yml). The parent pom lists every module, so the
# whole source tree must be present or Maven aborts with "Child module ... does not exist"; .dockerignore
# keeps the context small.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl notification-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/notification-service/target/notification-service-1.0.0.jar app.jar
USER app
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
