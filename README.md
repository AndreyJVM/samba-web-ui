```shell
samba-web-ui/
├── .github/workflows/docker-publish.yml    # CI/CD пайплайн
├── src/
│   └── main/
│       ├── java/mari/samba/
│       │   ├── config/                     # Конфигурации (пока отключена)
│       │   ├── controller/                 # MVC контроллеры
│       │   │   ├── AuthController.java     # Аутентификация
│       │   │   └── ShareController.java    # Управление шарами
│       │   ├── dto/                        # Data Transfer Objects
│       │   │   ├── SambaShareCreateDto.java
│       │   │   ├── SambaShareDto.java
│       │   │   └── SshConnectionRequest.java
│       │   ├── model/                      # Модели данных
│       │   │   └── SambaShare.java
│       │   ├── service/                    # Бизнес-логика
│       │   │   ├── SambaConfigService.java # Парсинг smb.conf
│       │   │   ├── SambaShareService.java  # CRUD для шар
│       │   │   └── SshSessionManager.java  # Управление SSH-сессиями
│       │   └── SambaWebUiApplication.java  # Точка входа
│       └── resources/
│           ├── static/css/style.css        # Стили
│           ├── templates/
│           │   ├── index.html              # Форма подключения
│           │   └── shares/                 # Страницы управления шарами
│           │       ├── create.html
│           │       ├── edit.html
│           │       └── list.html
│           └── application.properties
├── Dockerfile
├── docker-compose.yml
└── pom.xml                   
```