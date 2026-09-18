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
                
                // Фильтруем. Обычно пользовательские группы начинаются с GID >= 1000, 
                // но для Samba-веба нас интересуют любые группы, созданные вручную.
                // Для начала давайте возвращать стандартные пользовательские группы + созданные нами.
                int gid;
                try {
                    gid = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    continue;
                }
                
                // Пропускаем служебные системные группы (GID < 1000)
                if (gid < 1000 && gid != 0) {
                   continue;
                }

                String usersRaw = parts[3];
                List<String> userList = new ArrayList<>();
                if (!usersRaw.isBlank()) {
                    userList = Arrays.asList(usersRaw.split(","));
                }
                
                groups.add(new SambaGroup(groupName, userList));
            }
        }
        return groups;
    }

    public void createGroup(String sessionId, SambaGroupCreateDto dto) throws Exception {
        // Команда добавления группы в ОС
        String command = String.format("sudo groupadd %s", escape(dto.groupName()));
        commandExecutor.execute(sessionId, command);
    }

    public void deleteGroup(String sessionId, String groupName) throws Exception {
        String command = String.format("sudo groupdel %s", escape(groupName));
        commandExecutor.execute(sessionId, command);
    }

    public void addUserToGroup(String sessionId, String username, String groupName) throws Exception {
        String command = String.format("sudo gpasswd -a %s %s", escape(username), escape(groupName));
        commandExecutor.execute(sessionId, command);
    }

    public void removeUserFromGroup(String sessionId, String username, String groupName) throws Exception {
        String command = String.format("sudo gpasswd -d %s %s", escape(username), escape(groupName));
        commandExecutor.execute(sessionId, command);
    }

    private String escape(String arg) {
        if (arg == null) return "''";
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}
