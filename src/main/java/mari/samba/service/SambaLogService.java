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

  /** РџРѕР»СѓС‡РµРЅРёРµ РїРѕСЃР»РµРґРЅРёС… N СЃС‚СЂРѕРє РёР· Р»РѕРіР° smbd */
  public String getRecentLogs(String sessionId, int linesCount) {
    int safeLines = ALLOWED_LINE_COUNTS.contains(linesCount) ? linesCount : 100;
    String command = LinuxCommands.tail(DEFAULT_LOG_PATH, safeLines);

    try {
      return commandExecutor.execute(sessionId, command);
    } catch (Exception e) {
      return "РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕС‡РёС‚Р°С‚СЊ Р»РѕРі-С„Р°Р№Р» "
          + DEFAULT_LOG_PATH
          + ": "
          + e.getMessage();
    }
  }
}
