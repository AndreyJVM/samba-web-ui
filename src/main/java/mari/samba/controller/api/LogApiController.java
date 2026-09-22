package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Operations for retrieving Samba service logs")
public class LogApiController {

  @Autowired private SambaLogService logService;

  @GetMapping("/raw")
  @Operation(
      summary = "Fetch raw logs",
      description = "Retrieves the most recent log lines from the Samba service systemd journal.")
  public ResponseEntity<ApiResponse<String>> fetchRawLogs(
      @Parameter(description = "Number of recent log lines to fetch", example = "100")
          @RequestParam(defaultValue = "100")
          int lines,
      HttpSession httpSession) {
    String sessionId = httpSession.getId();
    try {
      String logs = logService.getRecentLogs(sessionId, lines);
      return ResponseEntity.ok(ApiResponse.ok(logs));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }
}
