package mari.samba.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final SambaUserService sambaUserService;

    public UserApiController(SambaUserService sambaUserService) {
        this.sambaUserService = sambaUserService;
    }

    @GetMapping
    public ApiResponse<List<SambaUser>> getAllUsers(@NonNull HttpSession session) {
        String sessionId = session.getId();
        List<SambaUser> users = sambaUserService.getAllUsers(sessionId);
        return ApiResponse.ok(users);
    }

    @PostMapping
    public ApiResponse<Void> createUser(@NonNull HttpSession session, @Valid @RequestBody SambaUserCreateDto dto) {
        String sessionId = session.getId();
        sambaUserService.createUser(sessionId, dto.username(), dto.password(), dto.fullName());
        return ApiResponse.ok("User '" + dto.username() + "' created successfully", null);
    }

    @DeleteMapping("/{username}")
    public ApiResponse<Void> deleteUser(@NonNull HttpSession session, @PathVariable String username) {
        String sessionId = session.getId();
        sambaUserService.deleteUser(sessionId, username);
        return ApiResponse.ok("User '" + username + "' deleted successfully", null);
    }

    @RequestMapping(
            value = "/{username}/password",
            method = {RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<Void> changePassword(
            @NonNull HttpSession session,
            @PathVariable String username,
            @Valid @RequestBody SambaUserChangePasswordDto dto) {
        String sessionId = session.getId();
        sambaUserService.changePassword(sessionId, username, dto.newPassword());
        return ApiResponse.ok("Password for user '" + username + "' updated successfully", null);
    }
}
