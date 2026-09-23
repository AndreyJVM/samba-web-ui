package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Groups", description = "Operations for managing Linux/Samba user groups")
public class GroupApiController {

  @Autowired private SambaGroupService groupService;

  @GetMapping
  @Operation(
      summary = "Get all groups",
      description = "Retrieves a list of all OS groups configured for Samba access.")
  public ApiResponse<List<SambaGroup>> getAllGroups(
      HttpSession session) throws Exception {
    String sessionId = session.getId();
    return ApiResponse.ok(groupService.getAllGroups(sessionId));
  }

  @PostMapping
  @Operation(summary = "Create group", description = "Creates a new OS group.")
  public ApiResponse<Void> createGroup(
      HttpSession session,
      @Valid @RequestBody SambaGroupCreateDto dto)
      throws Exception {
    String sessionId = session.getId();
    groupService.createGroup(sessionId, dto);
    return ApiResponse.ok("Группа успешно создана", null);
  }

  @DeleteMapping("/{groupName}")
  @Operation(summary = "Delete group", description = "Deletes an existing OS group by name.")
  public ApiResponse<Void> deleteGroup(
      HttpSession session,
      @Parameter(description = "Name of the group to delete", example = "sambashare") @PathVariable
          String groupName)
      throws Exception {
    String sessionId = session.getId();
    groupService.deleteGroup(sessionId, groupName);
    return ApiResponse.ok("Группа успешно удалена", null);
  }

  @PostMapping("/{groupName}/users")
  @Operation(
      summary = "Add user to group",
      description = "Assigns an existing OS user to the specified group.")
  public ApiResponse<Void> addUserToGroup(
      HttpSession session,
      @Parameter(description = "Target group name", example = "sambashare") @PathVariable
          String groupName,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "JSON containing the username to add")
          @RequestBody
          Map<String, String> body)
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
  @Operation(
      summary = "Remove user from group",
      description = "Removes a user from the specified group.")
  public ApiResponse<Void> removeUserFromGroup(
      HttpSession session,
      @Parameter(description = "Target group name", example = "sambashare") @PathVariable
          String groupName,
      @Parameter(description = "User to remove", example = "john_doe") @PathVariable
          String username)
      throws Exception {
    String sessionId = session.getId();
    groupService.removeUserFromGroup(sessionId, username, groupName);
    return ApiResponse.ok("Пользователь удален из группы", null);
  }
}
