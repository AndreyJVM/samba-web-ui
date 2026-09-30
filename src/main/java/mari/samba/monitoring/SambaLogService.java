package mari.samba.monitoring;

import java.util.Set;
import mari.samba.config.SambaProperties;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaLogService {

  private static final Logger log = LoggerFactory.getLogger(SambaLogService.class);

  public static final String DEFAULT_LOG_PATH = "/var/log/samba/log.smbd";
  private static final Set<Integer> ALLOWED_LINE_COUNTS = Set.of(50, 100, 200, 500, 1000);

  private final CommandExecutor commandExecutor;
  private final String defaultLogPath;

  public SambaLogService(
      CommandExecutor commandExecutor, @Autowired(required = false) SambaProperties properties) {
    this.commandExecutor = commandExecutor;
    this.defaultLogPath =
        properties != null && properties.paths() != null && properties.paths().log() != null
            ? properties.paths().log()
            : DEFAULT_LOG_PATH;
  }

  public String getRecentLogs(String sessionId, int lines) {
    return getRecentLogs(sessionId, defaultLogPath, lines);
  }

  public String getRecentLogs(String sessionId, String logPath, int lines) {
    int safeLines = ALLOWED_LINE_COUNTS.contains(lines) ? lines : 50;
    String targetPath =
        (logPath == null || logPath.isBlank()) ? defaultLogPath : logPath.trim().replace('\\', '/');

    try {
      return commandExecutor.execute(sessionId, LinuxCommands.tail(targetPath, safeLines));
    } catch (Exception e) {
      log.warn("Failed to read Samba logs from {}: {}", targetPath, e.getMessage());
      return "Log file empty or not accessible at " + targetPath + ": " + e.getMessage();
    }
  }
}
