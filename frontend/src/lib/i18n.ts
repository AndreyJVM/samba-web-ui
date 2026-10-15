import { useState, useEffect } from "react";

export type Language = "ru" | "en";

export const translations = {
  en: {
    common: {
      cancel: "Cancel",
      save: "Save",
      create: "Create",
      add: "Add",
      delete: "Delete",
      remove: "Remove",
      confirm: "Confirm",
      actions: "Actions",
      members: "Members",
      root: "root",
      browse: "Browse...",
      select: "Select",
      search: "Search shares, users, groups...",
      download: "Download"
    },
    sidebar: {
      dashboard: "Overview",
      shares: "Shares",
      users: "Users",
      groups: "Groups",
      ad: "Active Directory",
      settings: "Global Settings",
      fstec: "FSTEC Compliance",
      logout: "Sign Out",
      server: "Server",
      user: "User",
      running: "Running",
      stopped: "Stopped",
      wait: "Wait...",
      collapse: "Collapse",
      expand: "Expand",
      restart: "Restart"
    },
    dashboard: {
      title: "System Overview",
      serviceStatus: "Service Status",
      startService: "Start Service",
      stop: "Stop",
      restart: "Restart",
      activeSessions: "Active Sessions",
      clients: "clients",
      openFiles: "Open Files",
      locked: "locked",
      noFilesInUse: "No files in use",
      storageExplorer: "Storage & Explorer",
      total: "Total",
      used: "Used",
      free: "Free",
      mount: "Mount",
      mkdir: "New Directory",
      dirEmpty: "Directory is empty",
      dirname: "dirname"
    },
    shares: {
      title: "Network Shares",
      newShare: "New Share",
      noShares: "No shares available",
      noSharesDesc: "Create your first directory to share over the network.",
      createShare: "Create Share",
      shareName: "Share Name",
      diskPath: "Disk Path",
      access: "Access",
      comment: "Comment",
      browseable: "Browseable",
      readOnly: "Read Only",
      guestOk: "Guest Ok",
      validUsers: "Valid Users",
      validUsersHint: "Group names start with '@'.",
      externalGroups: "External Groups",
      localUsers: "Local Users",
      advOptions: "Advanced Options",
      writeList: "Write List",
      maxConn: "Max Connections",
      createMask: "Create Mask",
      dirMask: "Directory Mask",
      forceUser: "Force User",
      forceGroup: "Force Group",
      hostsAllow: "Hosts Allow",
      hostsDeny: "Hosts Deny",
      deleteConfirmTitle: "Delete Share",
      deleteConfirmMsg: "Are you sure you want to delete share '{name}'? Path '{path}' will NOT be removed from disk."
    },
    users: {
      title: "Users Management",
      newUser: "New User",
      noUsers: "No local users found.",
      username: "Username",
      password: "Password",
      status: "Status",
      enabled: "Enabled",
      disabled: "Disabled",
      deleteConfirmTitle: "Delete User",
      deleteConfirmMsg: "Are you sure you want to delete user '{name}'?",
      pwdChangeTitle: "Change Password for {name}",
      newPass: "New Password",
      enableUser: "Enable",
      disableUser: "Disable",
      changePassword: "Change Password"
    },
    groups: {
      title: "System Groups",
      newGroup: "New Group",
      noGroups: "No groups found.",
      groupName: "Group Name",
      gid: "GID",
      deleteConfirmTitle: "Delete Group",
      deleteConfirmMsg: "Are you sure you want to delete group '{name}'?",
      manageMembers: "Manage Members: {name}",
      availableUsers: "Available Users",
      noUsersInGroup: "No users in this group"
    },
    ad: {
      title: "Active Directory Integration",
      statusTitle: "Domain Status",
      connected: "Joined to Domain",
      notConnected: "Not Joined (Standalone Server)",
      realm: "Realm (DNS Domain)",
      workgroup: "Workgroup / NetBIOS Domain",
      domainController: "Domain Controller",
      adminUser: "Administrator Login",
      adminPass: "Administrator Password",
      joinDomain: "Join Domain",
      leaveDomain: "Leave Domain",
      idmapTitle: "IDMAP Settings",
      idmapDesc: "To configure UID/GID range mappings, visit the",
      idmapTab: "Global Settings",
      idmapDescPart2: "tab. Samba automatically maps domain SIDs to Linux UIDs based on those rules.",
      joinConfirmTitle: "Join Active Directory",
      joinConfirmMsg: "Are you sure you want to join domain {domain}? This will reconfigure Kerberos and Winbind.",
      leaveConfirmTitle: "Leave Active Directory",
      leaveConfirmMsg: "Are you sure you want to leave the domain? Domain users will lose access to files."
    },
    config: {
      title: "Global Configuration",
      general: "General",
      network: "Network",
      security: "Security",
      backups: "Configuration Backups",
      backupsDesc: "Restore the smb.conf to a previous state.",
      noBackups: "No backups found.",
      workgroup: "Workgroup",
      netbiosName: "NetBIOS Name",
      serverString: "Server String",
      loadPrinters: "Load Printers",
      interfaces: "Interfaces (Listen IP/Eth)",
      bindInterfacesOnly: "Bind Interfaces Only",
      disableNetbios: "Disable NetBIOS",
      securityModel: "Security Model",
      mapToGuest: "Map To Guest",
      minProtocol: "Min Protocol",
      maxProtocol: "Max Protocol",
      restore: "Restore",
      defaults: "Reset Defaults",
      applyConfig: "Apply Config",
      saving: "Saving...",
      autoGenerated: "Auto-generated if empty",
      activeDirectory: "Active Directory",
      never: "Never",
      badUser: "Bad User",
      badPassword: "Bad Password"
    },
    fstec: {
      title: "FSTEC Compliance",
      subtitle: "Security compliance analyzer according to FSTEC recommendations for Samba software",
      recheck: "Recheck",
      copyFixSnippet: "Copy smb.conf Fix Snippet",
      copied: "Copied to clipboard",
      filterAll: "All",
      filterFailed: "FAILED",
      filterOk: "OK",
      filterManual: "Manual",
      statusOk: "OK",
      statusFailed: "FAILED",
      statusManual: "MANUAL",
      statusNotApplicable: "N/A",
      severityHigh: "High",
      severityMedium: "Medium",
      severityLow: "Low",
      checkedAt: "Last checked",
      complianceScore: "Compliance Score",
      howToFix: "How to fix",
      consequences: "Potential consequences",
      sharesDetails: "Share details",
      shareName: "Share",
      shareStatus: "Status",
      shareValue: "Value",
      emptyRules: "No rules matching the selected filter",
      copyRuleFix: "Copy fix",
      notice: "This tool verifies configuration against FSTEC security guidelines. It is an informational check and does not replace official certification."
    }
  },
  ru: {
    common: {
      cancel: "Отмена",
      save: "Сохранить",
      create: "Создать",
      add: "Добавить",
      delete: "Удалить",
      remove: "Убрать",
      confirm: "Подтвердить",
      actions: "Действия",
      members: "Участники",
      root: "root",
      browse: "Обзор...",
      select: "Выбрать",
      search: "Поиск папок, пользователей, групп...",
      download: "Скачать"
    },
    sidebar: {
      dashboard: "Обзор",
      shares: "Папки",
      users: "Пользователи",
      groups: "Группы",
      ad: "Active Directory",
      settings: "Глобальные настройки",
      fstec: "ФСТЭК",
      logout: "Выход",
      server: "Сервер",
      user: "Пользователь",
      running: "Работает",
      stopped: "Остановлен",
      wait: "Ожидание...",
      collapse: "Свернуть",
      expand: "Развернуть",
      restart: "Перезапустить"
    },
    dashboard: {
      title: "Панель управления",
      serviceStatus: "Статус Samba",
      startService: "Запустить",
      stop: "Остановить",
      restart: "Перезапуск",
      activeSessions: "Активные сессии",
      clients: "клиентов",
      openFiles: "Открытые файлы",
      locked: "заблокировано",
      noFilesInUse: "Нет используемых файлов",
      storageExplorer: "Диски и Файлы",
      total: "Всего",
      used: "Занято",
      free: "Свободно",
      mount: "Монтирование",
      mkdir: "Новый ресурс",
      dirEmpty: "Папка пуста",
      dirname: "имя_папки"
    },
    shares: {
      title: "Сетевые папки",
      newShare: "Новый ресурс",
      noShares: "Нет общих ресурсов",
      noSharesDesc: "Создайте свою первую директорию для сетевого доступа (share).",
      createShare: "Создать ресурс",
      shareName: "Имя ресурса (Share Name)",
      diskPath: "Путь на диске",
      access: "Доступ",
      comment: "Описание",
      browseable: "Отображаемый (Browseable)",
      readOnly: "Только чтение (Read Only)",
      guestOk: "Гостевой доступ (Guest Ok)",
      validUsers: "Разрешенные пользователи",
      validUsersHint: "Имена групп начинаются с '@'.",
      externalGroups: "Внешние Группы",
      localUsers: "Локальные Пользователи",
      advOptions: "Тонкие настройки",
      writeList: "Список записи (write list)",
      maxConn: "Макс. подключений",
      createMask: "Маска создания файлов",
      dirMask: "Маска создания директорий",
      forceUser: "Принудительный пользователь (Force User)",
      forceGroup: "Принудительная группа (Force Group)",
      hostsAllow: "Разрешенные хосты (hosts allow)",
      hostsDeny: "Запрещенные хосты (hosts deny)",
      deleteConfirmTitle: "Удаление ресурса",
      deleteConfirmMsg: "Вы уверены, что хотите удалить сетевой ресурс '{name}'? Папка '{path}' останется на диске."
    },
    users: {
      title: "Пользователи Samba",
      newUser: "Новый пользователь",
      noUsers: "Локальные пользователи не найдены.",
      username: "Логин",
      password: "Пароль",
      status: "Статус",
      enabled: "Активен",
      disabled: "Отключен",
      deleteConfirmTitle: "Удаление пользователя",
      deleteConfirmMsg: "Удалить пользователя '{name}' из системы и Samba?",
      pwdChangeTitle: "Смена пароля: {name}",
      newPass: "Новый пароль",
      enableUser: "Включить",
      disableUser: "Отключить",
      changePassword: "Сменить пароль"
    },
    groups: {
      title: "Группы системы",
      newGroup: "Новая группа",
      noGroups: "Группы не найдены.",
      groupName: "Имя группы",
      gid: "GID",
      deleteConfirmTitle: "Удаление группы",
      deleteConfirmMsg: "Вы уверены, что хотите удалить группу '{name}'?",
      manageMembers: "Участники группы: {name}",
      availableUsers: "Доступные пользователи",
      noUsersInGroup: "В этой группе пока нет пользователей"
    },
    ad: {
      title: "Интеграция с Active Directory",
      statusTitle: "Статус подключения к Домену",
      connected: "В домене (Введен)",
      notConnected: "Не подключен (Автономный сервер)",
      realm: "Realm (DNS имя домена)",
      workgroup: "Рабочая группа (NetBIOS домен)",
      domainController: "Контроллер домена",
      adminUser: "Логин администратора",
      adminPass: "Пароль администратора",
      joinDomain: "Войти в Домен",
      leaveDomain: "Покинуть Домен",
      idmapTitle: "Настройки IDMAP",
      idmapDesc: "Для привязки диапазонов ID перейдите на вкладку",
      idmapTab: "Общие Настройки",
      idmapDescPart2: ". Samba автоматически назначает UID для пользователей AD на основе этих конфигураций.",
      joinConfirmTitle: "Ввод в Active Directory",
      joinConfirmMsg: "Уверены, что хотите вступить в домен {domain}? Это переконфигурирует Kerberos и Winbind.",
      leaveConfirmTitle: "Выход из Active Directory",
      leaveConfirmMsg: "Уверены, что хотите покинуть домен? Доступ к файлам по доменным учеткам пропадет."
    },
    config: {
      title: "Глобальные настройки",
      general: "Общие",
      network: "Сеть",
      security: "Безопасность",
      backups: "Резервные копии конфига",
      backupsDesc: "Откат smb.conf к сохраненному времени.",
      noBackups: "Резервные копии не найдены.",
      workgroup: "Рабочая группа",
      netbiosName: "Имя NetBIOS",
      serverString: "Описание сервера",
      loadPrinters: "Загрузка принтеров",
      interfaces: "Сетевые интерфейсы",
      bindInterfacesOnly: "Только выбранные интерфейсы",
      disableNetbios: "Отключить NetBIOS",
      securityModel: "Модель безопасности",
      mapToGuest: "Гостевой маппинг",
      minProtocol: "Мин. протокол",
      maxProtocol: "Макс. протокол",
      restore: "Восстановить",
      defaults: "Сбросить по умолчанию",
      applyConfig: "Применить",
      saving: "Сохранение...",
      autoGenerated: "Сгенерируется автоматически",
      activeDirectory: "Active Directory (ADS)",
      never: "Никогда (Never)",
      badUser: "Плохой польз. (Bad User)",
      badPassword: "Плохой пароль (Bad Password)"
    },
    fstec: {
      title: "ФСТЭК",
      subtitle: "Анализатор соответствия конфигурации Samba рекомендациям ФСТЭК России по безопасной настройке",
      recheck: "Перепроверить",
      copyFixSnippet: "Скопировать фрагмент smb.conf",
      copied: "Скопировано в буфер обмена",
      filterAll: "Все",
      filterFailed: "FAILED",
      filterOk: "OK",
      filterManual: "Вручную",
      statusOk: "OK",
      statusFailed: "FAILED",
      statusManual: "Вручную",
      statusNotApplicable: "N/A",
      severityHigh: "Высокая",
      severityMedium: "Средняя",
      severityLow: "Низкая",
      checkedAt: "Проверено",
      complianceScore: "Соответствие",
      howToFix: "Как исправить",
      consequences: "Последствия изменения",
      sharesDetails: "Детализация по ресурсам",
      shareName: "Ресурс",
      shareStatus: "Статус",
      shareValue: "Значение",
      emptyRules: "Нет правил, соответствующих выбранному фильтру",
      copyRuleFix: "Копировать",
      notice: "Вкладка выполняет аудит конфигурации согласно методическим рекомендациям ФСТЭК России. Проверка носит информационный характер и не заменяет официальную сертификацию или аттестацию."
    }
  }
};

let currentLang: Language = (localStorage.getItem("samba_lang") as Language) || "ru";
const listeners = new Set<(lang: Language) => void>();

export const setLanguage = (lang: Language) => {
  currentLang = lang;
  localStorage.setItem("samba_lang", lang);
  listeners.forEach(l => l(lang));
};

export const getLanguage = () => currentLang;

export const useTranslation = () => {
  const [lang, setLang] = useState<Language>(currentLang);
  
  useEffect(() => {
    listeners.add(setLang);
    return () => {
      listeners.delete(setLang);
    };
  }, []);

  const t = (key: string): string => {
    const keys = key.split(".");
    let result: any = translations[lang];
    for (const k of keys) {
      if (result && typeof result === "object" && k in result) {
        result = result[k];
      } else {
        result = undefined;
        break;
      }
    }
    if (result === undefined) {
      result = translations["en"];
      for (const k of keys) {
        if (result && typeof result === "object" && k in result) {
          result = result[k];
        } else {
          return key;
        }
      }
    }
    return typeof result === "string" ? result : key;
  };

  return { t, lang, setLanguage };
};
