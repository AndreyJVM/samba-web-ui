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
  public ApiResponse<List<SambaGroup>> getAllGroups(HttpSession session) throws Exception {
    String sessionId = session.getId();
    return ApiResponse.ok(groupService.getAllGroups(sessionId));
  }

  @PostMapping
  public ApiResponse<Void> createGroup(
      HttpSession session, @Valid @RequestBody SambaGroupCreateDto dto) throws Exception {
    String sessionId = session.getId();
    groupService.createGroup(sessionId, dto);
    return ApiResponse.ok("Группа успешно создана", null);
  }

  @DeleteMapping("/{groupName}")
  public ApiResponse<Void> deleteGroup(HttpSession session, @PathVariable String groupName)
      throws Exception {
    String sessionId = session.getId();
    groupService.deleteGroup(sessionId, groupName);
    return ApiResponse.ok("Группа успешно удалена", null);
  }

  @PostMapping("/{groupName}/users")
  public ApiResponse<Void> addUserToGroup(
      HttpSession session, @PathVariable String groupName, @RequestBody Map<String, String> body)
      throws Exception {
    String sessionId = session.getId();
    String username = body.get("username");
    if (username == null || username.isBlank()) {
      return ApiResponse.error("Имя пользователя обязательно");
    }

    groupService.addUserToGroup(sessionId, username, groupName);
    return ApiResponse.ok("Пользователь добавлен в группу", null);
  }

  @DeleteMapping("/{groupName}/users/{username}")
  public ApiResponse<Void> removeUserFromGroup(
      HttpSession session, @PathVariable String groupName, @PathVariable String username)
      throws Exception {
    String sessionId = session.getId();
    groupService.removeUserFromGroup(sessionId, username, groupName);
    return ApiResponse.ok("Пользователь удален из группы", null);
  }
}
