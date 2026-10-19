
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
