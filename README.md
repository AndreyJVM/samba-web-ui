# Samba Web UI

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Docker-Ready-blue.svg" alt="Docker Ready">
  <img src="https://img.shields.io/badge/Docs-MkDocs%20Material-purple.svg" alt="Documentation">
</p>

**Samba Web UI** — это современная веб-панель управления файловыми серверами Samba на Linux. Позволяет системным администраторам централизованно управлять каталогами, учетными записями, правами доступа и глобальными параметрами `smb.conf` без установки агентов на сервер (Zero-Agent via SSH).

---

## Документация

Полное руководство пользователя, инструкции по настройке `sudoers` и описание архитектуры доступны на нашем сайте:  
👉 **[https://AndreyJVM.github.io/samba-web-ui/](https://AndreyJVM.github.io/samba-web-ui/)**

---

## Быстрый старт (Docker)

```bash
docker run -d \
  --name samba-web-ui \
  -p 8080:8080 \
  --restart unless-stopped \
  andreyvorobevaqa/samba-web-ui:latest
```

Откройте браузер по адресу http://localhost:8080.