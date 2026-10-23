# Package the tested application JAR, including frontend assets.
# Build the JAR first: ./mvnw clean package
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --chmod=0644 ${JAR_FILE} /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Preserve the target used by the local sandbox.
FROM runtime AS local

FROM runtime AS production
