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
  public List<String> getDiskUsage(String sessionId) {
    List<String> stats = new ArrayList<>();
    try {
      String output = commandExecutor.execute(sessionId, "df -h");
      String[] lines = output.trim().split("\\r?\\n");
      for (int i = 1; i < lines.length; i++) {
        String[] parts = lines[i].trim().split("\\s+");
        if (parts.length >= 6) {
          stats.add(parts[5] + " " + parts[4] + " " + parts[1]);
        }
      }
    } catch (Exception e) {
    }
    return stats;
  }
}
