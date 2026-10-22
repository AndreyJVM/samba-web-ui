# Локальный режим без SSH

Локальный режим управляет Samba на том же Linux-сервере, где работает ShareGuard. Первая версия
пакета предназначена для Ubuntu, Debian и Astra Linux 1.8. Удалённый режим через SSH остаётся
доступным и используется по умолчанию, пока локальный пароль не настроен.

В локальной установке работают две службы:

- `shareguard` запускает веб-приложение от системного пользователя `shareguard`;
- `shareguard-helper` выполняет только известные приложению административные операции от `root`.

Службы обмениваются типизированными JSON-запросами через
`/run/shareguard/helper.sock`. Helper не принимает произвольные shell-команды, проверяет UID клиента,
имена пользователей и групп, пути, сервисы и параметры операций. Символические ссылки разрешены,
только если их итоговый путь находится внутри одного из разрешённых корней.

## Режимы выполнения

Параметр `SHAREGUARD_EXECUTION_MODE` принимает следующие значения:

| Значение | Поведение |
| --- | --- |
| `REMOTE` | Все команды выполняются через SSH. В форме входа требуются host и SSH-реквизиты. |
| `LOCAL` | Команды выполняются локально. В форме входа требуются локальные имя пользователя и пароль ShareGuard. |
| `AUTO` | Выбирает `LOCAL`, если настроен локальный пароль; в противном случае выбирает `REMOTE`. |

Значение по умолчанию — `AUTO`. Оно сохраняет прежнее SSH-поведение для существующих установок.

## Установка готового пакета

Скачайте готовый DEB-пакет из [GitHub Release](https://github.com/AndreyJVM/shareguard/releases/tag/v0.10.0)
и установите его на Samba-сервере. Сборка проекта и установка Node.js/npm не требуются:

```bash
curl -fL -o shareguard_0.10.0_all.deb \
  https://github.com/AndreyJVM/shareguard/releases/download/v0.10.0/shareguard_0.10.0_all.deb
sudo apt install ./shareguard_0.10.0_all.deb
sudo systemctl status shareguard-helper shareguard
```

Во время первой установки создаются системный пользователь и группа `shareguard`, а также случайный
пароль локального администратора. Имя пользователя и пароль доступны только `root`:

```bash
sudo grep -E '^SHAREGUARD_EXECUTION_LOCAL_(USERNAME|PASSWORD)=' /etc/shareguard/shareguard.env
```

Откройте `http://127.0.0.1:8080/ui/`. Локальная учётная запись ShareGuard не является учётной
записью Linux или Samba. Для удалённого доступа используйте SSH-туннель:

```bash
ssh -L 8080:127.0.0.1:8080 administrator@samba-server
```

После подключения откройте `http://127.0.0.1:8080/ui/` на рабочем компьютере.

Для разовой проверки с другой машины в изолированной тестовой сети доступен
[временный проброс через socat](temporary-access.md).

## Параметры

| Переменная окружения | Значение по умолчанию | Назначение |
| --- | --- | --- |
| `SHAREGUARD_EXECUTION_MODE` | `AUTO` | Выбор локального или удалённого выполнения. |
| `SHAREGUARD_EXECUTION_LOCAL_USERNAME` | `admin` | Имя пользователя для входа в локальном режиме. |
| `SHAREGUARD_EXECUTION_LOCAL_PASSWORD` | пусто | Пароль локального режима. Пустое значение не включает локальный режим в `AUTO`. |
| `SHAREGUARD_EXECUTION_COMMAND_TIMEOUT` | `30s` | Максимальная длительность локальной команды. |
| `SHAREGUARD_EXECUTION_HELPER_SOCKET` | `/run/shareguard/helper.sock` | Unix-сокет, используемый веб-приложением. |
| `SHAREGUARD_HELPER_SOCKET` | `/run/shareguard/helper.sock` | Unix-сокет, создаваемый helper. |
| `SHAREGUARD_ALLOWED_ROOTS` | `/mnt:/media:/srv:/data:/home` | Разрешённые корни файловых операций helper, разделённые двоеточием. |
| `SAMBA_SECURITY_ALLOWED_ROOTS` | `/mnt,/media,/srv,/data,/home` | Те же корни для предварительной проверки в backend, разделённые запятыми. |
| `SERVER_ADDRESS` | `127.0.0.1` в пакете | Адрес HTTP-сервера. |

Параметры пакета находятся в `/etc/shareguard/shareguard.env`. После изменения настроек перезапустите
обе службы:

```bash
sudo systemctl restart shareguard-helper shareguard
```

Значения `SHAREGUARD_ALLOWED_ROOTS` и `SAMBA_SECURITY_ALLOWED_ROOTS` должны описывать одинаковый
набор каталогов. Helper выполняет окончательную проверку после разрешения символических ссылок.

## Диагностика и удаление

```bash
sudo journalctl -u shareguard-helper -u shareguard
sudo apt remove shareguard
```

Обычное удаление сохраняет `/etc/shareguard/shareguard.env`. Для удаления конфигурации используйте
`sudo apt purge shareguard`.
