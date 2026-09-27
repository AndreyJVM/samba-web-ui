package mari.samba.service;

import java.util.Arrays;
import java.util.List;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaLogService {

  private static final String DEFAULT_LOG_PATH = "/var/log/samba/log.smbd";
  private static final List<Integer> ALLOWED_LINE_COUNTS = Arrays.asList(50, 100, 200, 500);

  @Autowired private CommandExecutor commandExecutor;

  public String getRecentLogs(String sessionId, int linesCount) {
    int safeLines = ALLOWED_LINE_COUNTS.contains(linesCount) ? linesCount : 100;
    String command = LinuxCommands.tail(DEFAULT_LOG_PATH, safeLines);

    try {
      return commandExecutor.execute(sessionId, command);
    } catch (Exception e) {
      return "Operation failed due to an error." + DEFAULT_LOG_PATH + ": " + e.getMessage();
    }
  }
}
