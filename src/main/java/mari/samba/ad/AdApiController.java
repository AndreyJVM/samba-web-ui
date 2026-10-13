package mari.samba.ad;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Active Directory (AD) integration settings.
 *
 * <p>Provides endpoints to retrieve the current AD joined status, as well as actions to join or
 * leave a specified Active Directory domain.
 */
@RestController
@RequestMapping("/api/ad")
public class AdApiController {

    private final AdIntegrationService adService;

    /**
     * Constructs a new {@link AdApiController}.
     *
     * @param adService the service responsible for Active Directory operations
     */
    public AdApiController(@NonNull AdIntegrationService adService) {
        this.adService = adService;
    }

    /**
     * Retrieves the current Active Directory integration status.
     *
     * @param session the user's current HTTP session
     * @return an {@link ApiResponse} containing the {@link AdStatusDto} with AD status details
     */
    @GetMapping("/status")
    public ApiResponse<AdStatusDto> getStatus(@NonNull HttpSession session) {
        return ApiResponse.ok(adService.getStatus(session.getId()));
    }

    /**
     * Initiates the process of joining an Active Directory domain.
     *
     * @param session the user's current HTTP session
     * @param request the {@link AdJoinRequestDto} containing domain credentials and configuration
     * @return an {@link ApiResponse} containing the resulting {@link AdStatusDto}
     */
    @PostMapping("/join")
    public ApiResponse<AdStatusDto> joinDomain(
            @NonNull HttpSession session, @NonNull @Valid @RequestBody AdJoinRequestDto request) {
        AdStatusDto status = adService.joinDomain(session.getId(), request);
        return ApiResponse.ok("Successfully joined domain", status);
    }

    /**
     * Initiates the process of leaving the currently joined Active Directory domain.
     *
     * @param session the user's current HTTP session
     * @param request the {@link AdJoinRequestDto} containing administrator credentials for removal
     * @return an {@link ApiResponse} containing the resulting {@link AdStatusDto}
     */
    @PostMapping("/leave")
    public ApiResponse<AdStatusDto> leaveDomain(
            @NonNull HttpSession session, @NonNull @Valid @RequestBody AdJoinRequestDto request) {
        AdStatusDto status = adService.leaveDomain(session.getId(), request);
        return ApiResponse.ok("Successfully left domain", status);
    }
}
