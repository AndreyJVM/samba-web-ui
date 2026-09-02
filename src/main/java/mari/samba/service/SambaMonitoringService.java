package mari.samba.service;

import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SambaMonitoringService {

    @Autowired
    private SshSessionManager sessionManager;

    /**
     * Проверяет статус службы smbd (active/inactive)
     */
    public boolean isServiceRunning(Session session) {
        try {
            String output = sessionManager.executeCommand(session, "sudo systemctl is-active smbd");
            return "active".equalsIgnoreCase(output.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получает список активных подключений через smbstatus -b (brief)
     */
    public List<Map<String, String>> getActiveConnections(Session session) {
        List<Map<String, String>> connections = new ArrayList<>();
        try {
            // smbstatus -b выводит краткую информацию о соединениях
            String output = sessionManager.executeCommand(session, "sudo smbstatus -b");
            String[] lines = output.split("\\r?\\n");

            boolean parsingSessions = false;
            for (String line : lines) {
                line = line.trim();
                // Ищем начало секции сессий в выводе smbstatus
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
                    // Пример строки: 12345   user1   192.168.1.50   IPv4     (unix charset)
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
        } catch (Exception e) {
            // Если smbstatus недоступен или нет активных подключений
        }
        return connections;
    }

    /**
     * Управление службой (restart / start / stop)
     */
    public void controlService(Session session, String action) throws Exception {
        if (!action.matches("restart|start|stop")) {
            throw new IllegalArgumentException("Недопустимое действие для службы");
        }
        sessionManager.executeCommand(session, "sudo systemctl " + action + " smbd");
    }
}