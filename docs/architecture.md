# Архитектура и Устройство

Приложение построено по классической слоистой архитектуре (Layered Architecture) в связке с паттерном Фасад для удаленного управления (SSH/JSch), позволяющим избежать установки агента.

---

## Стек технологий

* **Ядро бэкенда**: Spring Boot 3.4 (Java 21)
* **Безопасность**: Spring Security, HttpSession (заменило кастомные перехватчики)
* **Шаблонизатор**: Thymeleaf (с использованием фрагментов)
* **Фронтенд**: Bootstrap 5.3, FontAwesome 6, кастомный CSS (`style.css`)
* **SSH-клиент**: JSch (`com.github.mwiede:jsch`) через обёртку `CommandExecutor`
* **Контейнеризация**: Многоэтапный Dockerfile
* **CI/CD & Docs**: GitHub Actions, Docker Hub, MkDocs Material (GitHub Pages)

---

## Структура директорий и пакетов

```shell
samba-web-ui/
├── .github/
│   └── workflows/
│       ├── ci-cd.yml             # Сборка JAR, прогон тестов, публикация в Docker Hub
│       └── docs.yml              # Автоматическая сборка MkDocs на GitHub Pages
├── docs/                         # Документация в Markdown
├── src/
│   ├── main/
│   │   ├── java/mari/samba/
│   │   │   ├── SambaWebUiApplication.java   # Точка входа Spring Boot
│   │   │   │
│   │   │   ├── config/                      # Конфигурации Spring
│   │   │   │   └── SecurityConfig.java      # Настройки авторизации и защиты от CSRF 
│   │   │   │
│   │   │   ├── controller/web/              # Веб-контроллеры
│   │   │   │   ├── AuthController.java      # Вход, авторизация и выход
│   │   │   │   ├── ShareController.java     # Управление общими папками
│   │   │   │   ├── UserController.java      # Управление пользователями
│   │   │   │   ├── GroupController.java     # Управление группами
│   │   │   │   ├── ConfigController.java    # Логика работы с smb.conf и бэкапами
│   │   │   │   ├── MonitoringController.java# Мониторинг smbd и smbstatus
│   │   │   │   └── WebRoutes.java           # Константы всех эндпоинтов (Anti-Magic Strings)
│   │   │   │
│   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   ├── SambaShareCreateDto.java # Создание папки
│   │   │   │   ├── SambaUserCreateDto.java  # Форма пользователя
│   │   │   │   ├── SambaGroupCreateDto.java # Форма группы
│   │   │   │   └── SambaGlobalConfigDto.java# Настройки секции [global]
│   │   │   │
│   │   │   ├── model/                       # Доменные модели
│   │   │   │   ├── SambaShare.java          # Сущность шары
│   │   │   │   ├── SambaUser.java           # Сущность пользователя
│   │   │   │   └── SambaGroup.java          # Сущность группы
│   │   │   │
│   │   │   └── service/                     # Бизнес-логика и сервисы
│   │   │       ├── infra/CommandExecutor.java# Выполнение shell-комманд по SSH
│   │   │       ├── infra/SshSessionManager.java # Работа с SSH сессиями JSch
│   │   │       ├── SmbConfParser.java       # Парсинг/запись INI-файла Samba
│   │   │       ├── SambaConfigService.java  # Сохранение smb.conf, валидация testparm
│   │   │       ├── SambaShareService.java   # Логика общих папок
│   │   │       ├── SambaGroupService.java   # Логика Linux групп
│   │   │       ├── SambaUserService.java    # Управление пользователями
│   │   │       └── SambaMonitoringService.java # Статус процессов и дисков
│   │   │
│   │   └── resources/
│   │       ├── static/css/style.css         # Основные стили
│   │       ├── templates/                   # Thymeleaf-шаблоны (shares, users, groups, error)
│   │       └── application.yaml             # Конфигурация Spring Boot
│   └── test/                                # WebMvcTest-покрытие контроллеров
├── Dockerfile                               # Сборка образа
├── mkdocs.yml                               # Настройки документации
└── README.md                                
```

## Ключевые паттерны

1. **Выполнение команд**: Код не плодит локальных потоков. Связь к Linux выполняется через обертку CommandExecutor, отправляющую команды (например, пользовательские `useradd`) через SSH. Это оставляет проект независимым от ОС сервера, где он развернут.
2. **Безопасное обновление `smb.conf`**: Любое изменение в глобальных настройках проходит цикл "Резервная копия -> запись во временный файл `/tmp` -> проверка валидности структуры через команду `testparm` -> перенос `mv` -> перезагрузка `smbd`".
3. **Безопасность (Spring Security)**: Все методы и модифицирующие эндпоинты покрыты CSRF защитой и проверяют наличие авторизованной сессии в `SecurityContextHolder`.
4. **Clean Code API**: Все URL маршруты приложения собраны в `WebRoutes.java` (Anti-Magic Strings рефакторинг), а контроллеры полностью покрыты `WebMvcTest` испытаниями.
