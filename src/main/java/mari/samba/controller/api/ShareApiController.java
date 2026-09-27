package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shares")
public class ShareApiController {

  @Autowired private SambaShareService shareService;

  @GetMapping
  public ApiResponse<List<SambaShare>> getAllShares(HttpSession session) {
    String sessionId = session.getId();
    List<SambaShare> shares = shareService.getAllShares(sessionId);
    return ApiResponse.ok(shares);
  }

  @GetMapping("/{sharename}")
  public ApiResponse<SambaShare> getShare(@PathVariable String sharename, HttpSession session) {
    String sessionId = session.getId();
    SambaShare share = shareService.getShareByName(sessionId, sharename);
    return ApiResponse.ok(share);
  }

  @PostMapping
  public ApiResponse<Void> createShare(
      HttpSession session, @Valid @RequestBody SambaShareCreateDto dto) {
    String sessionId = session.getId();
    shareService.createShare(sessionId, dto);
    return ApiResponse.ok("РћР±С‰Р°СЏ РїР°РїРєР° '" + dto.getName() + "' СЃРѕР·РґР°РЅР°", null);
  }

  @PutMapping("/{sharename}")
  public ApiResponse<Void> updateShare(
      HttpSession session,
      @PathVariable String sharename,
      @Valid @RequestBody SambaShareCreateDto dto) {
    String sessionId = session.getId();
    dto.setName(sharename); // РџСЂРёРЅСѓРґРёС‚РµР»СЊРЅРѕ РёСЃРїРѕР»СЊР·СѓРµРј РёРјСЏ РёР· РїСѓС‚Рё
    shareService.updateShare(sessionId, sharename, dto);
    return ApiResponse.ok(
        "РќР°СЃС‚СЂРѕР№РєРё РїР°РїРєРё '" + sharename + "' РѕР±РЅРѕРІР»РµРЅС‹", null);
  }

  @DeleteMapping("/{sharename}")
  public ApiResponse<Void> deleteShare(HttpSession session, @PathVariable String sharename) {
    String sessionId = session.getId();
    shareService.deleteShare(sessionId, sharename);
    return ApiResponse.ok("РЁР°СЂР° '" + sharename + "' СѓРґР°Р»РµРЅР°", null);
  }
}
