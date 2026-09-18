package mari.samba.service;

import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SambaGroupService {

    private static final String GROUP_PREFIX = "smb_";

    @Autowired
    private CommandExecutor commandExecutor;

    public List<SambaGroup> getAllGroups(String sessionId) throws Exception {
        List<SambaGroup> groups = new ArrayList<>();
        // Выполняем getent group для получения списка системных групп
        String output = commandExecutor.execute(sessionId, "getent group");
        if (output == null || output.isBlank()) {
            return groups; // Если ничего не вернулось, отдаем пустой список
        }

        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String groupName = parts[0];
                
                // Вариант 2: Строгая привязка к префиксу smb_
                // Показываем в UI только те группы, которыми управляет наше приложение
                if (!groupName.startsWith(GROUP_PREFIX)) {
                    continue;
                }

                // Для UI мы можем отрезать префикс, чтобы было красиво (smb_managers -> managers)
                String displayName = groupName.substring(GROUP_PREFIX.length());

                String usersRaw = parts[3];
                List<String> userList = new ArrayList<>();
                if (!usersRaw.isBlank()) {
                    userList = Arrays.asList(usersRaw.split(","));
                }
                
                groups.add(new SambaGroup(displayName, userList));
            }
        }
        return groups;
    }

    public void createGroup(String sessionId, SambaGroupCreateDto dto) throws Exception {
        // Добавляем префикс перед созданием
        String fullGroupName = GROUP_PREFIX + dto.groupName();
        String command = String.format("sudo groupadd %s", escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    public void deleteGroup(String sessionId, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo groupdel %s", escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    public void addUserToGroup(String sessionId, String username, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo gpasswd -a %s %s", escape(username), escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    public void removeUserFromGroup(String sessionId, String username, String groupName) throws Exception {
        String fullGroupName = GROUP_PREFIX + groupName;
        String command = String.format("sudo gpasswd -d %s %s", escape(username), escape(fullGroupName));
        commandExecutor.execute(sessionId, command);
    }

    private String escape(String arg) {
        if (arg == null) return "''";
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}
