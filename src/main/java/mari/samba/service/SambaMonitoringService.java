package mari.samba.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SambaMonitoringService {

  @Autowired private CommandExecutor commandExecutor;

  public boolean isServiceRunning(String sessionId) {
    try {
      String output =
          commandExecutor.execute(sessionId, LinuxCommands.systemctl("is-active", "smbd"));
      return "active".equalsIgnoreCase(output.trim());
    } catch (Exception e) {
      return false;
    }
  }

  public List<Map<String, String>> getActiveConnections(String sessionId) {
    List<Map<String, String>> connections = new ArrayList<>();
    try {
      String output = commandExecutor.execute(sessionId, LinuxCommands.smbstatus("-b"));
      String[] lines = output.split("\\r?\\n");

      boolean parsingSessions = false;
      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("Samba version")) continue;
        if (line.contains("PID") && line.contains("User") && line.contains("Machine")) {
          parsingSessions = true;
          continue;
        }
        if (line.startsWith("---")) continue;
        if (line.isEmpty()) {
          parsingSessions = false;
          continue;
        }

        if (parsingSessions) {
          String[] parts = line.split("\\s+");
          if (parts.length >= 3) {
            Map<String, String> conn = new HashMap<>();
            conn.put("pid", parts[0]);
            conn.put("user", parts[1]);
            conn.put("machine", parts[2]);
            connections.add(conn);
          }
        }
      }
    } catch (Exception ignored) {
    }
    return connections;
  }

  public List<Map<String, String>> getOpenFiles(String sessionId) {
    List<Map<String, String>> openFiles = new ArrayList<>();
    try {
      String output = commandExecutor.execute(sessionId, LinuxCommands.smbstatus("-L"));
      String[] lines = output.split("\\r?\\n");

      boolean parsingFiles = false;
      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("Pid") && line.contains("DenyMode")) {
          parsingFiles = true;
          continue;
        }
        if (line.startsWith("---")) continue;
        if (line.isEmpty() || line.startsWith("Locked files:")) continue;

        if (parsingFiles) {
          String[] parts = line.split("\\s+");
          if (parts.length >= 8) {
            Map<String, String> file = new HashMap<>();
            file.put("pid", parts[0]);
            file.put("rw", parts[4]); // RDONLY, WRONLY, RDWR

            StringBuilder filePath = new StringBuilder();
            for (int i = 6; i < parts.length; i++) {
              if (parts[i].matches("Mon|Tue|Wed|Thu|Fri|Sat|Sun")) {
                break;
              }
              filePath.append(parts[i]).append(" ");
            }
            file.put("file", filePath.toString().trim());
            openFiles.add(file);
          }
        }
      }
    } catch (Exception ignored) {
    }
    return openFiles;
  }

  public void controlService(String sessionId, String action) throws Exception {
    if (!action.matches("restart|start|stop")) {
      throw new IllegalArgumentException("Недопустимое действие для службы: " + action);
    }
    commandExecutor.execute(sessionId, LinuxCommands.systemctl(action, "smbd"));
  }

  public void killSession(String sessionId, String pid) throws Exception {
    if (!pid.matches("^\\d+$")) {
      throw new IllegalArgumentException("Некорректный PID процесса: " + pid);
    }
    commandExecutor.execute(sessionId, LinuxCommands.kill(pid));
  }

  /** Получает статистику свободного места на дисках (/srv/samba или корня) */
  public Map<String, String> getDiskUsage(String sessionId) {
    Map<String, String> stats = new HashMap<>();
    try {
      // Выполняем df -h, ищем строку с / или монтированием /srv
      String output = commandExecutor.execute(sessionId, "df -h / | tail -n 1");
      String[] parts = output.trim().split("\\s+");
      if (parts.length >= 5) {
        stats.put("total", parts[1]);
        stats.put("used", parts[2]);
        stats.put("free", parts[3]);
        stats.put("percent", parts[4].replace("%", ""));
      }
    } catch (Exception e) {
      stats.put("total", "N/A");
      stats.put("used", "N/A");
      stats.put("free", "N/A");
      stats.put("percent", "0");
    }
    return stats;
  }
}
