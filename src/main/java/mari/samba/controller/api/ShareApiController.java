package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shares")
public class ShareApiController {

  private final SambaShareService shareService;

  public ShareApiController(SambaShareService shareService) {
    this.shareService = shareService;
  }

  @GetMapping
  public ApiResponse<List<SambaShare>> getAllShares(HttpSession session) {
    List<SambaShare> shares = shareService.getAllShares(session.getId());
    return ApiResponse.ok(shares);
  }

  @GetMapping("/{sharename}")
  public ApiResponse<SambaShare> getShare(@PathVariable String sharename, HttpSession session) {
    SambaShare share = shareService.getShareByName(session.getId(), sharename);
    return ApiResponse.ok(share);
  }

  @GetMapping("/{sharename}/size")
  public ApiResponse<String> getShareSize(@PathVariable String sharename, HttpSession session) {
    String size = shareService.getShareSize(session.getId(), sharename);
    return ApiResponse.ok("Share size calculated successfully", size);
  }

  @PostMapping
  public ApiResponse<Void> createShare(
      HttpSession session, @Valid @RequestBody SambaShareCreateDto dto) {
    shareService.createShare(session.getId(), dto);
    return ApiResponse.ok("Share '" + dto.getName() + "' created successfully", null);
  }

  @PutMapping("/{sharename}")
  public ApiResponse<Void> updateShare(
      HttpSession session,
      @PathVariable String sharename,
      @Valid @RequestBody SambaShareCreateDto dto) {
    shareService.updateShare(session.getId(), sharename, dto);
    return ApiResponse.ok("Share '" + sharename + "' updated successfully", null);
  }

  @DeleteMapping("/{sharename}")
  public ApiResponse<Void> deleteShare(HttpSession session, @PathVariable String sharename) {
    shareService.deleteShare(session.getId(), sharename);
    return ApiResponse.ok("Share '" + sharename + "' deleted successfully", null);
  }
}
