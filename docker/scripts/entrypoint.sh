#!/bin/bash
set -e

# Start Samba Daemons
service smbd start
service nmbd start

# Execute SSH Daemon in foreground to keep container running
exec /usr/sbin/sshd -D
