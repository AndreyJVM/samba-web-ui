package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
@Tag(
    name = "Monitoring",
    description = "Operations for monitoring and controlling the Samba service")
public class MonitoringApiController {

  @Autowired private SambaMonitoringService monitoringService;

  @GetMapping("/dashboard")
  @Operation(
      summary = "Get dashboard metrics",
      description =
          "Retrieves current Samba service status, disk usage, active connections, and open files.")
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
  @Operation(
      summary = "Control Samba service",
      description =
          "Execute systemctl commands (start, stop, restart, reload) on the smbd service.")
  public ApiResponse<Void> controlService(
      HttpSession session,
      @Parameter(description = "Action to perform (start, stop, restart)", example = "restart")
          @RequestParam
          String action)
      throws Exception {
    String sessionId = session.getId();
    monitoringService.controlService(sessionId, action);
    return ApiResponse.ok("Команда '" + action + "' успешно выполнена", null);
  }

  @DeleteMapping("/sessions/{pid}")
  @Operation(
      summary = "Kill user session",
      description = "Kills a specific SMB user connection by its Process ID.")
  public ApiResponse<Void> killSession(
      HttpSession session,
      @Parameter(description = "PID of the smbd process to kill", example = "12345") @PathVariable
          String pid)
      throws Exception {
    String sessionId = session.getId();
    monitoringService.killSession(sessionId, pid);
    return ApiResponse.ok("Сессия (PID: " + pid + ") успешно завершена", null);
  }
}
