package mari.samba.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mari.samba.config.SambaProperties;
import mari.samba.dto.config.SambaBackupDto;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import mari.samba.service.parser.SmbConfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaConfigService {

  private static final Logger log = LoggerFactory.getLogger(SambaConfigService.class);

  public static final String SMB_CONF_PATH = "/etc/samba/smb.conf";
  public static final String BACKUP_DIR = "/etc/samba/backups";

  private final CommandExecutor commandExecutor;
  private final SmbConfParser smbConfParser;
  private final String configPath;
  private final String backupDir;

  public SambaConfigService(
      CommandExecutor commandExecutor,
      SmbConfParser smbConfParser,
      @Autowired(required = false) SambaProperties properties) {
    this.commandExecutor = commandExecutor;
    this.smbConfParser = smbConfParser;
    this.configPath =
        properties != null && properties.paths() != null && properties.paths().config() != null
            ? properties.paths().config()
            : SMB_CONF_PATH;
    this.backupDir =
        properties != null && properties.paths() != null && properties.paths().backups() != null
            ? properties.paths().backups()
            : BACKUP_DIR;
  }

  public String getSmbConfContent(String sessionId) {
    return commandExecutor.execute(sessionId, LinuxCommands.cat(configPath));
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

  public void createBackup(String sessionId) {
    commandExecutor.execute(sessionId, LinuxCommands.mkdir(backupDir));
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String backupFile = backupDir + "/smb.conf.backup_" + timestamp;

    commandExecutor.execute(sessionId, LinuxCommands.copy(configPath, backupFile));
    commandExecutor.execute(sessionId, LinuxCommands.cleanupOldBackups(backupDir, 10));
    log.info("Created configuration backup: {}", backupFile);
  }

  public List<SambaBackupDto> listBackups(String sessionId) {
    List<SambaBackupDto> backups = new ArrayList<>();
    try {
      String cmd = LinuxCommands.listBackupsDetailed(backupDir);
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
    } catch (Exception e) {
      log.warn("Failed to list configuration backups: {}", e.getMessage());
    }
    return backups;
  }

  public void restoreBackup(String sessionId, String filename) {
    if (!filename.matches("^smb\\.conf\\.backup_\\d{8}_\\d{6}$")) {
      throw new IllegalArgumentException("Operation failed due to an error.");
    }

    String backupFile = backupDir + "/" + filename;
    createBackup(sessionId);

    commandExecutor.execute(sessionId, LinuxCommands.copy(backupFile, configPath));
    commandExecutor.execute(sessionId, LinuxCommands.systemctl("restart", "smbd"));
    log.info("Restored configuration from backup {} and restarted smbd", filename);
  }

  public void updateSmbConf(String sessionId, String content) {
    String tempFile = "/tmp/smb.conf.tmp";

    createBackup(sessionId);
    commandExecutor.execute(sessionId, LinuxCommands.writeToFileStdin(tempFile), content);
    commandExecutor.execute(sessionId, LinuxCommands.testparmSilent(tempFile));
    commandExecutor.execute(sessionId, LinuxCommands.move(tempFile, configPath));
    commandExecutor.execute(sessionId, LinuxCommands.systemctl("restart", "smbd"));
    log.info("Updated {} and restarted smbd", configPath);
  }

  public SambaGlobalConfigDto getGlobalConfig(String sessionId) {
    String content = getSmbConfContent(sessionId);
    return smbConfParser.parseGlobalConfig(content);
  }

  public void updateGlobalConfig(String sessionId, SambaGlobalConfigDto dto) {
    String currentContent = getSmbConfContent(sessionId);
    String updatedContent = smbConfParser.updateGlobalSection(currentContent, dto);
    updateSmbConf(sessionId, updatedContent);
  }
}
