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
      int pidIdx = 0,
          userIdx = 1,
          machineIdx =
              3; // Р’ СЃРѕРІСЂРµРјРµРЅРЅС‹С… Samba РјРµР¶РґСѓ User Рё Machine РёРґРµС‚ Group

      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("Samba version")) continue;

        // Р§РёС‚Р°РµРј Р·Р°РіРѕР»РѕРІРѕРє, С‡С‚РѕР±С‹ РїРѕРЅСЏС‚СЊ РіРґРµ СЂР°СЃРїРѕР»РѕР¶РµРЅС‹
        // РєРѕР»РѕРЅРєРё (РІРµСЂСЃРёРё Samba РЅРµРјРЅРѕРіРѕ РѕС‚Р»РёС‡Р°СЋС‚СЃСЏ)
        if (line.contains("PID") && line.contains("Username")) {
          parsingSessions = true;
          String[] headers = line.split("\\s+");
          for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase("PID")) pidIdx = i;
            if (headers[i].equalsIgnoreCase("Username")) userIdx = i;
            if (headers[i].equalsIgnoreCase("Machine")) machineIdx = i;
          }
          continue;
        }

        if (line.startsWith("---")) continue;
        if (line.isEmpty()) {
          parsingSessions = false;
          continue;
        }

        if (parsingSessions) {
          String[] parts = line.split("\\s+");
          if (parts.length > Math.max(userIdx, machineIdx)) {
            Map<String, String> conn = new HashMap<>();
            conn.put("pid", parts[pidIdx]);
            conn.put("user", parts[userIdx]);
            conn.put("machine", parts[machineIdx]);
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
          // Р¤РѕСЂРјР°С‚: Pid, Uid, DenyMode, Access, R/W, Oplock, SharePath, Name, Time
          // Name РЅР°С‡РёРЅР°РµС‚СЃСЏ СЃ 7-РіРѕ РёРЅРґРµРєСЃР°.
          if (parts.length >= 8) {
            Map<String, String> file = new HashMap<>();
            file.put("pid", parts[0]);
            file.put("rw", parts[4]); // RDONLY, WRONLY, RDWR

            StringBuilder fileName = new StringBuilder();
            // РЎРєР»РµРёРІР°РµРј РёРјСЏ С„Р°Р№Р»Р°, С‚Р°Рє РєР°Рє РѕРЅРѕ РјРѕР¶РµС‚
            // СЃРѕРґРµСЂР¶Р°С‚СЊ РїСЂРѕР±РµР»С‹ (parts СЃ 7 РёРЅРґРµРєСЃР°)
            for (int i = 7; i < parts.length; i++) {
              // РРјРµРЅР° С„Р°Р№Р»РѕРІ Р·Р°РєР°РЅС‡РёРІР°СЋС‚СЃСЏ, РєРѕРіРґР° РЅР°С‡РёРЅР°РµС‚СЃСЏ
              // Time (Р”РµРЅСЊ РЅРµРґРµР»Рё)
              if (parts[i].matches("Mon|Tue|Wed|Thu|Fri|Sat|Sun")) {
                break;
              }
              fileName.append(parts[i]).append(" ");
            }
            file.put("file", fileName.toString().trim());
            openFiles.add(file);
          }
        }
      }
    } catch (Exception ignored) {
    }
    return openFiles;
  }

  public void controlService(String sessionId, String action) {
    if (!action.matches("restart|start|stop")) {
      throw new IllegalArgumentException(
          "РќРµРґРѕРїСѓСЃС‚РёРјРѕРµ РґРµР№СЃС‚РІРёРµ РґР»СЏ СЃР»СѓР¶Р±С‹: " + action);
    }
    commandExecutor.execute(sessionId, LinuxCommands.systemctl(action, "smbd"));
  }

  public void killSession(String sessionId, String pid) {
    if (!pid.matches("^\\d+$")) {
      throw new IllegalArgumentException("РќРµРєРѕСЂСЂРµРєС‚РЅС‹Р№ PID РїСЂРѕС†РµСЃСЃР°: " + pid);
    }
    commandExecutor.execute(sessionId, LinuxCommands.kill(pid));
  }

  /**
   * РџРѕР»СѓС‡Р°РµС‚ СЃС‚Р°С‚РёСЃС‚РёРєСѓ СЃРІРѕР±РѕРґРЅРѕРіРѕ РјРµСЃС‚Р° РЅР° РґРёСЃРєР°С…
   * (/srv/samba РёР»Рё РєРѕСЂРЅСЏ)
   */
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
