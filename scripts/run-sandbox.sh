#!/usr/bin/env bash

# ==============================================================================
# Samba Web UI - Docker Compose Sandbox Runner with Clean Teardown
# ==============================================================================
# Этот скрипт:
# 1. Предлагает выбор режима развёртывания (с тестами / без тестов)
# 2. Собирает frontend на хосте (если dist/ отсутствует или устарел)
# 3. Собирает backend JAR на хосте (используя локальный Maven-кэш без долгих загрузок)
# 4. Собирает Docker образ web-приложения (--target local для мгновенной сборки)
# 5. Запускает окружение (docker compose up -d)
# 6. Ожидает готовности сервисов (healthcheck samba-node и web)
# 7. Предоставляет интерактивное меню команд:
#    - b / build / fast : быстрая частичная пересборка (обновление кода/кнопки без сброса данных)
#    - bf / build-full  : полная пересборка с нуля (удаление контейнеров, томов и сборка)
#    - restart / logs / status / stop
# 8. При завершении (Enter, stop, q или Ctrl+C) полностью останавливает compose,
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

# Автоопределение Java 21 на хосте (если установлена и не переопределена)
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    elif [ -d "/usr/lib/jvm/java-1.21.0-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-1.21.0-openjdk-amd64"
    fi
fi

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

# ==============================================================================
# Выбор режима развёртывания
# ==============================================================================
echo "=========================================================="
echo "         Samba Web UI - Запуск Docker Sandbox             "
echo "=========================================================="
echo ""
echo "Выберите режим развёртывания:"
echo ""
echo "  1) Полный прогон (с чистого листа + тесты)"
echo "     Полная сборка с запуском тестов Maven."
echo "     Рекомендуется для CI и финального тестирования."
echo ""
echo "  2) Быстрый запуск (с чистого листа, без тестов)"
echo "     Сборка без тестов (-DskipTests)."
echo "     Быстрее для локальной разработки."
echo ""

while true; do
    read -r -p "Ваш выбор [1/2] (по умолчанию: 2): " DEPLOY_MODE
    DEPLOY_MODE="${DEPLOY_MODE:-2}"
    case "$DEPLOY_MODE" in
        1)
            SKIP_TESTS="false"
            echo ""
            echo ">>> Режим: ПОЛНЫЙ ПРОГОН (с тестами)"
            break
            ;;
        2)
            SKIP_TESTS="true"
            echo ""
            echo ">>> Режим: БЫСТРЫЙ ЗАПУСК (без тестов)"
            break
            ;;
        *)
            echo "Неверный выбор. Введите 1 или 2."
            ;;
    esac
done

echo "Рабочая директория: $PROJECT_ROOT"
echo ""

# Сборка фронтенда на хосте
build_frontend() {
    echo ">>> Сборка фронтенда на хосте..."
    local ok=0
    if [ -x "$PROJECT_ROOT/frontend/node/npm" ]; then
        (cd "$PROJECT_ROOT/frontend" && ./node/npm run build) && ok=1
    elif command -v npm >/dev/null 2>&1; then
        (cd "$PROJECT_ROOT/frontend" && npm run build) && ok=1
    else
        echo ">>> Запуск Maven для генерации фронтенда..."
        ./mvnw generate-resources -DskipTests && ok=1
    fi
    if [ "$ok" -ne 1 ]; then
        echo "Ошибка: не удалось собрать frontend!" >&2
        return 1
    fi
    return 0
}

# Сборка бэкенда (JAR) на хосте с использованием локального кэша Maven
build_backend() {
    echo ">>> Сборка backend JAR на хосте (быстро с локальным кэшем .m2)..."
    local ok=0
    if [ "$SKIP_TESTS" = "true" ]; then
        ./mvnw package -DskipTests -Dmaven.test.skip=true -Dfrontend.skip=true && ok=1
    else
        ./mvnw package -Dfrontend.skip=true && ok=1
    fi
    if [ "$ok" -ne 1 ]; then
        echo "Ошибка: не удалось собрать backend JAR!" >&2
        return 1
    fi
    return 0
}

# Сборка Docker-образа web
build_docker_image() {
    echo ">>> Сборка Docker-образа web (перенос JAR в alpine JRE)..."
    if ! docker build --target local --network host -t samba-web-ui-web:latest .; then
        echo "Ошибка: не удалось собрать Docker-образ web!" >&2
        return 1
    fi
    return 0
}

# Ожидание готовности samba-node
wait_for_node() {
    echo ">>> Ожидание готовности samba-node (проверка healthcheck)..."
    local MAX_WAIT=60
    local WAIT_COUNT=0
    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
        NODE_STATUS=$(docker inspect --format='{{json .State.Health.Status}}' samba-node-sandbox 2>/dev/null || echo "\"starting\"")
        if [ "$NODE_STATUS" = "\"healthy\"" ]; then
            echo ""
            echo ">>> samba-node-sandbox успешно запущен и готов к работе!"
            return 0
        fi
        sleep 2
        WAIT_COUNT=$((WAIT_COUNT + 2))
        printf "."
    done
    echo ""
    echo "Предупреждение: samba-node-sandbox не перешел в статус healthy за $MAX_WAIT сек."
    return 0
}

# Информационный баннер подключения
print_banner() {
    echo ""
    echo "=========================================================="
    echo "  Docker Sandbox успешно запущен!                         "
    echo "=========================================================="
    if [ "$SKIP_TESTS" = "true" ]; then
        echo "  Режим:          Быстрый запуск (тесты пропущены)      "
    else
        echo "  Режим:          Полный прогон (тесты выполнены)        "
    fi
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
}

