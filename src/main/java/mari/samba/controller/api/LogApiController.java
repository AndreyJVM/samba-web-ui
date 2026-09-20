package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogApiController {

  @Autowired private SambaLogService logService;

  @GetMapping("/raw")
  public ResponseEntity<ApiResponse<String>> fetchRawLogs(
      @RequestParam(defaultValue = "100") int lines, HttpSession httpSession) {
    String sessionId = httpSession.getId();
    try {
      String logs = logService.getRecentLogs(sessionId, lines);
      return ResponseEntity.ok(ApiResponse.ok(logs));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }
}
