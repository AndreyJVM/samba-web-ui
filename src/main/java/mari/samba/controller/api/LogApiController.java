package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogApiController {

  @Autowired private SambaLogService logService;

  @GetMapping("/raw")
  public ApiResponse<String> fetchRawLogs(
      @RequestParam(defaultValue = "100") int lines, HttpSession httpSession) throws Exception {
    String sessionId = httpSession.getId();
    String logs = logService.getRecentLogs(sessionId, lines);
    return ApiResponse.ok(logs);
  }
}
