package mari.samba.service;

import mari.samba.dto.SambaBackupDto;
import mari.samba.dto.SambaGlobalConfigDto;
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

    @Autowired
    private SmbConfParser smbConfParser;

    public String getSmbConfContent(String sessionId) throws Exception {
        return commandExecutor.execute(sessionId, "cat " + SMB_CONF_PATH);
    }

    public List<SambaShare> parseShares(String content) {
        return smbConfParser.parseShares(content);
    }

    public String buildShareSection(SambaShareCreateDto dto) {
        return smbConfParser.buildShareSection(dto);
    }

    public String removeShareSection(String content, String shareName) {
        return smbConfParser.removeSection(content, shareName);
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

    public SambaGlobalConfigDto getGlobalConfig(String sessionId) throws Exception {
        String content = getSmbConfContent(sessionId);
        return smbConfParser.parseGlobalConfig(content);
    }

    public void updateGlobalConfig(String sessionId, SambaGlobalConfigDto dto) throws Exception {
        String currentContent = getSmbConfContent(sessionId);
        String updatedContent = smbConfParser.updateGlobalSection(currentContent, dto);
        updateSmbConf(sessionId, updatedContent);
    }
}