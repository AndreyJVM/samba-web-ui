#!/usr/bin/env bash

# ==============================================================================
# ShareGuard - Host Server Setup & Dynamic Sudo Configuration
# ==============================================================================
# Этот скрипт подготавливает хост-машину (Ubuntu/Debian) для безопасного
# управления через веб-интерфейс ShareGuard:
# 1. Проверяет права root.
# 2. Создаёт отдельного системного пользователя-администратора (по умолчанию: admin).
# 3. Устанавливает Samba, Winbind, Kerberos и необходимые утилиты.
# 4. Динамически разрешает в /etc/sudoers.d/ ТОЛЬКО необходимые бинарники без пароля,
#    используя абсолютные пути из вашей системы (защита от взлома PATH).
# ==============================================================================

set -euo pipefail

# Цвета для красивого вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[ИНФО]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[ВНИМАНИЕ]${NC} $1"
}

log_error() {
    echo -e "${RED}[ОШИБКА]${NC} $1" >&2
}

# 1. Проверка root-прав
if [ "$EUID" -ne 0 ]; then
    log_error "Скрипт должен быть запущен с правами суперпользователя (root) через sudo."
    exit 1
fi

ADMIN_USER="${1:-admin}"

# Validate: username must only contain safe characters (alphanumeric, dash, underscore, dot)
if ! echo "$ADMIN_USER" | grep -qE '^[a-z_][a-z0-9_.-]{0,31}$'; then
    log_error "Invalid username: '$ADMIN_USER'. Must match ^[a-z_][a-z0-9_.-]{0,31}$"
    exit 1
fi

SUDOERS_FILE="/etc/sudoers.d/shareguard-${ADMIN_USER}"

echo "========================================================"
echo "    Настройка сервера для работы с ShareGuard           "
echo "========================================================"
log_info "Административный пользователь: $ADMIN_USER"

# 2. Создание пользователя, если он не существует
if id "$ADMIN_USER" >/dev/null 2>&1; then
    log_info "Пользователь $ADMIN_USER уже существует."
else
    log_info "Создание пользователя $ADMIN_USER..."
    useradd -m -s /bin/bash "$ADMIN_USER"
    echo "Задайте пароль для пользователя $ADMIN_USER:"
    passwd "$ADMIN_USER"
fi

# 3. Обновление пакетов и установка Samba
log_info "Проверка и установка необходимых пакетов..."
apt-get update -y
apt-get install -y samba smbclient winbind krb5-user libpam-winbind libnss-winbind sudo

# Вспомогательная функция для безопасного поиска абсолютного пути к бинарнику.
resolve_cmd() {
    which "$1" 2>/dev/null || true
}

# ==================================================================
# РАЗРЕШЕНИЯ (Инструменты работы с системой Samba)
# Открываем полный доступ к бинарникам (без ограничений аргументов),
# так как бэкенд Java использует разные динамические флаги (например, --)
# ==================================================================
DYNAMIC_COMMANDS=(
  systemctl pdbedit net tee testparm
  useradd userdel chpasswd smbpasswd
  groupadd groupdel gpasswd
  smbstatus df du tail kill find
  cat mv mkdir cp rm chmod chown
)

SUDO_ENTRIES=()
for cmd in "${DYNAMIC_COMMANDS[@]}"; do
    bin_path=$(resolve_cmd "$cmd")
    if [ -n "$bin_path" ]; then
        SUDO_ENTRIES+=("$bin_path")
    fi
done

# Собираем строку через запятую
SUDO_LINE="$(IFS=', '; echo "${SUDO_ENTRIES[*]}")"

# Пишем файл sudoers
cat <<EOF > "$SUDOERS_FILE"
# Автоматически сгенерировано setup-samba-server.sh
# Разрешения для ShareGuard (пользователь: $ADMIN_USER)
$ADMIN_USER ALL=(ALL) NOPASSWD: $SUDO_LINE
EOF

# Ограничиваем права самого файла (обязательное требование sudo)
chmod 0440 "$SUDOERS_FILE"

echo "========================================================"
echo " Настройка успешно завершена!                           "
echo " Теперь вы можете подключиться к этому серверу          "
echo " через ShareGuard, используя пользователя: $ADMIN_USER  "
echo "========================================================"
