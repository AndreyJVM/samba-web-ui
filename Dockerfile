# ==========================================
# Stage 1: Build the Application
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Install Node.js & npm (required by frontend-maven-plugin fallback)
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs

# Copy Maven wrapper & project config
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Give execution rights to Maven wrapper
RUN chmod +x mvnw

# Download dependencies (cache layer)
RUN ./mvnw dependency:go-offline -B -DskipTests

# Copy source code and build the JAR
COPY src ./src
COPY frontend ./frontend
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Production Container (Web UI Only)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# The Web UI container does not need internal Samba or SSH server.
# It only serves the Spring Boot application (which connects to the target Samba node via SSH)

# Copy Spring Boot App from Builder
COPY --from=build /app/target/samba-web-ui-*.jar /app/samba-web-ui.jar

# Expose Web Interface port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/samba-web-ui.jar"]
