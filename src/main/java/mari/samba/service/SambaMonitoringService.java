package mari.samba.service;

import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SambaMonitoringService {

    @Autowired
    private CommandExecutor commandExecutor;

    public boolean isServiceRunning(String sessionId) {
        try {
            String output = commandExecutor.execute(sessionId, LinuxCommands.systemctl("is-active", "smbd"));
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

    /**
     * Получает список открытых/заблокированных файлов (smbstatus -L)
     */
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
                    // Вывод smbstatus -L содержит много колонок, PID - 0-я, R/W режим - 4-я.
                    // Путь к файлу начинается примерно с 6-й колонки
                    if (parts.length >= 8) {
                        Map<String, String> file = new HashMap<>();
                        file.put("pid", parts[0]);
                        file.put("rw", parts[4]); // RDONLY, WRONLY, RDWR

                        // Собираем путь к файлу (учитываем возможные пробелы в названиях)
                        StringBuilder filePath = new StringBuilder();
                        for (int i = 6; i < parts.length; i++) {
                            // Отсекаем дату в конце строки (обычно начинается с дня недели - Mon, Tue...)
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

    /**
     * Принудительно завершает сессию пользователя (Kill Switch)
     */
    public void killSession(String sessionId, String pid) throws Exception {
        // Жесткая валидация: PID должен состоять только из цифр
        if (!pid.matches("^\\d+$")) {
            throw new IllegalArgumentException("Некорректный PID процесса: " + pid);
        }
        commandExecutor.execute(sessionId, LinuxCommands.kill(pid));
    }
}