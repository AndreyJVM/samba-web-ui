package mari.samba.controller.api;

import jakarta.validation.Valid;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.SambaGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

    @Autowired
    private SambaGroupService groupService;

    @GetMapping
    public ApiResponse<List<SambaGroup>> getAllGroups(@RequestAttribute("sessionId") String sessionId) throws Exception {
        return ApiResponse.ok(groupService.getAllGroups(sessionId));
    }

    @PostMapping
    public ApiResponse<Void> createGroup(@RequestAttribute("sessionId") String sessionId,
                                         @Valid @RequestBody SambaGroupCreateDto dto) throws Exception {
        groupService.createGroup(sessionId, dto);
        return ApiResponse.ok("Группа успешно создана", null);
    }

    @DeleteMapping("/{groupName}")
    public ApiResponse<Void> deleteGroup(@RequestAttribute("sessionId") String sessionId,
                                         @PathVariable String groupName) throws Exception {
        groupService.deleteGroup(sessionId, groupName);
        return ApiResponse.ok("Группа успешно удалена", null);
    }

    @PostMapping("/{groupName}/users")
    public ApiResponse<Void> addUserToGroup(@RequestAttribute("sessionId") String sessionId,
                                            @PathVariable String groupName,
                                            @RequestBody Map<String, String> body) throws Exception {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.error("Имя пользователя обязательно");
        }
        
        groupService.addUserToGroup(sessionId, username, groupName);
        return ApiResponse.ok("Пользователь добавлен в группу", null);
    }

    @DeleteMapping("/{groupName}/users/{username}")
    public ApiResponse<Void> removeUserFromGroup(@RequestAttribute("sessionId") String sessionId,
                                                 @PathVariable String groupName,
                                                 @PathVariable String username) throws Exception {
        groupService.removeUserFromGroup(sessionId, username, groupName);
        return ApiResponse.ok("Пользователь удален из группы", null);
    }
}
