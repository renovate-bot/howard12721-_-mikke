FROM eclipse-temurin:25.0.2_10-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY api ./api
COPY platform ./platform
COPY events ./events
COPY services ./services

RUN ./gradlew :api:installDist --no-daemon

FROM eclipse-temurin:25.0.2_10-jre-jammy AS runtime

WORKDIR /opt/app

COPY --from=builder /workspace/api/build/install/api ./

EXPOSE 8080

ENTRYPOINT ["/opt/app/bin/api"]
