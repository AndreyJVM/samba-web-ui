package mari.samba.service;

import mari.samba.model.SambaUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SambaUserService {

    @Autowired
    private CommandExecutor commandExecutor;

    public List<SambaUser> getAllUsers(String sessionId) throws Exception {
        String output = commandExecutor.execute(sessionId, "sudo pdbedit -L");

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

    public void createUser(String sessionId, String username, String password, String fullName) throws Exception {
        String cleanUsername = username.trim();
        String cleanPassword = password.trim();

        String comment = (fullName != null && !fullName.isBlank())
                ? fullName.trim().replace("\"", "\\\"")
                : cleanUsername;

        commandExecutor.execute(sessionId, String.format("sudo useradd -m -s /bin/bash -c \"%s\" %s", comment, cleanUsername));
        commandExecutor.execute(sessionId, "sudo chpasswd", cleanUsername + ":" + cleanPassword + "\n");
        commandExecutor.execute(sessionId, "sudo smbpasswd -s -a " + cleanUsername, cleanPassword + "\n" + cleanPassword + "\n");
        commandExecutor.execute(sessionId, "sudo smbpasswd -e " + cleanUsername);
    }

    public void deleteUser(String sessionId, String username) throws Exception {
        try {
            commandExecutor.execute(sessionId, "sudo smbpasswd -x " + username);
        } catch (Exception ignored) {
        }
        commandExecutor.execute(sessionId, "sudo userdel -r " + username);
    }

    public void changePassword(String sessionId, String username, String newPassword) throws Exception {
        commandExecutor.execute(sessionId, "sudo chpasswd", username + ":" + newPassword + "\n");
        commandExecutor.execute(sessionId, "sudo smbpasswd -s " + username, newPassword + "\n" + newPassword + "\n");
    }

    public boolean userExists(String sessionId, String username) {
        try {
            commandExecutor.execute(sessionId, "id " + username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}