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
  public ApiResponse<List<SambaUser>> getAllUsers(HttpSession session) throws Exception {
    String sessionId = session.getId();
    List<SambaUser> users = userService.getAllUsers(sessionId);
    return ApiResponse.ok(users);
  }

  @PostMapping
  public ApiResponse<Void> createUser(
      HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) throws Exception {
    String sessionId = session.getId();
    userService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
    return ApiResponse.ok("Пользователь '" + dto.username() + "' успешно создан", null);
  }

  @DeleteMapping("/{username}")
  public ApiResponse<Void> deleteUser(HttpSession session, @PathVariable String username)
      throws Exception {
    String sessionId = session.getId();
    userService.deleteUser(sessionId, username);
    return ApiResponse.ok("Пользователь '" + username + "' успешно удален", null);
  }

  @PutMapping("/{username}/password")
  public ApiResponse<Void> changePassword(
      HttpSession session, @PathVariable String username, @RequestBody Map<String, String> body)
      throws Exception {
    String sessionId = session.getId();
    String newPassword = body.get("newPassword");

    if (newPassword == null || newPassword.isBlank()) {
      return ApiResponse.error("Новый пароль обязателен");
    }

    userService.changePassword(sessionId, username, newPassword);
    return ApiResponse.ok("Пароль для пользователя '" + username + "' обновлен", null);
  }
}
