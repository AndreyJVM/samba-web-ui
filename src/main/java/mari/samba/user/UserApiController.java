package mari.samba.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Samba system users via SSH.
 *
 * <p>Provides endpoints to retrieve all users, create new users, delete users, and update passwords
 * for existing Samba accounts.
 */
@RestController
@RequestMapping("/api/users")
public class UserApiController {

  private final SambaUserService sambaUserService;

  /**
   * Constructs a new {@link UserApiController}.
   *
   * @param sambaUserService the underlying user logic service layer
   */
  public UserApiController(SambaUserService sambaUserService) {
    this.sambaUserService = sambaUserService;
  }

  /**
   * Retrieves a list of all recognized users from the remote system (e.g., via pdbedit or
   * /etc/passwd).
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the list of {@link SambaUser} configuration objects
   */
  @GetMapping
  public ApiResponse<List<SambaUser>> getAllUsers(@NonNull HttpSession session) {
    String sessionId = session.getId();
    List<SambaUser> users = sambaUserService.getAllUsers(sessionId);
    return ApiResponse.ok(users);
  }

  /**
   * Creates a new Samba user on the remote server system and syncs the smbpasswd database.
   *
   * @param session the user's current HTTP session
   * @param dto the configuration container holding username, full name, and raw password
   * @return an {@link ApiResponse} indicating successful creation
   */
  @PostMapping
  public ApiResponse<Void> createUser(
      @NonNull HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) {
    String sessionId = session.getId();
    sambaUserService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
    return ApiResponse.ok("User '" + dto.username() + "' created successfully", null);
  }

  /**
   * Drops an existing Samba user and subsequently cleans up Unix user references if configured.
   *
   * @param session the user's current HTTP session
   * @param username the target username to be deleted
   * @return an {@link ApiResponse} indicating successful deletion
   */
  @DeleteMapping("/{username}")
  public ApiResponse<Void> deleteUser(@NonNull HttpSession session, @PathVariable String username) {
    String sessionId = session.getId();
    sambaUserService.deleteUser(sessionId, username);
    return ApiResponse.ok("User '" + username + "' deleted successfully", null);
  }

  /**
   * Resets/updates the login password for a specified Samba user string representation.
   *
   * @param session the user's current HTTP session
   * @param username the username on the system whose password is to be rotated
   * @param body a mapped arbitrary JSON body wrapping a 'newPassword' key
   * @return an {@link ApiResponse} indicating success
   * @throws IllegalArgumentException if the provided payload lacks a valid string mapped to
   *     'newPassword'
   */
  @RequestMapping(
      value = "/{username}/password",
      method = {RequestMethod.PUT, RequestMethod.POST})
  public ApiResponse<Void> changePassword(
      @NonNull HttpSession session,
      @PathVariable String username,
      @RequestBody Map<String, String> body) {
    String sessionId = session.getId();
    String newPassword = body != null ? body.get("newPassword") : null;
    if (newPassword == null || newPassword.isBlank()) {
      throw new IllegalArgumentException("New password must not be blank.");
    }
    sambaUserService.changePassword(sessionId, username, newPassword);
    return ApiResponse.ok("Password for user '" + username + "' updated successfully", null);
  }
}
