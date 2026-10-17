#!/bin/bash
set -e

# Ensure test directories and mock documents exist (e.g. if mounted over empty named volume)
if [ ! -f /srv/samba/public/welcome_readme.txt ] && [ -x /setup-test-data.sh ]; then
    /setup-test-data.sh || true
fi

# Start Samba Daemons
service smbd start
service nmbd start

# Execute SSH Daemon in foreground to keep container running
exec /usr/sbin/sshd -D
