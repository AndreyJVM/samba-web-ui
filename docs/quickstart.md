# Быстрый старт

## Запуск через Docker

Самый быстрый способ развернуть **Samba Web UI** — использовать готовый образ с Docker Hub.

```bash
docker run -d \
  --name samba-web-ui \
  -p 8080:8080 \
  --restart unless-stopped \
  andreyvorobevaqa/samba-web-ui:latest
```

После запуска откройте браузер по адресу: http://localhost:8080.

### Запуск из исходного кода
Требования:

- Java 17+

- Maven 3.8+ (или встроенный ./mvnw)

```shell
# Клонирование репозитория
git clone https://github.com/AndreyJVM/samba-web-ui.git
cd samba-web-ui

# Сборка и запуск
./mvnw spring-boot:run
```

После запуска откройте браузер по адресу: http://localhost:8080.