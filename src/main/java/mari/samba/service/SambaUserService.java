package mari.samba.service;

import com.jcraft.jsch.Session;
import mari.samba.model.SambaUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SambaUserService {

    @Autowired
    private SshSessionManager sessionManager;

    public List<SambaUser> getAllUsers(Session session) throws Exception {
        // Формат pdbedit -L: username:uid:Full Name (или просто username:uid)
        String output = sessionManager.executeCommand(session, "sudo pdbedit -L");

        List<SambaUser> users = new ArrayList<>();
        String[] lines = output.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(":");
            if (parts.length >= 2) {
                SambaUser user = new SambaUser();
                user.setUsername(parts[0].trim());
                if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                    user.setFullName(parts[2].trim());
                } else {
                    user.setFullName("-");
                }
                user.setAccountEnabled(true);
                users.add(user);
            }
        }
        return users;
    }

    public void createUser(Session session, String username, String password, String fullName) throws Exception {
        // 1. Создаем системного пользователя
        String comment = (fullName != null && !fullName.isBlank()) ? fullName.replace("\"", "\\\"") : username;
        String addUserCmd = String.format("sudo useradd -m -s /bin/bash -c \"%s\" %s", comment, username);
        sessionManager.executeCommand(session, addUserCmd);

        // 2. Устанавливаем системный пароль через stdin
        sessionManager.executeCommand(session, "sudo chpasswd", username + ":" + password + "\n");

        // 3. Добавляем и включаем в Samba
        sessionManager.executeCommand(session, "sudo smbpasswd -s -a " + username, password + "\n" + password + "\n");
        sessionManager.executeCommand(session, "sudo smbpasswd -e " + username);
    }

    public void deleteUser(Session session, String username) throws Exception {
        try {
            sessionManager.executeCommand(session, "sudo smbpasswd -x " + username);
        } catch (Exception ignored) {
            // Игнорируем, если пользователя не было в pdbedit
        }
        sessionManager.executeCommand(session, "sudo userdel -r " + username);
    }

    public void changePassword(Session session, String username, String newPassword) throws Exception {
        sessionManager.executeCommand(session, "sudo chpasswd", username + ":" + newPassword + "\n");
        sessionManager.executeCommand(session, "sudo smbpasswd -s " + username, newPassword + "\n" + newPassword + "\n");
    }

    public boolean userExists(Session session, String username) {
        try {
            sessionManager.executeCommand(session, "id " + username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}