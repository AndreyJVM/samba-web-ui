package mari.samba.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaGroupService {

  private static final String GROUP_PREFIX = "smb_";

  @Autowired private CommandExecutor commandExecutor;

  public List<SambaGroup> getAllGroups(String sessionId) {
    List<SambaGroup> groups = new ArrayList<>();

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
          continue;
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

  public void createGroup(String sessionId, SambaGroupCreateDto dto) {
    String fullGroupName = GROUP_PREFIX + dto.groupName();
    String command = String.format("sudo groupadd %s", escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  public void deleteGroup(String sessionId, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command = String.format("sudo groupdel %s", escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  public void addUserToGroup(String sessionId, String username, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command =
        String.format("sudo gpasswd -a %s %s", escape(username), escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  public void removeUserFromGroup(String sessionId, String username, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command =
        String.format("sudo gpasswd -d %s %s", escape(username), escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  private String escape(String arg) {
    if (arg == null) return "''";
    return "'" + arg.replace("'", "'\\''") + "'";
  }
}
