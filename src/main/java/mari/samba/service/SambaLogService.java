package mari.samba.service;

import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SambaLogService {

    private static final String DEFAULT_LOG_PATH = "/var/log/samba/log.smbd";
    private static final List<Integer> ALLOWED_LINE_COUNTS = Arrays.asList(50, 100, 200, 500);

    @Autowired
    private CommandExecutor commandExecutor;

    /**
     * Получение последних N строк из лога smbd
     */
    public String getRecentLogs(String sessionId, int linesCount) throws Exception {
        int safeLines = ALLOWED_LINE_COUNTS.contains(linesCount) ? linesCount : 100;
        String command = String.format("sudo tail -n %d %s", safeLines, DEFAULT_LOG_PATH);

        try {
            return commandExecutor.execute(sessionId, command);
        } catch (Exception e) {
            return "Не удалось прочитать лог-файл " + DEFAULT_LOG_PATH + ": " + e.getMessage();
        }
    }
}