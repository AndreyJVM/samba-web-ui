# Samba Web UI

<p>
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Docker-Ready-blue.svg" alt="Docker Ready">
  <img src="https://img.shields.io/badge/Docs-MkDocs%20Material-purple.svg" alt="Documentation">
</p>

**Samba Web UI** — это современная веб-панель управления файловыми серверами Samba на Linux. 
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
./scripts/run-sandbox.sh
```

**Windows (Git Bash / WSL):**
```bash
bash scripts/run-sandbox.sh
```
