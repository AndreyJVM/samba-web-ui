package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.ad.AdJoinRequestDto;
import mari.samba.dto.ad.AdStatusDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.ad.AdIntegrationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ad")
public class AdApiController {

  private final AdIntegrationService adService;

  public AdApiController(AdIntegrationService adService) {
    this.adService = adService;
  }

  @GetMapping("/status")
  public ApiResponse<AdStatusDto> getStatus(HttpSession session) {
    String sessionId = session.getId();
    AdStatusDto status = adService.getStatus(sessionId);
    return ApiResponse.ok(status);
  }

  @PostMapping("/join")
  public ApiResponse<AdStatusDto> joinDomain(
      HttpSession session, @Valid @RequestBody AdJoinRequestDto request) {
    String sessionId = session.getId();
    AdStatusDto status = adService.joinDomain(sessionId, request);
    return ApiResponse.ok("Successfully joined domain", status);
  }

  @PostMapping("/leave")
  public ApiResponse<AdStatusDto> leaveDomain(
      HttpSession session, @Valid @RequestBody AdJoinRequestDto request) {
    String sessionId = session.getId();
    AdStatusDto status = adService.leaveDomain(sessionId, request);
    return ApiResponse.ok("Successfully left domain", status);
  }
}
