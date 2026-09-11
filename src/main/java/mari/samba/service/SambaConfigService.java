package mari.samba.service;

import mari.samba.dto.config.SambaBackupDto;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import mari.samba.service.parser.SmbConfParser;
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
        return commandExecutor.execute(sessionId, LinuxCommands.cat(SMB_CONF_PATH));
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
        commandExecutor.execute(sessionId, LinuxCommands.mkdir(BACKUP_DIR));
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFile = BACKUP_DIR + "/smb.conf.backup_" + timestamp;

        commandExecutor.execute(sessionId, LinuxCommands.copy(SMB_CONF_PATH, backupFile));
        commandExecutor.execute(sessionId, LinuxCommands.cleanupOldBackups(BACKUP_DIR, 10));
    }

    public List<SambaBackupDto> listBackups(String sessionId) {
        List<SambaBackupDto> backups = new ArrayList<>();
        try {
            String cmd = LinuxCommands.listBackupsDetailed(BACKUP_DIR);
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

        commandExecutor.execute(sessionId, LinuxCommands.copy(backupFile, SMB_CONF_PATH));
        commandExecutor.execute(sessionId, LinuxCommands.systemctl("restart", "smbd"));
    }

    public void updateSmbConf(String sessionId, String content) throws Exception {
        String tempFile = "/tmp/smb.conf.tmp";

        createBackup(sessionId);
        commandExecutor.execute(sessionId, LinuxCommands.writeToFileStdin(tempFile), content);
        commandExecutor.execute(sessionId, LinuxCommands.testparmSilent(tempFile));
        commandExecutor.execute(sessionId, LinuxCommands.move(tempFile, SMB_CONF_PATH));
        commandExecutor.execute(sessionId, LinuxCommands.systemctl("restart", "smbd"));
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