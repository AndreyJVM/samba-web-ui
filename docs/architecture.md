# Архитектура проекта

Приложение спроектировано по принципам многослойной архитектуры (Layered Architecture) с чётким разделением ответственности между сетевым транспортом (SSH/JSch), парсингом конфигурации, доменной логикой и веб-представлением.

---

## Стек технологий

* **Ядро бэкенда**: Spring Boot 3.3 (Java 17)
* **Веб-слой**: Spring Web MVC, Spring Validation
* **Шаблонизатор**: Thymeleaf (с модульной компонентной структурой через fragments)
* **Фронтенд**: Bootstrap 5.3, FontAwesome 6, кастомный CSS (`style.css`)
* **SSH-транспорт**: JSch (`com.github.mwiede:jsch`) через абстракцию `CommandExecutor`
* **Контейнеризация**: Многоэтапный Dockerfile (Eclipse Temurin 17 JRE Alpine)
* **CI/CD & Docs**: GitHub Actions, Docker Hub, MkDocs Material (GitHub Pages)

---

## Полная структура проекта

```shell
samba-web-ui/
├── .github/
│   └── workflows/
│       ├── ci-cd.yml             # Сборка JAR, прогон тестов, публикация в Docker Hub
│       └── docs.yml              # Автосборка и деплой сайта MkDocs на GitHub Pages
├── docs/                         # Исходники документации в Markdown
├── src/
│   ├── main/
│   │   ├── java/mari/samba/
│   │   │   ├── SambaWebUiApplication.java   # Точка входа Spring Boot
│   │   │   │
│   │   │   ├── config/                      # Конфигурация Spring MVC
│   │   │   │   └── WebMvcConfig.java        # Регистрация Interceptor'ов и статики
│   │   │   │
│   │   │   ├── controller/                  # Контроллеры веб-маршрутов
│   │   │   │   ├── AuthController.java      # Вход, создание сессии и выход
│   │   │   │   ├── ShareController.java     # Управление общими папками
│   │   │   │   ├── UserController.java      # Управление пользователями
│   │   │   │   ├── ConfigController.java    # Просмотр smb.conf, бэкапы и [global]
│   │   │   │   ├── MonitoringController.java# Дашборд smbstatus и статус службы
│   │   │   │   └── GlobalExceptionHandler.java # Централизованный перехват ошибок
│   │   │   │
│   │   │   ├── dto/                         # Валидируемые DTO для форм
│   │   │   │   ├── ConnectionRequest.java   # Параметры подключения к хосту
│   │   │   │   ├── SambaShareCreateDto.java # Настройки сетевой папки
│   │   │   │   ├── SambaUserCreateDto.java  # Данные создаваемого пользователя
│   │   │   │   ├── SambaGlobalConfigDto.java# Параметры секции [global]
│   │   │   │   └── SambaBackupDto.java      # Метаданные архивного снимка smb.conf
│   │   │   │
│   │   │   ├── interceptor/                 # Перехватчики запросов
│   │   │   │   └── AuthInterceptor.java     # Защита маршрутов от неавторизованного доступа
│   │   │   │
│   │   │   ├── model/                       # Внутренние модели данных
│   │   │   │   ├── SambaShare.java          # Сущность ресурса Samba
│   │   │   │   └── SambaUser.java           # Сущность пользователя Samba
│   │   │   │
│   │   │   └── service/                     # Слой бизнес-логики и инфраструктуры
│   │   │       ├── CommandExecutor.java     # Абстрактный интерфейс вызова shell-команд
│   │   │       ├── SshSessionManager.java   # Пул сессий JSch, реализация CommandExecutor
│   │   │       ├── SmbConfParser.java       # Изолированный парсер/генератор INI-конфига
│   │   │       ├── SambaConfigService.java  # Оркестратор smb.conf, бэкапы и testparm
│   │   │       ├── SambaShareService.java   # Бизнес-логика создания/правки каталогов
│   │   │       ├── SambaUserService.java    # Бизнес-логика системных и Samba-аккаунтов
│   │   │       └── SambaMonitoringService.java # Мониторинг smbd и smbstatus
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       │   └── css/
│   │       │       └── style.css            # Единые стили, переменные, цвета, кнопки
│   │       ├── templates/                   # Thymeleaf-шаблоны
│   │       │   ├── fragments/
│   │       │   │   └── layout.html          # Общий <head>, навбар и скрипты
│   │       │   ├── config/
│   │       │   │   ├── view.html            # Просмотр smb.conf и история бэкапов
│   │       │   │   └── global.html          # Форма параметров секции [global]
│   │       │   ├── shares/
│   │       │   │   ├── list.html            # Список шар, статистика, статус службы
│   │       │   │   ├── create.html          # Форма создания каталога
│   │       │   │   └── edit.html            # Форма редактирования каталога
│   │       │   ├── status/
│   │       │   │   └── dashboard.html       # Дашборд smbd и smbstatus
│   │       │   ├── users/
│   │       │   │   ├── list.html            # Список пользователей и статистика
│   │       │   │   ├── create.html          # Форма регистрации пользователя
│   │       │   │   └── change-password.html # Форма смены пароля
│   │       │   └── index.html               # Страница входа (SSH connection)
│   │       └── application.properties       # Конфигурация Spring Boot
│   └── test/                                # Модульные и интеграционные тесты
├── Dockerfile                               # Двухэтапная сборка контейнера
├── docker-compose.yml                       # Локальный запуск стека
├── mkdocs.yml                               # Конфигурация сайта документации
├── pom.xml                                  # Зависимости и сборка Maven
└── README.md                                # Главная страница репозитория
```

## Принципы изоляции слоев
1. Транспортная независимость: Слой сервисов взаимодействует с Linux через интерфейс CommandExecutor. Ни один сервис не импортирует классы
библиотеки JSch (`Session`, `ChannelExec`). Это позволяет при необходимости подменить реализацию на локальный `ProcessBuilder` или другой SSH-клиент.

2. Безопасность транзакций `smb.conf`: Любое изменение файла конфигурации проходит обязательный цикл:

    - Создание снимка в `/etc/samba/backups/`.

    - Запись изменений во временный файл `/tmp/smb.conf.tmp`.

    - Проверка синтаксиса утилитой `testparm -s`.

    - Атомарный перенос через `mv` и перезапуск службы `systemctl restart smbd`.

3. Безопасность выполнения: Все действия на целевой машине производятся из-под выделенного пользователя `samba-admin` со 
строго ограниченным набором привилегий в `/etc/sudoers.d/samba-web-ui`.