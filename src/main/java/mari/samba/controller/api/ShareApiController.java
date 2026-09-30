package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing Samba shares.
 *
 * <p>Provides endpoints to retrieve, calculate size, create, update, and delete Samba shares over
 * SSH using the underlying service tier.
 */
@RestController
@RequestMapping("/api/shares")
public class ShareApiController {

  private final SambaShareService shareService;

  /**
   * Constructs a new {@link ShareApiController}.
   *
   * @param shareService the service containing core logic for share operations
   */
  public ShareApiController(SambaShareService shareService) {
    this.shareService = shareService;
  }

  /**
   * Retrieves all Samba shares available to the current user's session.
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping a list of {@link SambaShare} domain objects
   */
  @GetMapping
  public ApiResponse<List<SambaShare>> getAllShares(@NonNull HttpSession session) {
    List<SambaShare> shares = shareService.getAllShares(session.getId());
    return ApiResponse.ok(shares);
  }

  /**
   * Retrieves the details of a currently configured Samba share by its name.
   *
   * @param sharename the exact name of the share
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the configured {@link SambaShare}
   */
  @GetMapping("/{sharename}")
  public ApiResponse<SambaShare> getShare(
      @PathVariable String sharename, @NonNull HttpSession session) {
    SambaShare share = shareService.getShareByName(session.getId(), sharename);
    return ApiResponse.ok(share);
  }

  /**
   * Calculates and retrieves the current size of the specified share directory.
   *
   * @param sharename the exact name of the share
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the size represented as a readable string
   */
  @GetMapping("/{sharename}/size")
  public ApiResponse<String> getShareSize(
      @PathVariable String sharename, @NonNull HttpSession session) {
    String size = shareService.getShareSize(session.getId(), sharename);
    return ApiResponse.ok("Share size calculated successfully", size);
  }

  /**
   * Creates a net-new Samba share from a client request payload.
   *
   * @param session the user's current HTTP session
   * @param dto the shape/configuration for the new share
   * @return an {@link ApiResponse} indicating successful creation
   */
  @PostMapping
  public ApiResponse<Void> createShare(
      @NonNull HttpSession session, @Valid @RequestBody SambaShareCreateDto dto) {
    shareService.createShare(session.getId(), dto);
    return ApiResponse.ok("Share '" + dto.getName() + "' created successfully", null);
  }

  /**
   * Updates an existing Samba share using the specified payload. Note: The share is primarily
   * updated in the underlying file configuration.
   *
   * @param session the user's current HTTP session
   * @param sharename the original name of the share to update
   * @param dto the new shape/configuration for the share
   * @return an {@link ApiResponse} indicating successful modification
   */
  @PutMapping("/{sharename}")
  public ApiResponse<Void> updateShare(
      @NonNull HttpSession session,
      @PathVariable String sharename,
      @Valid @RequestBody SambaShareCreateDto dto) {
    shareService.updateShare(session.getId(), sharename, dto);
    return ApiResponse.ok("Share '" + sharename + "' updated successfully", null);
  }

  /**
   * Permanently deletes a Samba share from the configuration file. Does NOT natively delete root
   * directory contents unless explicitly forced by underlying scripts.
   *
   * @param session the user's current HTTP session
   * @param sharename the exact name of the share wrapper to remove
   * @return an {@link ApiResponse} indicating successful removal
   */
  @DeleteMapping("/{sharename}")
  public ApiResponse<Void> deleteShare(
      @NonNull HttpSession session, @PathVariable String sharename) {
    shareService.deleteShare(session.getId(), sharename);
    return ApiResponse.ok("Share '" + sharename + "' deleted successfully", null);
  }
}
