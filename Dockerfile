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
    procps \
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

# Polyfill systemctl for Alpine (which uses OpenRC)
# The backend relies on 'systemctl' to check/manage Samba services.
RUN echo '#!/bin/bash' > /usr/bin/systemctl && \
    echo 'action=$(echo "$1" | tr -d "'\''")' >> /usr/bin/systemctl && \
    echo 'shift' >> /usr/bin/systemctl && \
    echo 'if [ "$1" = "--" ]; then shift; fi' >> /usr/bin/systemctl && \
    echo 'service=$(echo "$1" | tr -d "'\''")' >> /usr/bin/systemctl && \
    echo 'if [ "$service" = "smbd" ]; then exec_bin="smbd";' >> /usr/bin/systemctl && \
    echo 'elif [ "$service" = "nmbd" ]; then exec_bin="nmbd";' >> /usr/bin/systemctl && \
    echo 'else exec_bin="$service"; fi' >> /usr/bin/systemctl && \
    echo 'if [ "$action" = "restart" ] || [ "$action" = "reload" ]; then' >> /usr/bin/systemctl && \
    echo '    pkill "$exec_bin" || true' >> /usr/bin/systemctl && \
    echo '    $exec_bin -D || true' >> /usr/bin/systemctl && \
    echo 'elif [ "$action" = "start" ]; then' >> /usr/bin/systemctl && \
    echo '    $exec_bin -D || true' >> /usr/bin/systemctl && \
    echo 'elif [ "$action" = "stop" ]; then' >> /usr/bin/systemctl && \
    echo '    pkill "$exec_bin" || true' >> /usr/bin/systemctl && \
    echo 'elif [ "$action" = "status" ]; then' >> /usr/bin/systemctl && \
    echo '    if pgrep "$exec_bin" > /dev/null; then echo "active (running)"; exit 0; else echo "inactive"; exit 3; fi' >> /usr/bin/systemctl && \
    echo 'elif [ "$action" = "is-active" ]; then' >> /usr/bin/systemctl && \
    echo '    if pgrep "$exec_bin" > /dev/null; then echo "active"; exit 0; else echo "inactive"; exit 3; fi' >> /usr/bin/systemctl && \
    echo 'fi' >> /usr/bin/systemctl && \
    echo 'exit 0' >> /usr/bin/systemctl && \
    chmod +x /usr/bin/systemctl

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
