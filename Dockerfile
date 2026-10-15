# ==========================================
# Fast Local / Sandbox Stage (using pre-built JAR)
# Used by scripts/run-sandbox.sh for instant builds
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS local
WORKDIR /app
COPY target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ==========================================
# Stage 1: Build Application JAR (Maven)
# Used for standalone / CI builds when JAR is not pre-built
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper & project config
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw

# Copy source code and pre-built frontend assets
COPY src ./src
COPY frontend ./frontend

# Build argument to skip tests (default: false)
ARG SKIP_TESTS=false

# Build the JAR
RUN --mount=type=cache,target=/root/.m2 \
    if [ "$SKIP_TESTS" = "true" ]; then \
        echo ">>> Сборка БЕЗ тестов (-DskipTests)" && \
        ./mvnw package -DskipTests -Dmaven.test.skip=true -Dfrontend.skip=true; \
    else \
        echo ">>> Сборка С тестами" && \
        ./mvnw package -Dfrontend.skip=true; \
    fi

# ==========================================
# Stage 2: Production Container (Web UI Only)
# Default target for standard multi-stage docker build
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# Copy Spring Boot App from Builder
COPY --from=build /app/target/*.jar /app/app.jar

# Expose Web Interface port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
