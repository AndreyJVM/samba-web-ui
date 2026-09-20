# Правила разработки и стандарты проекта (Samba Web UI)

## 1. Стиль кода Java (Google Java Style)
- В проекте строго используется **Google Java Style**.
- Форматирование автоматизировано через плагин `spotless-maven-plugin` с движком `google-java-format` (v1.22.0).
- Перед коммитом всегда проверяйте форматирование:
  ```bash
  ./mvnw spotless:check
  ```
  Или автоматически примените форматирование:
  ```bash
  ./mvnw spotless:apply
  ```
- В сборку `mvn compile` и `mvn test` включена автоматическая валидация стиля (`spotless:check`).

## 2. Архитектура и стек технологий
- **Java 21**, **Spring Boot 3.4.0**, **Thymeleaf**, **Bootstrap 5**.
- **Data Transfer Objects (DTO):**
  - Неизменяемые (immutable) DTO ответов API объявляются как **Java 21 `record`**.
  - Форм-биндинг DTO для Thymeleaf (`@ModelAttribute`) остаются классами с геттерами и сеттерами (Lombok `@Data` или стандартные POJO).
- **Frontend & Templates:**
  - Запрещено использовать inline `<script>` в Thymeleaf шаблонах `*.html`.
  - Весь JavaScript код должен быть строго вынесен в отдельные модульные файлы в `src/main/resources/static/js/`.
  - Дизайн интерфейса — минималистичный и аккуратный (в стиле Vercel / Cloudflare), без избыточных кричащих цветов.

## 3. SSH и системные команды Linux
- Samba Web UI управляет сервером удаленно по SSH в режиме Zero-Agent.
- Приоритетная целевая ОС сервера — **Astra Linux (Debian-based)**.
- Все системные команды используют префикс `sudo` с правами NOPASSWD в `/etc/sudoers.d/samba-web-ui`.