# Быстрая / частичная пересборка (обновление кода/кнопки без потери данных)
rebuild_fast() {
    echo ""
    echo "=========================================================="
    echo "  [БЫСТРАЯ ПЕРЕСБОРКА] Обновление изменений проекта...   "
    echo "=========================================================="
    if ! build_frontend; then
        echo ">>> Ошибка при сборке фронтенда. Пересборка отменена."
        return 1
    fi
    if ! build_backend; then
        echo ">>> Ошибка при сборке бэкенда. Пересборка отменена."
        return 1
    fi
    if ! build_docker_image; then
        echo ">>> Ошибка при сборке Docker-образа! Контейнер не обновлен."
        return 1
    fi

    echo ">>> Пересоздание контейнера web (samba-node и тома сохраняются)..."
    $COMPOSE_CMD up -d --no-deps web

    echo ""
    echo ">>> [ГОТОВО] Контейнер web успешно обновлен и запущен!"
    echo ">>> Web UI доступен: http://localhost:8080/ui"
    echo ""
}

# Полная пересборка с удалением всего
rebuild_full() {
    echo ""
    echo "=========================================================="
    echo "  [ПОЛНАЯ ПЕРЕСБОРКА] Очистка всех ресурсов и пересборка "
    echo "=========================================================="
    echo ">>> Остановка сервисов, удаление контейнеров, сетей, вольюмов и образов..."
    $COMPOSE_CMD down -v --rmi local --remove-orphans >/dev/null 2>&1 || true
    docker volume rm -f samba_sandbox_shares samba_sandbox_config >/dev/null 2>&1 || true
    docker rmi -f samba-web-ui-web:latest samba-web-ui_web:latest >/dev/null 2>&1 || true
    rm -rf "$PROJECT_ROOT/frontend/dist"

    if ! build_frontend; then
        echo ">>> Ошибка при сборке фронтенда. Полная пересборка прервана."
        return 1
    fi
    if ! build_backend; then
        echo ">>> Ошибка при сборке бэкенда. Полная пересборка прервана."
        return 1
    fi
    if ! build_docker_image; then
        echo ">>> Ошибка при сборке Docker-образа!"
        return 1
    fi

    echo ">>> Запуск всех служб ($COMPOSE_CMD up -d)..."
    $COMPOSE_CMD up -d

    wait_for_node
    print_banner
}

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
    $COMPOSE_CMD down -v --rmi local --remove-orphans >/dev/null 2>&1 || true

    # Дополнительная явная очистка именованных вольюмов Samba Sandbox
    docker volume rm -f samba_sandbox_shares samba_sandbox_config >/dev/null 2>&1 || true

    # Дополнительное удаление образа веб-приложения по тегам/именам
    docker rmi -f samba-web-ui-web:latest samba-web-ui_web:latest >/dev/null 2>&1 || true

    echo "=========================================================="
    echo "  [ГОТОВО] Контейнеры, вольюмы, сети и образ web удалены! "
    echo "  Следующий запуск будет абсолютно чистым.                "
    echo "=========================================================="
}

# Перехватываем сигналы прерывания для гарантированной очистки
trap cleanup EXIT INT TERM

# 1. Предварительная очистка от предыдущих незавершённых сессий (если были)
echo ">>> Проверка и предварительная очистка старых контейнеров/волюмов/образа web..."
$COMPOSE_CMD down -v --rmi local --remove-orphans >/dev/null 2>&1 || true
docker volume rm -f samba_sandbox_shares samba_sandbox_config >/dev/null 2>&1 || true
docker rmi -f samba-web-ui-web:latest samba-web-ui_web:latest >/dev/null 2>&1 || true

# Проверка наличия собранного фронтенда
if [ ! -d "$PROJECT_ROOT/frontend/dist" ] || [ -z "$(ls -A "$PROJECT_ROOT/frontend/dist" 2>/dev/null)" ]; then
    build_frontend
fi

# Сборка backend JAR на хосте
build_backend

# 2. Сборка Docker-образа web
build_docker_image

# 3. Запуск служб
echo ">>> Запуск служб ($COMPOSE_CMD up -d)..."
$COMPOSE_CMD up -d

# 4. Ожидание готовности контейнеров
wait_for_node
print_banner

# 5. Интерактивный цикл ожидания команды
print_help() {
    echo "Доступные команды:"
    echo "  build, b, fast        - Быстрая/частичная пересборка (обновить frontend/код без сброса samba-node и томов)"
    echo "  build-full, bf        - Полная пересборка с нуля (удаление всех контейнеров, томов и сборка с --no-cache)"
    echo "  restart, r            - Перезапустить контейнеры без пересборки"
    echo "  logs, l               - Просмотр логов в реальном времени (выйти из логов: Ctrl+C)"
    echo "  status, s, ps         - Показать статус контейнеров"
    echo "  help, h               - Показать эту справку"
    echo "  stop, q, exit, Enter  - Остановить и полностью очистить (контейнеры, вольюмы, сети, образ web)"
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
        build|b|fast|rebuild)
            rebuild_fast || true
            ;;
        build-full|bf|clean-build|rebuild-full)
            rebuild_full || true
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
            echo "Неизвестная команда: '$cmd'. Введите 'help' для списка доступных команд."
            ;;
    esac
done
