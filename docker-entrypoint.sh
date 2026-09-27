#!/bin/bash
set -e

echo "[i] Starting SSH daemon..."
/usr/sbin/sshd

echo "[i] Generating default smb.conf if not exists..."
if [ ! -f /etc/samba/smb.conf ]; then
    cat <<EOF > /etc/samba/smb.conf
[global]
    workgroup = WORKGROUP
    server string = Samba Web UI Appliance
    server role = standalone server
    security = user
    map to guest = never
    passdb backend = tdbsam
    load printers = no
    disable netbios = yes
    smb ports = 445
EOF
fi

echo "[i] Starting Samba daemon (smbd)..."
smbd -D

echo "[i] Starting Samba Web UI (Spring Boot)..."
exec java -jar /app/samba-web-ui.jar
