package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaMonitoringService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringApiController {

  private final SambaMonitoringService monitoringService;

  public MonitoringApiController(SambaMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @GetMapping("/dashboard")
  public ApiResponse<Map<String, Object>> getDashboard(HttpSession session) {
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

  @GetMapping("/status")
  public ApiResponse<Map<String, Object>> getServiceStatus(HttpSession session) {
    String sessionId = session.getId();
    boolean running = monitoringService.isServiceRunning(sessionId);
    Map<String, Object> status = new HashMap<>();
    status.put("running", running);
    return ApiResponse.ok(status);
  }

  @PostMapping("/control")
  public ApiResponse<Void> controlService(
      HttpSession session, @RequestParam("action") String action) {
    String sessionId = session.getId();
    monitoringService.controlService(sessionId, action);
    return ApiResponse.ok("Service control action '" + action + "' executed successfully", null);
  }

  @GetMapping("/connections")
  public ApiResponse<Object> getConnections(HttpSession session) {
    String sessionId = session.getId();
    return ApiResponse.ok(monitoringService.getActiveConnections(sessionId));
  }

  @GetMapping("/locks")
  public ApiResponse<Object> getLocks(HttpSession session) {
    String sessionId = session.getId();
    return ApiResponse.ok(monitoringService.getOpenFiles(sessionId));
  }

  @DeleteMapping({"/connections/{pid}", "/sessions/{pid}"})
  public ApiResponse<Void> killSession(HttpSession session, @PathVariable("pid") String pid) {
    String sessionId = session.getId();
    monitoringService.killSession(sessionId, pid);
    return ApiResponse.ok("Session PID " + pid + " terminated successfully", null);
  }

  @GetMapping("/disk")
  public ApiResponse<Object> getDisk(HttpSession session) {
    String sessionId = session.getId();
    return ApiResponse.ok(monitoringService.getDiskUsage(sessionId));
  }
}
