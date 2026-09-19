# Настройка целевого сервера Linux

Samba Web UI обращается с сервером по SSH от имени специального пользователя, которому выданы права `sudo`.

---

## 1. Установка Samba

На целевом Linux сервере (Ubuntu / Debian / Astra Linux):

```bash
sudo apt update
sudo apt install -y samba
```

## 2. Создание пользователя `samba-admin`

Создайте сервисного системного пользователя с паролем:

```shell
sudo useradd -m -s /bin/bash samba-admin
sudo passwd samba-admin
sudo smbpasswd -a samba-admin
```

## 3. Настройка `sudoers`
Добавьте права для `samba-admin` на выполнение требуемых команд без ввода пароля `sudo`:

```shell
sudo visudo -f /etc/sudoers.d/samba-web-ui
```

Вставьте следующую строку (всё в одну линию):

```shell
samba-admin ALL=(ALL) NOPASSWD: /usr/sbin/useradd, /sbin/useradd, /usr/sbin/userdel, /sbin/userdel, /usr/sbin/chpasswd, /usr/bin/chpasswd, /usr/sbin/groupadd, /sbin/groupadd, /usr/sbin/groupdel, /sbin/groupdel, /usr/bin/gpasswd, /usr/bin/smbpasswd, /usr/bin/pdbedit, /bin/systemctl, /usr/bin/systemctl, /bin/cat, /usr/bin/cat, /bin/mv, /usr/bin/mv, /bin/cp, /usr/bin/cp, /bin/rm, /usr/bin/rm, /bin/mkdir, /usr/bin/mkdir, /bin/chmod, /usr/bin/chmod, /bin/chown, /usr/bin/chown, /usr/bin/smbstatus
```
*(Примечание: Добавлены команды `groupadd`, `groupdel`, `gpasswd` для управления составом групп)*

Защитите файл прав:

```shell
sudo chmod 0440 /etc/sudoers.d/samba-web-ui
```
