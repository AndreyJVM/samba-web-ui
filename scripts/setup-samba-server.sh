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
read -r -p "Введите имя администратора Samba [по умолчанию: samba-admin]: " ADMIN_USER
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

# ------------------------------------------------------------------
# Динамическое разрешение путей к бинарникам через command -v.
# Это избавляет от дублирования /bin vs /usr/bin и /sbin vs /usr/sbin.
# Если команда не найдена, путь будет пустым — такая запись пропускается.
# ------------------------------------------------------------------
resolve_cmd() {
    command -v "$1" 2>/dev/null || echo ""
}

# --- Группа 1: Управление пользователями ---
BIN_USERADD="$(resolve_cmd useradd)"
BIN_USERDEL="$(resolve_cmd userdel)"
BIN_CHPASSWD="$(resolve_cmd chpasswd)"
BIN_SMBPASSWD="$(resolve_cmd smbpasswd)"
BIN_PDBEDIT="$(resolve_cmd pdbedit)"

# --- Группа 2: Управление группами ---
BIN_GROUPADD="$(resolve_cmd groupadd)"
BIN_GROUPDEL="$(resolve_cmd groupdel)"
BIN_GPASSWD="$(resolve_cmd gpasswd)"

# --- Группа 3: Управление службами ---
BIN_SYSTEMCTL="$(resolve_cmd systemctl)"
BIN_KILL="$(resolve_cmd kill)"

# --- Группа 4: Информация и диагностика ---
BIN_SMBSTATUS="$(resolve_cmd smbstatus)"
BIN_DF="$(resolve_cmd df)"
BIN_TAIL="$(resolve_cmd tail)"

# --- Группа 5: Файловые операции ---
BIN_CAT="$(resolve_cmd cat)"
BIN_MV="$(resolve_cmd mv)"
BIN_MKDIR="$(resolve_cmd mkdir)"
BIN_CP="$(resolve_cmd cp)"
BIN_RM="$(resolve_cmd rm)"
BIN_CHMOD="$(resolve_cmd chmod)"
BIN_CHOWN="$(resolve_cmd chown)"

# ------------------------------------------------------------------
# Формирование строки sudoers по группам
# ------------------------------------------------------------------
SUDOERS_FILE="/etc/sudoers.d/samba-web-ui"
SUDO_ENTRIES=()

# Хелпер: добавляет путь, только если он не пустой
add_rule() {
    [ -n "$1" ] && SUDO_ENTRIES+=("$1")
}

# Группа 1: Управление пользователями
add_rule "$BIN_USERADD"
add_rule "$BIN_USERDEL"
add_rule "$BIN_CHPASSWD"
add_rule "$BIN_SMBPASSWD"
add_rule "$BIN_PDBEDIT"

# Группа 2: Управление группами
add_rule "$BIN_GROUPADD"
add_rule "$BIN_GROUPDEL"
add_rule "$BIN_GPASSWD"

# Группа 3: Управление службами
add_rule "$BIN_SYSTEMCTL"
add_rule "$BIN_KILL"

# Группа 4: Информация и диагностика
add_rule "$BIN_SMBSTATUS"
add_rule "$BIN_DF"
add_rule "$BIN_TAIL"

# Группа 5: Файловые операции
add_rule "$BIN_CAT"
add_rule "$BIN_MV"
add_rule "$BIN_MKDIR"
add_rule "$BIN_CP"
add_rule "$BIN_RM"
add_rule "$BIN_CHMOD"
add_rule "$BIN_CHOWN"

# Собираем строку через запятую
SUDO_LINE="$(IFS=', '; echo "${SUDO_ENTRIES[*]}")"

# Пишем файл sudoers
cat <<EOF > "$SUDOERS_FILE"
# Автоматически сгенерировано setup-samba-server.sh
# Разрешения для Samba Web UI (пользователь: $ADMIN_USER)
$ADMIN_USER ALL=(ALL) NOPASSWD: $SUDO_LINE
EOF

chmod 0440 "$SUDOERS_FILE"

echo "========================================================"
echo " Настройка успешно завершена!                           "
echo " Теперь вы можете подключиться к этому серверу          "
echo " через Samba Web UI, используя пользователя: $ADMIN_USER"
echo "========================================================"
