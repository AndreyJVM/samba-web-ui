# Настройка целевого сервера Linux

Samba Web UI взаимодействует с сервером по SSH от имени специального непривилегированного пользователя с точечными правами `sudo`.

---

## 1. Установка Samba

На целевом сервере (Ubuntu / Debian / Astra Linux):

```bash
sudo apt update
sudo apt install -y samba
```

## 2. Создание сервисного пользователя `samba-admin`

Создайте учетную запись администратора и задайте пароль:

```shell
sudo useradd -m -s /bin/bash samba-admin
sudo passwd samba-admin
sudo smbpasswd -a samba-admin
```
## 3. Настройка прав `sudoers`
Создайте файл правил для утилиты `sudo`:

```shell
sudo visudo -f /etc/sudoers.d/samba-web-ui
```

Вставьте следующее правило (одной строкой):

```shell
samba-admin ALL=(ALL) NOPASSWD: /usr/sbin/useradd, /sbin/useradd, /usr/sbin/userdel, /sbin/userdel, /usr/sbin/chpasswd, /usr/bin/chpasswd, /usr/bin/smbpasswd, /usr/bin/pdbedit, /bin/systemctl, /usr/bin/systemctl, /bin/cat, /usr/bin/cat, /bin/mv, /usr/bin/mv, /bin/cp, /usr/bin/cp, /bin/rm, /usr/bin/rm, /bin/mkdir, /usr/bin/mkdir, /bin/chmod, /usr/bin/chmod, /bin/chown, /usr/bin/chown, /usr/bin/smbstatus
```

Установите корректные права на файл конфигурации:

```shell
sudo chmod 0440 /etc/sudoers.d/samba-web-ui
```