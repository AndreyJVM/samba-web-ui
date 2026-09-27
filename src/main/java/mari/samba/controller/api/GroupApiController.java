package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.SambaGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

  @Autowired private SambaGroupService groupService;

  @GetMapping
  public ApiResponse<List<SambaGroup>> getAllGroups(HttpSession session) {
    String sessionId = session.getId();
    return ApiResponse.ok(groupService.getAllGroups(sessionId));
  }

  @PostMapping
  public ApiResponse<Void> createGroup(
      HttpSession session, @Valid @RequestBody SambaGroupCreateDto dto) {
    String sessionId = session.getId();
    groupService.createGroup(sessionId, dto);
    return ApiResponse.ok("Р“СЂСѓРїРїР° СѓСЃРїРµС€РЅРѕ СЃРѕР·РґР°РЅР°", null);
  }

  @DeleteMapping("/{groupName}")
  public ApiResponse<Void> deleteGroup(HttpSession session, @PathVariable String groupName) {
    String sessionId = session.getId();
    groupService.deleteGroup(sessionId, groupName);
    return ApiResponse.ok("Р“СЂСѓРїРїР° СѓСЃРїРµС€РЅРѕ СѓРґР°Р»РµРЅР°", null);
  }

  @PostMapping("/{groupName}/users")
  public ApiResponse<Void> addUserToGroup(
      HttpSession session, @PathVariable String groupName, @RequestBody Map<String, String> body) {
    String sessionId = session.getId();
    String username = body.get("username");
    if (username == null || username.isBlank()) {
      return ApiResponse.error("РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ");
    }

    groupService.addUserToGroup(sessionId, username, groupName);
    return ApiResponse.ok("РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ РґРѕР±Р°РІР»РµРЅ РІ РіСЂСѓРїРїСѓ", null);
  }

  @DeleteMapping("/{groupName}/users/{username}")
  public ApiResponse<Void> removeUserFromGroup(
      HttpSession session, @PathVariable String groupName, @PathVariable String username) {
    String sessionId = session.getId();
    groupService.removeUserFromGroup(sessionId, username, groupName);
    return ApiResponse.ok("РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ СѓРґР°Р»РµРЅ РёР· РіСЂСѓРїРїС‹", null);
  }
}
