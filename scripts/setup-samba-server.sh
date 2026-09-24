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

# Поддержка автоматической (неинтерактивной) установки через переменные окружения
if [ -n "$AUTO_ADMIN_USER" ] && [ -n "$AUTO_ADMIN_PASS" ]; then
    ADMIN_USER="$AUTO_ADMIN_USER"
    ADMIN_PASS="$AUTO_ADMIN_PASS"
    INTERACTIVE=0
    echo "Запуск в неинтерактивном режиме (CI/Docker). Пользователь: $ADMIN_USER"
else
    INTERACTIVE=1
    # Ввод имени пользователя
    read -r -p "Введите имя администратора Samba [по умолчанию: samba-admin]: " ADMIN_USER
    ADMIN_USER=${ADMIN_USER:-samba-admin}
fi

# 1. Установка Samba
echo ">>> [1/4] Установка пакета Samba..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y samba

# 2. Создание системного пользователя
echo ">>> [2/4] Подготовка системного пользователя '$ADMIN_USER'..."
if id "$ADMIN_USER" &>/dev/null; then
    echo "Пользователь $ADMIN_USER уже существует."
else
    # Создаем без добавления в глобальную группу sudo/wheel
    useradd -m -s /bin/bash "$ADMIN_USER"
    
    if [ "$INTERACTIVE" -eq 1 ]; then
        echo "Пожалуйста, задайте системный пароль (Linux) для '$ADMIN_USER':"
        passwd "$ADMIN_USER"
    else
        echo "$ADMIN_USER:$ADMIN_PASS" | chpasswd
    fi
fi

# 3. Настройка Samba пароля
echo ">>> [3/4] Добавление '$ADMIN_USER' в базу пользователей Samba..."
if [ "$INTERACTIVE" -eq 1 ]; then
    echo "Пожалуйста, задайте пароль Samba для '$ADMIN_USER' (с ним будет происходить вход в Web UI):"
    smbpasswd -a "$ADMIN_USER"
    smbpasswd -e "$ADMIN_USER"
else
    (echo "$ADMIN_PASS"; echo "$ADMIN_PASS") | smbpasswd -s -a "$ADMIN_USER"
    smbpasswd -e "$ADMIN_USER"
fi

# 4. Настройка Sudoers (беспарольный доступ для Web UI)
echo ">>> [4/4] Настройка прав sudoers для '$ADMIN_USER'..."

SUDOERS_FILE="/etc/sudoers.d/samba-web-ui"
SUDO_ENTRIES=()

# ==================================================================
# 1. СТАТИЧЕСКИЕ КОМАНДЫ (Строго Ограниченные Аргументы)
# ==================================================================

# Безопасное управление службой Samba
SYSTEMCTL_BIN=$(command -v systemctl 2>/dev/null || true)
if [ -n "$SYSTEMCTL_BIN" ]; then
    for action in start stop restart reload status is-active; do
        SUDO_ENTRIES+=("$SYSTEMCTL_BIN $action smbd")
        SUDO_ENTRIES+=("$SYSTEMCTL_BIN $action nmbd")
    done
fi

# Безопасное чтение пользователей Samba
PDBEDIT_BIN=$(command -v pdbedit 2>/dev/null || true)
[ -n "$PDBEDIT_BIN" ] && SUDO_ENTRIES+=("$PDBEDIT_BIN -L")


# ==================================================================
# 2. ДИНАМИЧЕСКИЕ КОМАНДЫ (Инструменты работы с файлами и юзерами)
# ==================================================================
DYNAMIC_COMMANDS=(
  useradd userdel chpasswd smbpasswd
  groupadd groupdel gpasswd
  smbstatus df tail kill
  cat mv mkdir cp rm chmod chown
)

for cmd in "${DYNAMIC_COMMANDS[@]}"; do
    bin_path=$(command -v "$cmd" 2>/dev/null || true)
    if [ -n "$bin_path" ]; then
        SUDO_ENTRIES+=("$bin_path")
    fi
done

# Собираем строку через запятую
SUDO_LINE="$(IFS=', '; echo "${SUDO_ENTRIES[*]}")"

# Пишем файл sudoers
cat <<EOF > "$SUDOERS_FILE"
# Автоматически сгенерировано setup-samba-server.sh
# Разрешения для Samba Web UI (пользователь: $ADMIN_USER)
$ADMIN_USER ALL=(ALL) NOPASSWD: $SUDO_LINE
EOF

# Ограничиваем права самого файла (обязательное требование sudo)
chmod 0440 "$SUDOERS_FILE"

echo "========================================================"
echo " Настройка успешно завершена!                           "
echo " Теперь вы можете подключиться к этому серверу          "
echo " через Samba Web UI, используя пользователя: $ADMIN_USER"
echo "========================================================"
