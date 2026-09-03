package mari.samba.service;

import mari.samba.dto.SambaBackupDto;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SambaConfigService {

    private static final String SMB_CONF_PATH = "/etc/samba/smb.conf";
    private static final String BACKUP_DIR = "/etc/samba/backups";

    @Autowired
    private CommandExecutor commandExecutor;

    public String getSmbConfContent(String sessionId) throws Exception {
        return commandExecutor.execute(sessionId, "cat " + SMB_CONF_PATH);
    }

    public void createBackup(String sessionId) throws Exception {
        commandExecutor.execute(sessionId, "sudo mkdir -p " + BACKUP_DIR);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFile = BACKUP_DIR + "/smb.conf.backup_" + timestamp;

        commandExecutor.execute(sessionId, "sudo cp " + SMB_CONF_PATH + " " + backupFile);
        commandExecutor.execute(sessionId,
                "ls -t " + BACKUP_DIR + "/smb.conf.backup_* 2>/dev/null | tail -n +11 | xargs -r sudo rm --");
    }

    public List<SambaBackupDto> listBackups(String sessionId) {
        List<SambaBackupDto> backups = new ArrayList<>();
        try {
            String cmd = "ls -lh --time-style=\"+%Y-%m-%d %H:%M:%S\" " + BACKUP_DIR + "/smb.conf.backup_* 2>/dev/null";
            String output = commandExecutor.execute(sessionId, cmd);
            String[] lines = output.split("\\r?\\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("total")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 7) {
                    String size = parts[4];
                    String date = parts[5] + " " + parts[6];
                    String fullPath = parts[parts.length - 1];
                    String filename = fullPath.substring(fullPath.lastIndexOf('/') + 1);

                    backups.add(new SambaBackupDto(filename, date, size));
                }
            }
        } catch (Exception ignored) {
        }
        return backups;
    }

    public void restoreBackup(String sessionId, String filename) throws Exception {
        if (!filename.matches("^smb\\.conf\\.backup_\\d{8}_\\d{6}$")) {
            throw new IllegalArgumentException("Некорректное имя файла бэкапа");
        }

        String backupFile = BACKUP_DIR + "/" + filename;
        createBackup(sessionId);

        commandExecutor.execute(sessionId, "sudo cp " + backupFile + " " + SMB_CONF_PATH);
        commandExecutor.execute(sessionId, "sudo systemctl restart smbd");
    }

    public void updateSmbConf(String sessionId, String content) throws Exception {
        String tempFile = "/tmp/smb.conf.tmp";

        createBackup(sessionId);
        commandExecutor.execute(sessionId, "cat > " + tempFile, content);
        commandExecutor.execute(sessionId, "testparm -s " + tempFile + " > /dev/null");
        commandExecutor.execute(sessionId, "sudo mv " + tempFile + " " + SMB_CONF_PATH);
        commandExecutor.execute(sessionId, "sudo systemctl restart smbd");
    }

    public String buildShareSection(SambaShareCreateDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(dto.getName()).append("]\n");
        sb.append("   path = ").append(dto.getPath()).append("\n");

        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            sb.append("   comment = ").append(dto.getComment()).append("\n");
        }

        sb.append("   read only = ").append(dto.isReadOnly() ? "yes" : "no").append("\n");
        sb.append("   guest ok = ").append(dto.isGuestOk() ? "yes" : "no").append("\n");
        sb.append("   browseable = ").append(dto.isBrowseable() ? "yes" : "no").append("\n");

        if (dto.getValidUsers() != null && !dto.getValidUsers().isBlank()) {
            sb.append("   valid users = ").append(dto.getValidUsers()).append("\n");
        }
        if (dto.getWriteList() != null && !dto.getWriteList().isBlank()) {
            sb.append("   write list = ").append(dto.getWriteList()).append("\n");
        }
        if (dto.getCreateMask() != null && !dto.getCreateMask().isBlank()) {
            sb.append("   create mask = ").append(dto.getCreateMask()).append("\n");
        }
        if (dto.getDirectoryMask() != null && !dto.getDirectoryMask().isBlank()) {
            sb.append("   directory mask = ").append(dto.getDirectoryMask()).append("\n");
        }
        if (dto.getForceUser() != null && !dto.getForceUser().isBlank()) {
            sb.append("   force user = ").append(dto.getForceUser()).append("\n");
        }
        if (dto.getForceGroup() != null && !dto.getForceGroup().isBlank()) {
            sb.append("   force group = ").append(dto.getForceGroup()).append("\n");
        }
        if (dto.getMaxConnections() != null && !dto.getMaxConnections().isBlank()) {
            sb.append("   max connections = ").append(dto.getMaxConnections()).append("\n");
        }
        if (dto.getHostsAllow() != null && !dto.getHostsAllow().isBlank()) {
            sb.append("   hosts allow = ").append(dto.getHostsAllow()).append("\n");
        }
        if (dto.getHostsDeny() != null && !dto.getHostsDeny().isBlank()) {
            sb.append("   hosts deny = ").append(dto.getHostsDeny()).append("\n");
        }

        return sb.toString();
    }

    public String removeShareSection(String content, String shareName) {
        String[] lines = content.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        boolean insideTargetSection = false;
        boolean shareFound = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String sectionName = trimmed.substring(1, trimmed.length() - 1);
                if (sectionName.equalsIgnoreCase(shareName)) {
                    insideTargetSection = true;
                    shareFound = true;
                    continue;
                } else {
                    insideTargetSection = false;
                }
            }

            if (!insideTargetSection) {
                result.append(line).append("\n");
            }
        }

        if (!shareFound) {
            throw new RuntimeException("Шара с именем '" + shareName + "' не найдена в smb.conf");
        }

        return result.toString();
    }

    public List<SambaShare> parseShares(String content) {
        List<SambaShare> shares = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        SambaShare currentShare = null;
        boolean insideShare = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String sectionName = trimmed.substring(1, trimmed.length() - 1);
                if (!sectionName.equalsIgnoreCase("global") &&
                        !sectionName.equalsIgnoreCase("homes") &&
                        !sectionName.equalsIgnoreCase("printers")) {

                    if (currentShare != null && currentShare.getName() != null) {
                        shares.add(currentShare);
                    }
                    currentShare = new SambaShare();
                    currentShare.setName(sectionName);
                    insideShare = true;
                } else {
                    if (currentShare != null && currentShare.getName() != null) {
                        shares.add(currentShare);
                    }
                    insideShare = false;
                    currentShare = null;
                }
                continue;
            }

            if (insideShare && currentShare != null && trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (key) {
                    case "path": currentShare.setPath(value); break;
                    case "comment": currentShare.setComment(value); break;
                    case "read only": currentShare.setReadOnly("yes".equalsIgnoreCase(value)); break;
                    case "guest ok": currentShare.setGuestOk("yes".equalsIgnoreCase(value)); break;
                    case "browseable": currentShare.setBrowseable("yes".equalsIgnoreCase(value)); break;
                    case "valid users": currentShare.setValidUsers(value); break;
                    case "write list": currentShare.setWriteList(value); break;
                    case "create mask": currentShare.setCreateMask(value); break;
                    case "directory mask": currentShare.setDirectoryMask(value); break;
                    case "force user": currentShare.setForceUser(value); break;
                    case "force group": currentShare.setForceGroup(value); break;
                    case "max connections": currentShare.setMaxConnections(value); break;
                    case "hosts allow": currentShare.setHostsAllow(value); break;
                    case "hosts deny": currentShare.setHostsDeny(value); break;
                }
            }
        }

        if (currentShare != null && currentShare.getName() != null) {
            shares.add(currentShare);
        }

        return shares;
    }
}