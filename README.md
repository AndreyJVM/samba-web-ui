```shell
samba-web-ui/
├── .github/
│   └── workflows/
│       └── docker-publish.yml    # (адаптируем из первого проекта)
├── nginx/
│   └── nginx.conf                # (скопируем и поправим домен)
├── src/
│   ├── main/
│   │   ├── java/com/mari/samba/
│   │   │   ├── config/           # Spring Security, SSH Client
│   │   │   ├── controller/       # REST и MVC контроллеры
│   │   │   ├── dto/              # Данные для шар и пользователей
│   │   │   ├── service/          # Работа с Samba (парсинг, SSH)
│   │   │   └── SambaWebUiApplication.java
│   │   └── resources/
│   │       ├── static/           # CSS (UI)
│   │       ├── templates/        # Thymeleaf (главная, список шар)
│   │       └── application.properties
│   └── test/                     # Тесты
├── ssl/                          # (только на сервере)
├── .env.example                  
├── docker-compose.yml            
├── Dockerfile                    
└── pom.xml                       
```