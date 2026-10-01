#!/usr/bin/env bash

# ==============================================================================
# Samba Web UI - Docker Compose Sandbox Runner with Clean Teardown
# ==============================================================================
# Этот скрипт:
# 1. Запускает окружение (docker compose up --build -d)
# 2. Ожидает готовности сервисов (healthcheck samba-node и web)
# 3. Предоставляет интерактивное меню команд (logs, status, restart, stop)
# 4. При завершении (Enter, stop, q или Ctrl+C) полностью останавливает compose,
#    удаляет контейнеры, сети, вольюмы (samba-shares, samba-config)
#    и локальный образ web-приложения (samba-web-ui-web:latest),
#    гарантируя чистое состояние перед следующим запуском.
# ==============================================================================

set -eo pipefail

# Определение директории проекта (где лежит docker-compose.yml)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    PROJECT_ROOT="$SCRIPT_DIR"
elif [ -f "$SCRIPT_DIR/../docker-compose.yml" ]; then
    PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
else
    PROJECT_ROOT="$(pwd)"
    if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ]; then
        echo "Ошибка: не найден docker-compose.yml ни в $SCRIPT_DIR, ни в $PROJECT_ROOT" >&2
        exit 1
    fi
fi

cd "$PROJECT_ROOT"

# Определение команды docker compose / docker-compose
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    echo "Ошибка: docker compose или docker-compose не найден в PATH!" >&2
    exit 1
fi

# Проверка доступности Docker демона
if ! docker info >/dev/null 2>&1; then
    echo "Ошибка: Docker демон не запущен или недоступен текущему пользователю." >&2
    echo "Убедитесь, что служба Docker запущена (systemctl start docker / Docker Desktop)." >&2
    exit 1
fi

CLEANED_UP=0

cleanup() {
    # Предотвращаем повторный запуск очистки
    if [ "$CLEANED_UP" -eq 1 ]; then
        return
    fi
    CLEANED_UP=1

    echo ""
    echo "=========================================================="
    echo "  [ОЧИСТКА] Остановка контейнеров и удаление ресурсов...  "
    echo "=========================================================="

    # Останавливаем сервисы, удаляем контейнеры, сети, вольюмы и локально собранный образ web
    $COMPOSE_CMD down -v --rmi local --remove-orphans

    # Дополнительная явная очистка именованных вольюмов Samba Sandbox
    docker volume rm -f samba_sandbox_shares samba_sandbox_config >/dev/null 2>&1 || true

    # Дополнительное удаление образа веб-приложения по тегам/именам (на случай кастомного префикса проекта)
    docker rmi -f samba-web-ui-web:latest samba-web-ui_web:latest >/dev/null 2>&1 || true

    echo "=========================================================="
    echo "  [ГОТОВО] Контейнеры, вольюмы, сети и образ web удалены! "
    echo "  Следующий запуск будет абсолютно чистым.                "
    echo "=========================================================="
}

# Перехватываем сигналы прерывания для гарантированной очистки
trap cleanup EXIT INT TERM

echo "=========================================================="
echo "         Samba Web UI - Запуск Docker Sandbox             "
echo "=========================================================="
echo "Рабочая директория: $PROJECT_ROOT"

# 1. Предварительная очистка от предыдущих незавершённых сессий (если были)
echo ">>> Проверка и предварительная очистка старых контейнеров/вольюмов/образа web..."
$COMPOSE_CMD down -v --rmi local --remove-orphans >/dev/null 2>&1 || true
docker volume rm -f samba_sandbox_shares samba_sandbox_config >/dev/null 2>&1 || true
docker rmi -f samba-web-ui-web:latest samba-web-ui_web:latest >/dev/null 2>&1 || true

# 2. Запуск контейнеров (samba-node скачивается с Docker Hub, web собирается заново)
echo ">>> Запуск служб ($COMPOSE_CMD up --build -d)..."
$COMPOSE_CMD up --build -d

# 3. Ожидание готовности контейнеров
echo ">>> Ожидание готовности samba-node (проверка healthcheck)..."
MAX_WAIT=60
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    NODE_STATUS=$(docker inspect --format='{{json .State.Health.Status}}' samba-node-sandbox 2>/dev/null || echo "\"starting\"")
    if [ "$NODE_STATUS" = "\"healthy\"" ]; then
        echo ">>> samba-node-sandbox успешно запущен и готов к работе!"
        break
    fi
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
    printf "."
done
echo ""

echo ""
echo "=========================================================="
echo "  Docker Sandbox успешно запущен!                         "
echo "=========================================================="
echo "  - Web UI:       http://localhost:8080/ui                "
echo "  ------------------------------------------------------  "
echo "  Параметры для входа в Web UI:                           "
echo "  - Host (IP):    samba-node                              "
echo "  - SSH Port:     22                                      "
echo "  - Username:     admin                                   "
echo "  - Password:     admin                                   "
echo "  ------------------------------------------------------  "
echo "  Прямой доступ с локального хоста (Windows/Mac/Linux):   "
echo "  - SSH:          ssh admin@localhost -p 2222             "
echo "  - Samba (SMB):  \\\\localhost:1445\\public              "
echo "=========================================================="
echo ""

# 4. Интерактивный цикл ожидания команды
print_help() {
    echo "Доступные команды:"
    echo "  stop, q, exit, Enter  - Остановить и полностью очистить (контейнеры, вольюмы, сети, образ web)"
    echo "  logs, l               - Просмотр логов в реальном времени (выйти из логов: Ctrl+C)"
    echo "  status, s, ps         - Показать статус контейнеров"
    echo "  restart, r            - Перезапустить контейнеры"
    echo "  help, h               - Показать эту справку"
}

print_help
echo ""

while true; do
    read -r -p "sandbox> " cmd || break
    case "$cmd" in
        stop|q|quit|exit|"")
            echo "Получена команда на остановку..."
            break
            ;;
        logs|l)
            echo "Подключение к логам (для возврата в меню нажмите Ctrl+C)..."
            (
                # В подоболочке перехватываем Ctrl+C чтобы не выходить из главного скрипта
                trap 'exit 0' INT
                $COMPOSE_CMD logs -f --tail=100
            )
            echo ""
            ;;
        status|s|ps)
            $COMPOSE_CMD ps
            ;;
        restart|r)
            echo "Перезапуск контейнеров..."
            $COMPOSE_CMD restart
            ;;
        help|h)
            print_help
            ;;
        *)
            echo "Неизвестная команда: '$cmd'. Нажмите Enter или введите 'stop' для выхода."
            ;;
    esac
done
