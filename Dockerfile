# STAGE 1: Builder
# Используем сборку на базе Debian/Ubuntu (jammy), так как frontend-maven-plugin
# скачивает бинарники Node.js (glibc), которые не работают в чистом Alpine (musl).
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Устанавливаем maven
RUN apt-get update && apt-get install -y maven

COPY pom.xml .
RUN mvn dependency:go-offline

COPY frontend ./frontend
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
