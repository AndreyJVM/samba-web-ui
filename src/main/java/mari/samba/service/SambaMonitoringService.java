package mari.samba.service;

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
            String output = commandExecutor.execute(sessionId, "sudo systemctl is-active smbd");
            return "active".equalsIgnoreCase(output.trim());
        } catch (Exception e) {
            return false;
        }
    }

    public List<Map<String, String>> getActiveConnections(String sessionId) {
        List<Map<String, String>> connections = new ArrayList<>();
        try {
            String output = commandExecutor.execute(sessionId, "sudo smbstatus -b");
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

    public void controlService(String sessionId, String action) throws Exception {
        if (!action.matches("restart|start|stop")) {
            throw new IllegalArgumentException("Недопустимое действие для службы: " + action);
        }
        commandExecutor.execute(sessionId, "sudo systemctl " + action + " smbd");
    }
}