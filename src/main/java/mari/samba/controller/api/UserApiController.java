package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.user.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

  private final SambaUserService sambaUserService;

  public UserApiController(SambaUserService sambaUserService) {
    this.sambaUserService = sambaUserService;
  }

  @GetMapping
  public ApiResponse<List<SambaUser>> getAllUsers(HttpSession session) {
    String sessionId = session.getId();
    List<SambaUser> users = sambaUserService.getAllUsers(sessionId);
    return ApiResponse.ok(users);
  }

  @PostMapping
  public ApiResponse<Void> createUser(
      HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) {
    String sessionId = session.getId();
    sambaUserService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
    return ApiResponse.ok("User '" + dto.username() + "' created successfully", null);
  }

  @DeleteMapping("/{username}")
  public ApiResponse<Void> deleteUser(HttpSession session, @PathVariable String username) {
    String sessionId = session.getId();
    sambaUserService.deleteUser(sessionId, username);
    return ApiResponse.ok("User '" + username + "' deleted successfully", null);
  }

  @RequestMapping(
      value = "/{username}/password",
      method = {RequestMethod.PUT, RequestMethod.POST})
  public ApiResponse<Void> changePassword(
      HttpSession session, @PathVariable String username, @RequestBody Map<String, String> body) {
    String sessionId = session.getId();
    String newPassword = body != null ? body.get("newPassword") : null;
    if (newPassword == null || newPassword.isBlank()) {
      throw new IllegalArgumentException("New password must not be blank.");
    }
    sambaUserService.changePassword(sessionId, username, newPassword);
    return ApiResponse.ok("Password for user '" + username + "' updated successfully", null);
  }
}
