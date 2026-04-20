FROM eclipse-temurin:21.0.10_7-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY api ./api
COPY platform ./platform
COPY events ./events
COPY services ./services

RUN ./gradlew :services:guess-service:installDist --no-daemon

FROM eclipse-temurin:21.0.10_7-jre-jammy AS runtime

WORKDIR /opt/app

COPY --from=builder /workspace/services/guess-service/build/install/guess-service ./

EXPOSE 50055

ENTRYPOINT ["/opt/app/bin/guess-service"]
