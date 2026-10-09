# Samba Web UI

Samba Web UI is a lightweight and modern web interface for managing Samba file shares, built with Java 21, Spring Boot, and Vue 3.

## Quick Start

You can run the application quickly using Docker:

```bash
docker run -d \
  -p 8080:8080 \
  -v /path/to/smb.conf:/etc/samba/smb.conf \
  -e SAMBA_CONF_PATH=/etc/samba/smb.conf \
  andreyvorobevaqa/samba-web-ui:latest
```

## Features
- Complete UI for managing Samba shares
- Manage global Samba configuration
- View and manage Samba users
- Secure REST API

## Environment Variables
- `SAMBA_CONF_PATH` (default: `/etc/samba/smb.conf`): Path to the Samba configuration file.
- `SERVER_PORT` (default: `8080`): The port the web application listens on.
- `SPRING_SECURITY_USER_NAME` (default: `admin`): Web UI admin username.
- `SPRING_SECURITY_USER_PASSWORD` (default: `admin`): Web UI admin password.

## Documentation and Source Code

For full documentation, setup guides (including Docker Compose), and source code, please visit the [GitHub Repository](https://github.com/andreyvorobevaqa/samba-web-ui).
