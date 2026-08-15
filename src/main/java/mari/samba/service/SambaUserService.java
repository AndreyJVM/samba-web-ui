package mari.samba.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.model.SambaUser;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SambaUserService {

    /**
     * Получить список всех пользователей Samba
     */
    public List<SambaUser> getAllUsers(Session session) throws Exception {
        String command = "sudo pdbedit -L -v";
        String output = executeCommand(session, command);

        List<SambaUser> users = new ArrayList<>();
        String[] lines = output.split("\n");

        for (String line : lines) {
            // Пример строки: "username:1000:User Name:Full Name:/home/username:/bin/bash"
            if (line.contains(":")) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    SambaUser user = new SambaUser();
                    user.setUsername(parts[0]);
                    // parts[1] - это SID, parts[2] - полное имя
                    if (parts.length > 2) {
                        user.setFullName(parts[2]);
                    }
                    users.add(user);
                }
            }
        }

        return users;
    }

    /**
     * Создать нового пользователя Samba
     */
    public void createUser(Session session, String username, String password, String fullName) throws Exception {
        // 1. Добавляем системного пользователя
        String addUserCmd = String.format("sudo useradd -m -s /bin/bash %s", username);
        executeCommand(session, addUserCmd);

        // 2. Устанавливаем пароль в системе
        String passwdCmd = String.format("echo '%s:%s' | sudo chpasswd", username, password);
        executeCommand(session, passwdCmd);

        // 3. Добавляем пользователя в Samba
        String smbPasswdCmd = String.format("echo '%s\n%s' | sudo smbpasswd -a %s", password, password, username);
        executeCommand(session, smbPasswdCmd);

        // 4. Включаем пользователя
        String enableCmd = String.format("sudo smbpasswd -e %s", username);
        executeCommand(session, enableCmd);
    }

    /**
     * Удалить пользователя Samba
     */
    public void deleteUser(Session session, String username) throws Exception {
        // 1. Удаляем из Samba
        String smbDelCmd = String.format("sudo smbpasswd -x %s", username);
        executeCommand(session, smbDelCmd);

        // 2. Удаляем системного пользователя
        String delUserCmd = String.format("sudo userdel -r %s", username);
        executeCommand(session, delUserCmd);
    }

    /**
     * Сменить пароль пользователя
     */
    public void changePassword(Session session, String username, String newPassword) throws Exception {
        // 1. Меняем пароль в системе
        String passwdCmd = String.format("echo '%s:%s' | sudo chpasswd", username, newPassword);
        executeCommand(session, passwdCmd);

        // 2. Меняем пароль в Samba
        String smbPasswdCmd = String.format("echo '%s\n%s' | sudo smbpasswd -a %s", newPassword, newPassword, username);
        executeCommand(session, smbPasswdCmd);
    }

    /**
     * Проверить, существует ли пользователь в системе
     */
    public boolean userExists(Session session, String username) throws Exception {
        String command = "id " + username + " 2>/dev/null && echo 'exists' || echo 'notfound'";
        String output = executeCommand(session, command);
        return output.trim().contains("exists");
    }

    /**
     * Добавить пользователя в valid users шары
     */
    public void addUserToShare(Session session, String shareName, String username, SambaShareService shareService) throws Exception {
        // Получаем текущую шару
        SambaShare share = shareService.getShareByName(session, shareName);

        // Формируем новый список valid users
        String currentValidUsers = share.getValidUsers();
        String newValidUsers;
        if (currentValidUsers == null || currentValidUsers.isEmpty()) {
            newValidUsers = username;
        } else {
            newValidUsers = currentValidUsers + "," + username;
        }

        // Обновляем шару
        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName(share.getName());
        dto.setPath(share.getPath());
        dto.setComment(share.getComment());
        dto.setReadOnly(share.isReadOnly());
        dto.setGuestOk(share.isGuestOk());
        dto.setValidUsers(newValidUsers);
        dto.setWriteList(share.getWriteList());
        dto.setCreateMask(share.getCreateMask());
        dto.setDirectoryMask(share.getDirectoryMask());
        dto.setForceUser(share.getForceUser());
        dto.setForceGroup(share.getForceGroup());

        shareService.updateShare(session, shareName, dto);
    }

    /**
     * Удалить пользователя из valid users шары
     */
    public void removeUserFromShare(Session session, String shareName, String username, SambaShareService shareService) throws Exception {
        // Получаем текущую шару
        SambaShare share = shareService.getShareByName(session, shareName);
        String currentValidUsers = share.getValidUsers();

        if (currentValidUsers == null || currentValidUsers.isEmpty()) {
            return;
        }

        // Удаляем пользователя из списка
        String[] users = currentValidUsers.split(",");
        StringBuilder newValidUsers = new StringBuilder();
        for (String user : users) {
            if (!user.trim().equals(username)) {
                if (newValidUsers.length() > 0) {
                    newValidUsers.append(",");
                }
                newValidUsers.append(user.trim());
            }
        }

        // Обновляем шару
        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName(share.getName());
        dto.setPath(share.getPath());
        dto.setComment(share.getComment());
        dto.setReadOnly(share.isReadOnly());
        dto.setGuestOk(share.isGuestOk());
        dto.setValidUsers(newValidUsers.toString());
        dto.setWriteList(share.getWriteList());
        dto.setCreateMask(share.getCreateMask());
        dto.setDirectoryMask(share.getDirectoryMask());
        dto.setForceUser(share.getForceUser());
        dto.setForceGroup(share.getForceGroup());

        shareService.updateShare(session, shareName, dto);
    }

    /**
     * Выполнить SSH-команду
     */
    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        channel.setOutputStream(outStream);
        channel.setErrStream(errStream);

        channel.connect();

        while (channel.isConnected()) {
            Thread.sleep(100);
        }

        String output = outStream.toString("UTF-8");
        String error = errStream.toString("UTF-8");

        if (!error.isEmpty()) {
            throw new RuntimeException("SSH Error: " + error);
        }

        return output;
    }
}