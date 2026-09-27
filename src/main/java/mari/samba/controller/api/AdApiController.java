package mari.samba.controller.api;

import jakarta.validation.Valid;
import mari.samba.dto.ad.AdJoinRequestDto;
import mari.samba.dto.ad.AdStatusDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.ad.AdIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ad")
public class AdApiController {

  @Autowired private AdIntegrationService adIntegrationService;

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<AdStatusDto>> getStatus(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String sessionId) {
    return ResponseEntity.ok(ApiResponse.ok(adIntegrationService.getStatus(sessionId)));
  }

  @PostMapping("/join")
  public ResponseEntity<ApiResponse<AdStatusDto>> joinDomain(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String sessionId,
      @Valid @RequestBody AdJoinRequestDto request) {
    return ResponseEntity.ok(ApiResponse.ok(adIntegrationService.joinDomain(sessionId, request)));
  }

  @PostMapping("/leave")
  public ResponseEntity<ApiResponse<AdStatusDto>> leaveDomain(
      @RequestHeader(value = "X-Ssh-Session-Id", required = false) String sessionId,
      @Valid @RequestBody AdJoinRequestDto request) {
    return ResponseEntity.ok(ApiResponse.ok(adIntegrationService.leaveDomain(sessionId, request)));
  }
}
