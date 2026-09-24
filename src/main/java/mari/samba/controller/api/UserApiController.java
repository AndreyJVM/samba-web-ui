package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Operations for managing Samba users and their passwords")
public class UserApiController {

  @Autowired private SambaUserService userService;

  @GetMapping
  @Operation(summary = "Get all users", description = "Retrieves the list of all Samba users.")
  public ApiResponse<List<SambaUser>> getAllUsers(HttpSession session) throws Exception {
    String sessionId = session.getId();
    List<SambaUser> users = userService.getAllUsers(sessionId);
    return ApiResponse.ok(users);
  }

  @PostMapping
  @Operation(summary = "Create user", description = "Creates a new OS user and adds them to Samba.")
  public ApiResponse<Void> createUser(
      HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) throws Exception {
    String sessionId = session.getId();
    userService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
    return ApiResponse.ok("Пользователь '" + dto.username() + "' успешно создан", null);
  }

  @DeleteMapping("/{username}")
  @Operation(
      summary = "Delete user",
      description = "Deletes a Samba user and their related OS account.")
  public ApiResponse<Void> deleteUser(
      HttpSession session,
      @Parameter(description = "Username to delete", example = "john_doe") @PathVariable
          String username)
      throws Exception {
    String sessionId = session.getId();
    userService.deleteUser(sessionId, username);
    return ApiResponse.ok("Пользователь '" + username + "' успешно удален", null);
  }

  @PutMapping("/{username}/password")
  @Operation(
      summary = "Change user password",
      description = "Changes the password for an existing Samba user.")
  public ApiResponse<Void> changePassword(
      HttpSession session,
      @Parameter(description = "Username to update", example = "john_doe") @PathVariable
          String username,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "JSON containing the new password")
          @RequestBody
          Map<String, String> body)
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
