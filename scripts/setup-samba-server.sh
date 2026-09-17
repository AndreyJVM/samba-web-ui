#!/bin/bash

# Скрипт первоначальной настройки Samba сервера для Samba Web UI
# Запускать от имени root (sudo)

set -e

echo "========================================================"
echo "       Samba Web UI - Initial Server Setup Script       "
echo "========================================================"

# Проверка, что скрипт запущен от root
if [ "$EUID" -ne 0 ]; then
  echo "Ошибка: Данный скрипт необходимо запускать от имени root."
  echo "Использование: sudo ./setup-samba-server.sh"
  exit 1
fi

# Ввод имени пользователя
read -p "Введите имя администратора Samba [по умолчанию: samba-admin]: " ADMIN_USER
ADMIN_USER=${ADMIN_USER:-samba-admin}

# 1. Установка Samba
echo ">>> [1/4] Установка пакета Samba..."
apt-get update -qq
apt-get install -y samba

# 2. Создание системного пользователя
echo ">>> [2/4] Подготовка системного пользователя '$ADMIN_USER'..."
if id "$ADMIN_USER" &>/dev/null; then
    echo "Пользователь $ADMIN_USER уже существует."
else
    useradd -m -s /bin/bash "$ADMIN_USER"
    echo "Пожалуйста, задайте системный пароль (Linux) для '$ADMIN_USER':"
    passwd "$ADMIN_USER"
fi

# 3. Настройка Samba пароля
echo ">>> [3/4] Добавление '$ADMIN_USER' в базу пользователей Samba..."
echo "Пожалуйста, задайте пароль Samba для '$ADMIN_USER' (с ним будет происходить вход в Web UI):"
smbpasswd -a "$ADMIN_USER"
smbpasswd -e "$ADMIN_USER"

# 4. Настройка Sudoers (беспарольный доступ для Web UI)
echo ">>> [4/4] Настройка прав sudoers для '$ADMIN_USER'..."
SUDOERS_FILE="/etc/sudoers.d/samba-web-ui"

cat <<EOF > "$SUDOERS_FILE"
$ADMIN_USER ALL=(ALL) NOPASSWD: /usr/sbin/useradd, /sbin/useradd, /usr/sbin/userdel, /usr/bin/kill, /sbin/userdel, /usr/sbin/chpasswd, /usr/bin/chpasswd, /usr/bin/smbpasswd, /usr/bin/pdbedit, /usr/bin/tail, /bin/df, /usr/bin/df, /bin/systemctl, /usr/bin/systemctl, /bin/cat, /usr/bin/cat, /bin/mv, /usr/bin/mv, /bin/mkdir, /bin/cp, /usr/bin/cp, /bin/rm, /usr/bin/rm, /usr/bin/mkdir, /bin/chmod, /usr/bin/chmod, /bin/chown, /usr/bin/chown, /usr/bin/smbstatus, /bin/kill, /usr/bin/tail is-active smbd
EOF

chmod 0440 "$SUDOERS_FILE"

echo "========================================================"
echo " Настройка успешно завершена!                           "
echo " Теперь вы можете подключиться к этому серверу          "
echo " через Samba Web UI, используя пользователя: $ADMIN_USER"
echo "========================================================"
