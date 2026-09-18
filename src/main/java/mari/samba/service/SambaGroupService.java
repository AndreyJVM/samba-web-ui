package mari.samba.service;

import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Сервис для управления Linux-группами через Samba Web UI.
 * <p>
 * Все группы, создаваемые через приложение, получают префикс {@code smb_}.
 * Это позволяет безопасно изолировать их от системных сервисных групп ОС
 * (таких как root, daemon, sys) и избежать случайных поломок системы.
 * При выводе в пользовательский интерфейс префикс скрывается.
 */
@Service
public class SambaGroupService {

    /** Внутренний префикс для групп в Linux. */
    private static final String GROUP_PREFIX = "smb_";

    @Autowired
    private CommandExecutor commandExecutor;

    /**
     * Получает список всех SMB-групп, управляемых приложением.
     *
     * @param sessionId Идентификатор SSH-сессии пользователя.
     * @return Список объектов SambaGroup, где имена очищены от префикса.
     * @throws Exception в случае ошибки выполнения команды по SSH.
     */
    public List<SambaGroup> getAllGroups(String sessionId) throws Exception {
        List<SambaGroup> groups = new ArrayList<>();
        // Выполняем getent group. Формат вывода: groupname:password:gid:userlist
        String output = commandExecutor.execute(sessionId, "getent group");
        if (output == null || output.isBlank()) {
            return groups; 
        }

        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            String[] parts = line.split(":", -1); 
            if (parts.length >= 3) {
                String groupName = parts[0];
                
                if (!groupName.startsWith(GROUP_PREFIX)) {
                    continue; // Пропускаем все системные и не наши группы
                }

                String displayName = groupName.substring(GROUP_PREFIX.length());

                List<String> userList = new ArrayList<>();
                if (parts.length >= 4 && !parts[3].isBlank()) {
                    userList = Arrays.asList(parts[3].split(","));
                }
                
                groups.add(new SambaGroup(displayName, userList));
            }
        }
        return groups;
    }

    /**
     * Создает новую группу в операционной системе.
     * 
     * @param sessionId Идентификатор SSH-сессии.
     * @param dto DTO с информацией о создаваемой группе.
     * @throws Exception при ошибке создания.
     */
    public void createGroup(String sessionId, SambaGroupCreateDto dto) throws Exception {
        String fullGroupName = GROUP_PREFIX + dto.groupName();
        String command = String.format("sudo groupadd %s", escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    /**
     * Удаляет группу из операционной системы.
     *
     * @param sessionId Идентификатор SSH-сессии.
     * @param groupName Имя группы (без префикса).
     * @throws Exception при ошибке удаления.
     */
    public void deleteGroup(String sessionId, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo groupdel %s", escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    /**
     * Добавляет существующего пользователя в группу.
     *
     * @param sessionId Идентификатор SSH-сессии.
     * @param username Имя пользователя.
     * @param groupName Имя группы (без префикса).
     * @throws Exception при ошибке выполнения.
     */
    public void addUserToGroup(String sessionId, String username, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo gpasswd -a %s %s", escape(username), escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    /**
     * Исключает пользователя из группы.
     *
     * @param sessionId Идентификатор SSH-сессии.
     * @param username Имя пользователя.
     * @param groupName Имя группы (без префикса).
     * @throws Exception при ошибке выполнения.
     */
    public void removeUserFromGroup(String sessionId, String username, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo gpasswd -d %s %s", escape(username), escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    /**
     * Экранирует аргумент для безопасной подстановки в shell (защита от Command Injection).
     *
     * @param arg Строка для экранирования.
     * @return Экранированная строка в одинарных кавычках.
     */
    private String escape(String arg) {
        if (arg == null) return "''";
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}
