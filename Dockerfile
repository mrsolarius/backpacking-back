FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew
COPY build.gradle settings.gradle ./

RUN ./gradlew --no-daemon dependencies

COPY src/ src/
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --system --create-home --uid 10001 appuser
COPY --from=builder /app/build/libs/*.jar /app/app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
