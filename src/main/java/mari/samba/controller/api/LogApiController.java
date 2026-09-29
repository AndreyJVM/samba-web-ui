package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogApiController {

  private final SambaLogService logService;

  public LogApiController(SambaLogService logService) {
    this.logService = logService;
  }

  @GetMapping
  public ApiResponse<Map<String, String>> getLogs(
      HttpSession session,
      @RequestParam(value = "lines", defaultValue = "100") int lines,
      @RequestParam(value = "path", required = false) String path) {
    String sessionId = session.getId();
    String content =
        (path != null && !path.isBlank())
            ? logService.getRecentLogs(sessionId, path, lines)
            : logService.getRecentLogs(sessionId, lines);
    Map<String, String> data = new HashMap<>();
    data.put("content", content);
    return ApiResponse.ok("Logs retrieved successfully", data);
  }

  @GetMapping("/raw")
  public ApiResponse<String> getRawLogs(
      HttpSession session,
      @RequestParam(value = "lines", defaultValue = "100") int lines,
      @RequestParam(value = "path", required = false) String path) {
    String sessionId = session.getId();
    String content =
        (path != null && !path.isBlank())
            ? logService.getRecentLogs(sessionId, path, lines)
            : logService.getRecentLogs(sessionId, lines);
    return ApiResponse.ok("Logs retrieved successfully", content);
  }
}
