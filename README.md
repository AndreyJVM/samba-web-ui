# Samba Web UI

Веб-интерфейс для управления Samba-сервером через SSH. Позволяет просматривать, создавать, редактировать и удалять общие папки (шары), а также управлять пользователями Samba.

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


## Запуск через Docker

```bash
docker run -d \
  --name samba-web-ui \
  -p 8080:8080 \
  andreyvorobevaqa/samba-web-ui:latest
```

- Откройте браузер: `http://localhost:8080`
