#!/bin/sh
set -eu

key=/run/secrets/ssh_authorized_keys
if [ ! -s "$key" ] || ! ssh-keygen -l -f "$key" >/dev/null 2>&1; then
    echo "A valid management SSH public key file is required." >&2
    exit 1
fi
install -d -m 0755 /run/sshd /run/samba /srv/samba /var/log/samba
install -o root -g root -m 0600 "$key" /run/sambryx-authorized-keys
ssh-keygen -A >/dev/null
/usr/sbin/sshd -t -f /opt/sambryx-node/sshd_config
testparm -s >/dev/null
exec /usr/bin/supervisord -c /opt/sambryx-node/supervisord.conf
