package mari.samba.service;

import java.util.ArrayList;
import java.util.List;
import mari.samba.model.SambaUser;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaUserService {

  @Autowired private CommandExecutor commandExecutor;

  public List<SambaUser> getAllUsers(String sessionId) throws Exception {
    String output = commandExecutor.execute(sessionId, LinuxCommands.listSambaUsers());

    List<SambaUser> users = new ArrayList<>();
    String[] lines = output.split("\\r?\\n");

    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) continue;

      String[] parts = line.split(":");
      if (parts.length >= 2) {
        SambaUser user = new SambaUser();
        user.setUsername(parts[0].trim());
        user.setFullName((parts.length > 2 && !parts[2].trim().isEmpty()) ? parts[2].trim() : "-");
        user.setAccountEnabled(true);
        users.add(user);
      }
    }
    return users;
  }

  public void createUser(String sessionId, String username, String password, String fullName)
      throws Exception {
    String cleanUsername = username.trim();
    String cleanPassword = password.trim();
    String comment = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanUsername;

    commandExecutor.execute(sessionId, LinuxCommands.addSystemUserWithHome(cleanUsername, comment));
    commandExecutor.execute(
        sessionId, LinuxCommands.chpasswd(), cleanUsername + ":" + cleanPassword + "\n");
    commandExecutor.execute(
        sessionId,
        LinuxCommands.addSambaUser(cleanUsername),
        cleanPassword + "\n" + cleanPassword + "\n");
    commandExecutor.execute(sessionId, LinuxCommands.enableSambaUser(cleanUsername));
  }

  public void deleteUser(String sessionId, String username) throws Exception {
    try {
      commandExecutor.execute(sessionId, LinuxCommands.deleteSambaUser(username));
    } catch (Exception ignored) {
    }
    commandExecutor.execute(sessionId, LinuxCommands.deleteSystemUser(username));
  }

  public void changePassword(String sessionId, String username, String newPassword)
      throws Exception {
    commandExecutor.execute(
        sessionId, LinuxCommands.chpasswd(), username + ":" + newPassword + "\n");
    commandExecutor.execute(
        sessionId,
        LinuxCommands.changeSambaPassword(username),
        newPassword + "\n" + newPassword + "\n");
  }

  public boolean userExists(String sessionId, String username) {
    try {
      commandExecutor.execute(sessionId, LinuxCommands.checkUserExists(username));
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
