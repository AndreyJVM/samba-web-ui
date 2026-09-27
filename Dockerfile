# ==========================================
# Stage 1: Build the Application
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Install Node.js & npm (required by frontend-maven-plugin fallback, though usually it downloads its own)
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
# Stage 2: Production Container (Alpine Linux)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install OS dependencies: SSH Server, Samba, Sudo, Bash
RUN apk add --update --no-cache \
    openssh \
    samba \
    samba-common-tools \
    samba-client \
    sudo \
    bash \
    shadow \
    coreutils

# Setup SSH Host Keys
RUN ssh-keygen -A

# Create default administrative user for the Web UI (Admin mapped to OS)
RUN adduser -D -h /home/admin -s /bin/bash admin && \
    echo "admin:admin" | chpasswd && \
    echo "admin ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Prepare Samba config and backup directories
RUN mkdir -p /etc/samba/backups && \
    mkdir -p /mnt/samba/public && \
    chown -R admin:admin /mnt/samba

# Copy Docker entrypoint and apply permissions
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Copy Spring Boot App from Builder
COPY --from=build /app/target/samba-web-ui-*.jar /app/samba-web-ui.jar

# Expose Web Interface, SMB, and SSH (for web UI backend bridge)
EXPOSE 8080
EXPOSE 445
EXPOSE 139
EXPOSE 22

# Launch the orchestrator script
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
