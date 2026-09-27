package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.user.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

  @Autowired private SambaUserService userService;

  @GetMapping
  public ApiResponse<List<SambaUser>> getAllUsers(HttpSession session) {
    String sessionId = session.getId();
    List<SambaUser> users = userService.getAllUsers(sessionId);
    return ApiResponse.ok(users);
  }

  @PostMapping
  public ApiResponse<Void> createUser(
      HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) {
    String sessionId = session.getId();
    userService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
    return ApiResponse.ok(
        "РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ '" + dto.username() + "' СѓСЃРїРµС€РЅРѕ СЃРѕР·РґР°РЅ", null);
  }

  @DeleteMapping("/{username}")
  public ApiResponse<Void> deleteUser(HttpSession session, @PathVariable String username) {
    String sessionId = session.getId();
    userService.deleteUser(sessionId, username);
    return ApiResponse.ok(
        "РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ '" + username + "' СѓСЃРїРµС€РЅРѕ СѓРґР°Р»РµРЅ", null);
  }

  @PutMapping("/{username}/password")
  public ApiResponse<Void> changePassword(
      HttpSession session, @PathVariable String username, @RequestBody Map<String, String> body) {
    String sessionId = session.getId();
    String newPassword = body.get("newPassword");

    if (newPassword == null || newPassword.isBlank()) {
      return ApiResponse.error("РќРѕРІС‹Р№ РїР°СЂРѕР»СЊ РѕР±СЏР·Р°С‚РµР»РµРЅ");
    }

    userService.changePassword(sessionId, username, newPassword);
    return ApiResponse.ok(
        "РџР°СЂРѕР»СЊ РґР»СЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ '" + username + "' РѕР±РЅРѕРІР»РµРЅ", null);
  }
}
