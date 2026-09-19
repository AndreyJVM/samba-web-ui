package mari.samba.controller.web;

/**
 * Централизованное хранилище всех маршрутов (ручек/эндпоинтов) веб-приложения.
 * Используется для избежания "магических строк" в контроллерах.
 */
public final class WebRoutes {

    private WebRoutes() {
        // Утилитный класс
    }

    // --- Общие ---
    public static final String ROOT = "/";
    public static final String CONNECT = "/connect";
    public static final String DISCONNECT = "/disconnect";

    // --- Пользователи ---
    public static final String USERS = "/users";
    public static final String USERS_CREATE_MAPPING = "/create";
    public static final String USERS_DELETE_MAPPING = "/delete";
    public static final String USERS_CHANGE_PASSWORD_MAPPING = "/change-password/{username}";
    public static final String USERS_PASSWORD_MAPPING = "/password";

    // --- Группы ---
    public static final String GROUPS = "/groups";
    public static final String GROUPS_CREATE_MAPPING = "/create";
    public static final String GROUPS_DELETE_MAPPING = "/delete";
    public static final String GROUPS_MEMBERS_MAPPING = "/{groupname}/members";

    // --- Общие папки (Shares) ---
    public static final String SHARES = "/shares";
    public static final String SHARES_CREATE_MAPPING = "/create";
    public static final String SHARES_EDIT_MAPPING = "/edit/{sharename}";
    public static final String SHARES_DELETE_MAPPING = "/delete";

    // --- Конфигурация ---
    public static final String CONFIG = "/config";
    public static final String CONFIG_GLOBAL_MAPPING = "/global";
    public static final String CONFIG_RESTORE_MAPPING = "/restore";

    // --- Мониторинг ---
    public static final String STATUS = "/status";
    public static final String STATUS_KILL_SESSION_MAPPING = "/kill";
    public static final String STATUS_CONTROL_MAPPING = "/control";

    // --- Логи ---
    public static final String LOGS = "/logs";
}
