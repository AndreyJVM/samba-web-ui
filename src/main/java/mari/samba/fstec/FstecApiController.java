package mari.samba.fstec;

import jakarta.servlet.http.HttpSession;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for FSTEC Samba compliance analysis.
 *
 * <p>Provides read-only inspection endpoints to audit the effective Samba configuration against
 * FSTEC security guidelines.
 */
@RestController
@RequestMapping("/api/fstec")
public class FstecApiController {

    private final FstecComplianceService fstecService;

    /**
     * Constructs a new {@link FstecApiController}.
     *
     * @param fstecService the service responsible for auditing Samba configuration compliance
     */
    public FstecApiController(@NonNull FstecComplianceService fstecService) {
        this.fstecService = fstecService;
    }

    /**
     * Performs a read-only audit of the Samba server configuration and returns a compliance report.
     *
     * @param session the current HTTP session
     * @return an {@link ApiResponse} wrapping {@link FstecReportDto}
     */
    @GetMapping("/check")
    public ApiResponse<FstecReportDto> checkCompliance(@NonNull HttpSession session) {
        return ApiResponse.ok(fstecService.check(session.getId()));
    }
}
