package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.ad.AdJoinRequestDto;
import mari.samba.dto.ad.AdStatusDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.ad.AdIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/ad", "/api/v1/ad"})
public class AdApiController {

  @Autowired private AdIntegrationService adIntegrationService;

  private String resolveSessionId(String headerSessionId, HttpSession session) {
    if (headerSessionId != null && !headerSessionId.isBlank()) {
      return headerSessionId;
    }
    return session != null ? session.getId() : null;
  }

  @GetMapping("/status")
  public ApiResponse<AdStatusDto> getStatus(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String headerSessionId,
      HttpSession session) {
    String sessionId = resolveSessionId(headerSessionId, session);
    return ApiResponse.ok(adIntegrationService.getStatus(sessionId));
  }

  @PostMapping("/join")
  public ApiResponse<AdStatusDto> joinDomain(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String headerSessionId,
      HttpSession session,
      @Valid @RequestBody AdJoinRequestDto request) {
    String sessionId = resolveSessionId(headerSessionId, session);
    return ApiResponse.ok(adIntegrationService.joinDomain(sessionId, request));
  }

  @PostMapping("/leave")
  public ApiResponse<AdStatusDto> leaveDomain(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String headerSessionId,
      HttpSession session,
      @Valid @RequestBody AdJoinRequestDto request) {
    String sessionId = resolveSessionId(headerSessionId, session);
    return ApiResponse.ok(adIntegrationService.leaveDomain(sessionId, request));
  }
}
