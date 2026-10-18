# Архитектура ShareGuard

ShareGuard — веб-интерфейс для управления Samba на Linux через SSH. Backend предоставляет REST API, frontend работает как React SPA. Установка отдельного агента ShareGuard на управляемый сервер не требуется; необходимые серверные команды выполняются через SSH.

## Стек технологий

| Компонент | Реализация |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4.0, Spring Web и Bean Validation |
| Безопасность HTTP | Spring Security, серверная сессия, CSRF-токен для SPA |
| Frontend | React 18, TypeScript, Vite, React Router, Tailwind CSS |
| SSH | JSch через CommandExecutor и SshSessionManager |
| Проверки backend | JUnit, Spring Boot Test, Spring Security Test, Mockito |
| Сборка и документация | Maven Wrapper, Spotless, GitHub Actions, MkDocs Material |

Версии и зависимости определены в pom.xml и frontend/package.json. Thymeleaf и Bootstrap не используются в текущем интерфейсе.

## Структура проекта

```text
samba-web-ui/
├── frontend/
│   ├── src/
│   │   ├── pages/                 # Страницы интерфейса
│   │   ├── components/            # Общие компоненты и файловый менеджер
│   │   ├── lib/api.ts             # HTTP-клиент, CSRF и обработка ошибок
│   │   └── App.tsx                # Маршрутизация SPA
│   └── vite.config.ts            # Базовый путь /ui/ и dev-прокси API
├── src/main/java/mari/samba/
│   ├── SambaWebUiApplication.java # Точка входа Spring Boot
│   ├── auth/                     # Вход, выход и защита от перебора
│   ├── share/                    # Общие ресурсы Samba
│   ├── user/                     # Пользователи Samba
│   ├── group/                    # Группы и членство
│   ├── smbconfig/                # Чтение, изменение и резервные копии smb.conf
│   ├── filesystem/               # Файлы, каталоги и использование дисков
│   ├── monitoring/               # Состояние сервиса и журналы
│   ├── ad/                       # Интеграция с Active Directory
│   ├── fstec/                    # Оценка конфигурации по правилам YAML
│   ├── infra/                    # SSH, формирование команд, парсер smb.conf
│   ├── config/                   # Spring Security, MVC и свойства приложения
│   └── core/                     # Ответы API, исключения и их обработка
├── src/main/resources/
│   ├── application.properties    # Настройки приложения
│   └── fstec/                    # Правила оценки конфигурации
├── src/test/java/mari/samba/     # Тесты контроллеров, сервисов и инфраструктуры
├── .github/workflows/
│   ├── ci.yml                    # Сборка, тесты и публикация по тегам
│   └── docs.yml                  # Публикация документации
├── docker/                       # Файлы контейнерного стенда и образов
├── scripts/sandbox/              # Запуск локального стенда
├── docs/                         # Исходники документации
├── Dockerfile
└── pom.xml
```

Контроллеры, DTO и сервисы сгруппированы по функциональным областям. Единого класса WebRoutes в текущей структуре нет: маршруты API заданы аннотациями контроллеров.

## Запросы и удалённые операции

1. React обращается к REST API через frontend/src/lib/api.ts. Клиент передаёт CSRF-токен в изменяющих запросах и обрабатывает ответы HTTP 401.
2. Spring Security проверяет доступ. Публичные пути и исключения CSRF определены в SecurityConfig; login и logout исключены из проверки CSRF.
3. Контроллер вызывает сервис своей функциональной области.
4. Сервис использует CommandExecutor; реализация SshSessionManager выполняет команду через SSH. LinuxCommands централизует формирование команд и проверку аргументов.
5. Результат возвращается через API. Общие ответы и обработка исключений находятся в пакете core.

Наличие проверок аргументов не заменяет проверки прав и границ доступа. Доверие к SSH-хосту, обработка секретов и разрешённые файловые пути требуют отдельного анализа безопасности.

## Работа с конфигурацией Samba

SmbConfParser разбирает и изменяет содержимое smb.conf. SambaConfigService выполняет чтение, резервное копирование, сохранение и восстановление через SSH.

Метод updateSmbConf создаёт резервную копию, записывает новое содержимое во временный файл /tmp/smb.conf.tmp, запускает testparm, переносит файл на место основной конфигурации и перезапускает smbd. Это последовательность команд, а не транзакция с автоматическим откатом.

Восстановление резервной копии выполняется отдельным методом: проверка имени копии, резервирование текущего файла, копирование выбранной версии и перезапуск smbd.

## Frontend и сборка

React Router использует базовый путь /ui. Основные страницы: login, dashboard, shares, users, groups, config, ad и fstec. Файловый менеджер реализован компонентом FileManager.

Maven frontend-maven-plugin устанавливает локальные Node.js/npm, выполняет npm install и npm run build. Затем maven-resources-plugin копирует frontend/dist в target/classes/static/ui; frontend включается в итоговый Spring Boot JAR.

WebMvcConfig обслуживает /ui/** и возвращает index.html для маршрутов SPA. При отдельной разработке frontend Vite работает на порту 5173 и перенаправляет /api на backend по адресу http://localhost:8080.

## Проверки и CI

Backend содержит тесты контроллеров, сервисов и инфраструктуры. Наличие этих тестов не означает полного покрытия поведения приложения. В frontend предусмотрены команды build и lint; отдельной команды UI-тестов в package.json нет.

CI в .github/workflows/ci.yml использует JDK 21, проверяет Spotless и выполняет Maven clean package. Публикация GitHub Release и Docker-образов выполняется по тегам v*.*.*. Инструкции запуска и требования к окружению приведены в README.md.
