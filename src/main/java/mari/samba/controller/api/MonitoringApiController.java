package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringApiController {

  @Autowired private SambaMonitoringService monitoringService;

  @GetMapping("/dashboard")
  public ApiResponse<Map<String, Object>> getDashboard(HttpSession session) {
    String sessionId = session.getId();
    boolean isRunning = monitoringService.isServiceRunning(sessionId);

    java.util.Map<String, Object> responseData = new java.util.HashMap<>();
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

  @PostMapping("/control")
  public ApiResponse<Void> controlService(HttpSession session, @RequestParam String action)
      throws Exception {
    String sessionId = session.getId();
    monitoringService.controlService(sessionId, action);
    return ApiResponse.ok("Команда '" + action + "' успешно выполнена", null);
  }

  @DeleteMapping("/sessions/{pid}")
  public ApiResponse<Void> killSession(HttpSession session, @PathVariable String pid)
      throws Exception {
    String sessionId = session.getId();
    monitoringService.killSession(sessionId, pid);
    return ApiResponse.ok("Сессия (PID: " + pid + ") успешно завершена", null);
  }
}
