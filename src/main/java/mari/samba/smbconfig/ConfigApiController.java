package mari.samba.smbconfig;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing global Samba configurations and backups (smb.conf).
 *
 * <p>Provides endpoints to retrieve and update the global section over UI models, retrieve and push
 * raw file contents, and manage automatic/manual backups.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigApiController {

  private final SambaConfigService configService;

  /**
   * Constructs a new {@link ConfigApiController}.
   *
   * @param configService the underlying service managing configuration operations
   */
  public ConfigApiController(SambaConfigService configService) {
    this.configService = configService;
  }

  /**
   * Retrieves the global Samba configuration parsed into a structured DTO.
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the parsed {@link SambaGlobalConfigDto}
   */
  @GetMapping("/global")
  public ApiResponse<SambaGlobalConfigDto> getGlobalConfig(@NonNull HttpSession session) {
    String sessionId = session.getId();
    SambaGlobalConfigDto config = configService.getGlobalConfig(sessionId);
    return ApiResponse.ok(config);
  }

  /**
   * Updates the global Samba configuration from a structured DTO.
   *
   * @param session the user's current HTTP session
   * @param dto the configuration parameters (workgroup, security, etc.)
   * @return an {@link ApiResponse} indicating successful application
   */
  @PutMapping("/global")
  public ApiResponse<Void> updateGlobalConfig(
      @NonNull HttpSession session, @Valid @RequestBody SambaGlobalConfigDto dto) {
    String sessionId = session.getId();
    configService.updateGlobalConfig(sessionId, dto);
    return ApiResponse.ok("Global configuration updated successfully", null);
  }

  /**
   * Retrieves the entire raw content of the currently active smb.conf file.
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the raw file text
   */
  @GetMapping("/raw")
  public ApiResponse<String> getRawConfig(@NonNull HttpSession session) {
    String sessionId = session.getId();
    String content = configService.getSmbConfContent(sessionId);
    return ApiResponse.ok("Configuration retrieved successfully", content);
  }

  /**
   * Completely replaces the raw smb.conf file with the provided content.
   *
   * @param session the user's current HTTP session
   * @param dto the request body containing the raw multiline text
   * @return an {@link ApiResponse} indicating successful file update and service restart
   */
  @PutMapping("/raw")
  public ApiResponse<Void> updateRawConfig(
      @NonNull HttpSession session, @Valid @RequestBody SambaRawConfigDto dto) {
    String sessionId = session.getId();
    configService.updateSmbConf(sessionId, dto.content());
    return ApiResponse.ok("Configuration file updated and service restarted", null);
  }

  /**
   * Lists all available historical backups of the smb.conf file.
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} wrapping the collection of {@link SambaBackupDto}
   */
  @GetMapping("/backups")
  public ApiResponse<List<SambaBackupDto>> listBackups(@NonNull HttpSession session) {
    String sessionId = session.getId();
    List<SambaBackupDto> backups = configService.listBackups(sessionId);
    return ApiResponse.ok(backups);
  }

  /**
   * Manually creates a new backup of the current smb.conf file to the backup directory.
   *
   * @param session the user's current HTTP session
   * @return an {@link ApiResponse} indicating success
   */
  @PostMapping("/backups")
  public ApiResponse<Void> createBackup(@NonNull HttpSession session) {
    String sessionId = session.getId();
    configService.createBackup(sessionId);
    return ApiResponse.ok("Backup created successfully", null);
  }

  /**
   * Restores smb.conf from a previous backup file identified by its path variable.
   *
   * @param session the user's current HTTP session
   * @param filename the exact name of the backup file to restore
   * @return an {@link ApiResponse} indicating successful restore
   */
  @PostMapping("/backups/{filename}/restore")
  public ApiResponse<Void> restoreBackupByPath(
      @NonNull HttpSession session, @PathVariable("filename") String filename) {
    String sessionId = session.getId();
    configService.restoreBackup(sessionId, filename);
    return ApiResponse.ok("Configuration restored from " + filename, null);
  }

  /**
   * Restores smb.conf from a previous backup file identified by a request parameter.
   *
   * @param session the user's current HTTP session
   * @param filename the exact name of the backup file to restore via parameter
   * @return an {@link ApiResponse} indicating successful restore
   */
  @PostMapping("/backups/restore")
  public ApiResponse<Void> restoreBackupByParam(
      @NonNull HttpSession session, @RequestParam("filename") String filename) {
    return restoreBackupByPath(session, filename);
  }
}
