package mari.samba.monitoring;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for providing dashboard metrics and Samba monitoring information.
 *
 * <p>Exposes endpoints to check the status of the Samba service, control its state (start, stop,
 * restart), view active client connections, open file locks, and terminate specific sessions.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringApiController {

    private final SambaMonitoringService monitoringService;

    /**
     * Constructs a new {@link MonitoringApiController}.
     *
     * @param monitoringService the service responsible for querying and managing Samba status
     */
    public MonitoringApiController(SambaMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Retrieves aggregated dashboard monitoring metrics.
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing a map of dashboard metrics (disk, connections, open
     *     files)
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard(@NonNull HttpSession session) {
        String sessionId = session.getId();
        boolean isRunning = monitoringService.isServiceRunning(sessionId);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("isRunning", isRunning);
        responseData.put("diskUsage", monitoringService.getDiskUsage(sessionId));

        if (isRunning) {
            responseData.put("connections", monitoringService.getActiveConnections(sessionId));
            responseData.put("openFiles", monitoringService.getOpenFiles(sessionId));
        } else {
            responseData.put("connections", List.of());
            responseData.put("openFiles", List.of());
        }

        return ApiResponse.ok(responseData);
    }

    /**
     * Retrieves the current running status of the Samba service (smbd).
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing a boolean 'running' flag
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getServiceStatus(@NonNull HttpSession session) {
        String sessionId = session.getId();
        boolean running = monitoringService.isServiceRunning(sessionId);
        Map<String, Object> status = new HashMap<>();
        status.put("running", running);
        return ApiResponse.ok(status);
    }

    /**
     * Executes a control action (e.g., start, stop, restart) on the Samba service.
     *
     * @param session the user's current HTTP session
     * @param action the command to pass (start, stop, restart)
     * @return an {@link ApiResponse} indicating successful execution
     */
    @PostMapping("/control")
    public ApiResponse<Void> controlService(@NonNull HttpSession session, @RequestParam("action") String action) {
        String sessionId = session.getId();
        monitoringService.controlService(sessionId, action);
        return ApiResponse.ok("Service control action '" + action + "' executed successfully", null);
    }

    /**
     * Retrieves a list of active Samba connections (smbstatus).
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing the connections collection
     */
    @GetMapping("/connections")
    public ApiResponse<Object> getConnections(@NonNull HttpSession session) {
        String sessionId = session.getId();
        return ApiResponse.ok(monitoringService.getActiveConnections(sessionId));
    }

    /**
     * Retrieves a list of currently open files or locks held by Samba clients.
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing the locked files collection
     */
    @GetMapping("/locks")
    public ApiResponse<Object> getLocks(@NonNull HttpSession session) {
        String sessionId = session.getId();
        return ApiResponse.ok(monitoringService.getOpenFiles(sessionId));
    }

    /**
     * Terminates a specific active Samba session by its Process ID (PID).
     *
     * @param session the user's current HTTP session
     * @param pid the PID of the worker handling the connection
     * @return an {@link ApiResponse} indicating successful termination
     */
    @DeleteMapping({"/connections/{pid}", "/sessions/{pid}"})
    public ApiResponse<Void> killSession(@NonNull HttpSession session, @PathVariable("pid") String pid) {
        String sessionId = session.getId();
        monitoringService.killSession(sessionId, pid);
        return ApiResponse.ok("Session PID " + pid + " terminated successfully", null);
    }

    /**
     * Retrieves the current disk usage statistics of the server.
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing the disk usage payload
     */
    @GetMapping("/disk")
    public ApiResponse<Object> getDisk(@NonNull HttpSession session) {
        String sessionId = session.getId();
        return ApiResponse.ok(monitoringService.getDiskUsage(sessionId));
    }
}
