# Samba Web UI

Веб-интерфейс для управления Samba-сервером через SSH. Позволяет просматривать, создавать, редактировать и удалять общие папки (шары), а также управлять пользователями Samba.

## Запуск через Docker

```bash
docker run -d \
  --name samba-web-ui \
  -p 8080:8080 \
  andreyvorobevaqa/samba-web-ui:latest
```

- Откройте браузер: `http://localhost:8080`
