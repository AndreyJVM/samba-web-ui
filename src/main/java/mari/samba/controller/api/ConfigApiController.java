package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.config.SambaBackupDto;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Operations for managing smb.conf and backups")
public class ConfigApiController {

  @Autowired private SambaConfigService configService;

  @GetMapping("/raw")
  @Operation(
      summary = "Get raw smb.conf content",
      description = "Retrieves the full textual content of the smb.conf file from the server.")
  public ApiResponse<String> getRawConfig(HttpSession session) throws Exception {
    String sessionId = session.getId();
    String content = configService.getSmbConfContent(sessionId);
    return ApiResponse.ok(content);
  }

  @GetMapping("/global")
  @Operation(
      summary = "Get global configuration",
      description = "Retrieves the parsed parameters from the [global] section of smb.conf.")
  public ApiResponse<SambaGlobalConfigDto> getGlobalConfig(HttpSession session) throws Exception {
    String sessionId = session.getId();
    SambaGlobalConfigDto globalConfig = configService.getGlobalConfig(sessionId);
    return ApiResponse.ok(globalConfig);
  }

  @PutMapping("/global")
  @Operation(
      summary = "Update global configuration",
      description = "Updates the [global] section of smb.conf and restarts the Samba service.")
  public ApiResponse<Void> updateGlobalConfig(
      HttpSession session, @RequestBody SambaGlobalConfigDto globalConfig) throws Exception {
    String sessionId = session.getId();
    configService.updateGlobalConfig(sessionId, globalConfig);
    return ApiResponse.ok("Глобальная конфигурация успешно обновлена", null);
  }

  @GetMapping("/backups")
  @Operation(
      summary = "List backups",
      description = "Retrieves a list of available smb.conf backup files.")
  public ApiResponse<List<SambaBackupDto>> listBackups(HttpSession session) throws Exception {
    String sessionId = session.getId();
    List<SambaBackupDto> backups = configService.listBackups(sessionId);
    return ApiResponse.ok(backups);
  }

  @PostMapping("/backups/{filename}/restore")
  @Operation(
      summary = "Restore from backup",
      description =
          "Restores the smb.conf file from the specified backup and restarts the Samba service.")
  public ApiResponse<Void> restoreBackup(
      HttpSession session,
      @Parameter(
              description = "Filename of the backup to restore",
              example = "smb.conf.backup-2023-10-01")
          @PathVariable
          String filename)
      throws Exception {
    String sessionId = session.getId();
    configService.restoreBackup(sessionId, filename);
    return ApiResponse.ok("Конфигурация восстановлена из " + filename, null);
  }
}
