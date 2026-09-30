package mari.samba.group;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Samba platform groups via SSH.
 *
 * <p>Provides endpoints to retrieve all system groups applicable to Samba, create new group
 * entries, fully wipe groups, and manage cross-group user membership assignments.
 */
@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

  private final SambaGroupService sambaGroupService;

  /**
   * Constructs a new {@link GroupApiController}.
   *
   * @param sambaGroupService the underlying business logic service for group administration
   */
  public GroupApiController(SambaGroupService sambaGroupService) {
    this.sambaGroupService = sambaGroupService;
  }

  /**
   * Retrieves a list of all identified groups from the operating system with their associated
   * members.
   *
   * @param session the user's current HTTP session referencing the remote connection
   * @return an {@link ApiResponse} wrapping the collection of {@link SambaGroup} configurations
   */
  @GetMapping
  public ApiResponse<List<SambaGroup>> getAllGroups(@NonNull HttpSession session) {
    String sessionId = session.getId();
    List<SambaGroup> groups = sambaGroupService.getAllGroups(sessionId);
    return ApiResponse.ok(groups);
  }

  /**
   * Provisions a new group entry on the underlying remote host.
   *
   * @param session the user's current HTTP session
   * @param dto the encapsulated data needed to create the group (e.g. group name)
   * @return an {@link ApiResponse} reflecting successful execution
   */
  @PostMapping
  public ApiResponse<Void> createGroup(
      @NonNull HttpSession session, @Valid @RequestBody SambaGroupCreateDto dto) {
    String sessionId = session.getId();
    sambaGroupService.createGroup(sessionId, dto);
    return ApiResponse.ok("Group '" + dto.groupName() + "' created successfully", null);
  }

  /**
   * Safely deletes an existing system group if permitted.
   *
   * @param session the user's current HTTP session
   * @param groupName the textual identifier of the group meant for deletion
   * @return an {@link ApiResponse} reflecting successful execution
   */
  @DeleteMapping("/{groupName}")
  public ApiResponse<Void> deleteGroup(
      @NonNull HttpSession session, @PathVariable String groupName) {
    String sessionId = session.getId();
    sambaGroupService.deleteGroup(sessionId, groupName);
    return ApiResponse.ok("Group '" + groupName + "' deleted successfully", null);
  }

  /**
   * Appends an existing user to an existing system group.
   *
   * @param session the user's current HTTP session
   * @param groupName the intended target group name
   * @param body a dynamic map expecting a 'username' payload identifying who to add
   * @return an {@link ApiResponse} indicating successful group role assignment
   * @throws IllegalArgumentException if the 'username' key is missing or entirely blank
   */
  @PostMapping("/{groupName}/users")
  public ApiResponse<Void> addUserToGroup(
      @NonNull HttpSession session,
      @PathVariable String groupName,
      @RequestBody Map<String, String> body) {
    String sessionId = session.getId();
    String username = body != null ? body.get("username") : null;
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username must not be blank.");
    }
    sambaGroupService.addUserToGroup(sessionId, username, groupName);
    return ApiResponse.ok("User '" + username + "' added to group '" + groupName + "'", null);
  }

  /**
   * Detaches a specific user from a specific system group, withdrawing related rights.
   *
   * @param session the user's current HTTP session
   * @param groupName the group currently holding the membership
   * @param username the target individual to strip group presence from
   * @return an {@link ApiResponse} indicating successful unenrollment
   */
  @DeleteMapping("/{groupName}/users/{username}")
  public ApiResponse<Void> removeUserFromGroup(
      @NonNull HttpSession session, @PathVariable String groupName, @PathVariable String username) {
    String sessionId = session.getId();
    sambaGroupService.removeUserFromGroup(sessionId, username, groupName);
    return ApiResponse.ok("User '" + username + "' removed from group '" + groupName + "'", null);
  }
}
