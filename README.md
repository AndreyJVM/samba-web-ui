# ShareGuard

<p>
  <img src="frontend/public/shareguard/logo-full-light.svg" alt="ShareGuard" height="64">
</p>

<p>
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Docker-Ready-blue.svg" alt="Docker Ready">
  <img src="https://img.shields.io/badge/code%20style-palantir--spotless-magenta.svg" alt="Code Style">
  <img src="https://img.shields.io/badge/Docs-MkDocs%20Material-purple.svg" alt="Documentation">
</p>

**ShareGuard** — это современная веб-панель управления файловыми серверами Samba на Linux. 
Позволяет системным администраторам централизованно управлять каталогами, учетными записями,
правами доступа и глобальными параметрами `smb.conf` без установки агентов на сервер (Zero-Agent via SSH).

## Документация

Полное руководство пользователя, инструкции по настройке и описание архитектуры доступны в **[документации](https://AndreyJVM.github.io/samba-web-ui/)**

## Быстрый старт с Docker (Продакшн)

```bash
docker run -d \
  --name samba-web-ui \
  -p 8080:8080 \
  --restart unless-stopped \
  andreyvorobevaqa/samba-web-ui:latest
```

Откройте браузер по адресу http://localhost:8080/ui.

## Локальная разработка и тестирование (Sandbox)

Для локальной разработки и тестирования проекта мы подготовили изолированное окружение (Sandbox). Оно автоматически поднимает контейнер с сервером Samba/SSH и собирает локальный образ веб-интерфейса, чтобы вы могли тестировать функционал без изменения настроек вашей реальной машины.

**Настоятельно рекомендуется запускать тестовый стенд через специальный скрипт**, который корректно соберет окружение, дождется готовности сервисов, а после завершения работы — аккуратно удалит все временные ресурсы (контейнеры, сети и тома), чтобы каждый новый запуск был чистым.

Для запуска выполните:

**Linux / macOS:**
```bash
./scripts/sandbox/run-sandbox.sh
```

**Windows (Git Bash / WSL):**
```bash
bash scripts/sandbox/run-sandbox.sh
```

---

### Что под капотом песочницы:
1. **Сборка фронтенда и бэкенда** прямо на хосте (быстро с локальным Maven кэшем `.m2` без долгого скачивания библиотек).
2. **Docker Compose** поднимает:
   - `samba-node`: Узел Ubuntu с реальным Samba-сервером и SSH (порт `2222` и SMB `1445`, `1139`).
   - `web`: Локальный контейнер Web UI на `http://localhost:8080/ui`.
3. **Автоматическая очистка**: при завершении скрипта (Ctrl+C или Enter) скрипт вычищает все временные контейнеры и тома.

Подробнее о песочнице и параметрах читайте в [docker/README.md](docker/README.md).

## Разработка

### Требования
- Java 21 (JDK)
- Node.js 20+ и npm 10+
- Docker и Docker Compose (для запуска Sandbox)

### Сборка проекта
```bash
# Сборка без тестов
./mvnw clean package -DskipTests

# Запуск тестов
./mvnw test

# Проверка стиля кода (Spotless)
./mvnw spotless:check

# Автоматическое форматирование
./mvnw spotless:apply
```

### Запуск бэкенда из IDE
Основной класс: `mari.samba.ShareGuardApplication`  
Перед запуском из IDE убедитесь, что фронтенд собран (`cd frontend && npm run build`), либо запустите Vite dev-сервер:
```bash
cd frontend
npm run dev
```
Vite dev-сервер доступен на `http://localhost:5173/ui/` с автопроксированием API-запросов на `http://localhost:8080`.

## Лицензия

Распространяется под лицензией MIT. Подробнее см. в файле [LICENSE](LICENSE).
