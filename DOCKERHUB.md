# ShareGuard

ShareGuard is a lightweight and modern web interface for managing Samba file servers on Linux, built with Java 21, Spring Boot, and React. It operates using a Zero-Agent architecture, managing remote servers directly via SSH without requiring local agents or config mounts.

## Quick Start

Run the container using Docker:

```bash
docker run -d \
  --name shareguard \
  -p 8080:8080 \
  --restart unless-stopped \
  andreyvorobevaqa/shareguard:latest
```

Open `http://localhost:8080/ui` in your browser. From the UI, you can connect to your Samba server using SSH credentials.

### Astra Linux Special Edition (Parsec)

For Astra Linux SE 1.8 hosts where Parsec blocks JVM JIT executable memory, use the dedicated image:

```bash
docker run -d \
  --name shareguard \
  -p 8080:8080 \
  --restart unless-stopped \
  andreyvorobevaqa/shareguard:astra-latest
```

The Astra image disables PaX MPROTECT only for the `java` executable inside the image. This permits the JVM JIT to start and reduces memory-execution protection for that Java process. The regular `latest` image keeps its existing runtime configuration. Versioned Astra images use the `astra-<version>` tag.

## Features
- Complete UI for managing Samba shares and permissions
- Manage global Samba configuration (`smb.conf`) remotely
- View and manage Samba users
- Zero-Agent architecture: connects to servers via SSH
- Secure REST API

## Environment Variables
- `SERVER_PORT` (default: `8080`): The port the web application listens on.
- `SPRING_SECURITY_USER_NAME` (default: `admin`): Web UI admin username.
- `SPRING_SECURITY_USER_PASSWORD` (default: `admin`): Web UI admin password.

## Documentation and Source Code

For full documentation, setup guides, and local development sandbox instructions, please visit the [GitHub Repository](https://github.com/AndreyJVM/shareguard).
