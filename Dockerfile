# ==========================================
# Stage 1: Build Application JAR (Maven)
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper & project config
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Give execution rights to Maven wrapper
RUN chmod +x mvnw

# Download dependencies (cache layer)
RUN ./mvnw dependency:go-offline -B -DskipTests -Dfrontend.skip=true

# Copy source code and pre-built frontend assets
COPY src ./src
COPY frontend ./frontend

# Build argument to skip tests (default: false)
ARG SKIP_TESTS=false

# Build the JAR (uses pre-built frontend/dist via maven-resources-plugin)
RUN if [ "$SKIP_TESTS" = "true" ]; then \
        echo ">>> Сборка БЕЗ тестов (-DskipTests)" && \
        ./mvnw package -DskipTests -Dmaven.test.skip=true -Dfrontend.skip=true; \
    else \
        echo ">>> Сборка С тестами" && \
        ./mvnw package -Dfrontend.skip=true; \
    fi

# ==========================================
# Stage 2: Production Container (Web UI Only)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy Spring Boot App from Builder
COPY --from=build /app/target/shareguard-*.jar /app/shareguard.jar

# Expose Web Interface port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/shareguard.jar"]
