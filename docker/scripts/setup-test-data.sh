#!/bin/bash
set -e

echo "========================================================"
echo "       Samba Sandbox - Initializing Test Data           "
echo "========================================================"

echo ">>> [1/3] Создание тестовых групп..."
groupadd developers
groupadd qa
groupadd hr
groupadd finance

echo ">>> [2/3] Создание тестовых пользователей..."
TEST_PASS="test"

setup_user() {
    local username=$1
    local group=$2
    
    # Создаем системного пользователя без возможности входа по SSH (no shell)
    # и назначаем ему основную группу.
    useradd -m -s /usr/sbin/nologin -g "$group" "$username"
    
    # Устанавливаем пароль только для Samba
    (echo "$TEST_PASS"; echo "$TEST_PASS") | smbpasswd -s -a "$username"
    smbpasswd -e "$username"
}

setup_user "dev1" "developers"
setup_user "dev2" "developers"
setup_user "qa1" "qa"
setup_user "qa2" "qa"
setup_user "hr1" "hr"
setup_user "fin1" "finance"

# Пользователь admin (созданный ранее) добавляется во все группы для полного доступа
for g in developers qa hr finance; do
    usermod -aG "$g" admin
done

echo ">>> [3/3] Создание директорий и назначение прав..."
mkdir -p /srv/samba/developers
mkdir -p /srv/samba/qa
mkdir -p /srv/samba/hr
mkdir -p /srv/samba/finance

# Устанавливаем владельца и группу. Флаг 2 (SetGID) гарантирует, что 
# новые файлы в этих папках унаследуют группу директории (например: developers).
chown root:developers /srv/samba/developers
chmod 2770 /srv/samba/developers

chown root:qa /srv/samba/qa
chmod 2770 /srv/samba/qa

chown root:hr /srv/samba/hr
chmod 2770 /srv/samba/hr

chown root:finance /srv/samba/finance
chmod 2770 /srv/samba/finance

echo "========================================================"
echo " Тестовые пользователи и папки успешно созданы!         "
echo " Пользователи: dev1, dev2, qa1, qa2, hr1, fin1"
echo " Пароль для всех тестовых аккаунтов: test"
echo "========================================================"
