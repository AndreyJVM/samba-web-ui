package mari.samba.controller.api;

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
public class ConfigApiController {

  @Autowired private SambaConfigService configService;

  @GetMapping("/raw")
  public ApiResponse<String> getRawConfig(HttpSession session) throws Exception {
    String sessionId = session.getId();
    String content = configService.getSmbConfContent(sessionId);
    return ApiResponse.ok(content);
  }

  @GetMapping("/global")
  public ApiResponse<SambaGlobalConfigDto> getGlobalConfig(HttpSession session) throws Exception {
    String sessionId = session.getId();
    SambaGlobalConfigDto globalConfig = configService.getGlobalConfig(sessionId);
    return ApiResponse.ok(globalConfig);
  }

  @PutMapping("/global")
  public ApiResponse<Void> updateGlobalConfig(
      HttpSession session, @RequestBody SambaGlobalConfigDto globalConfig) throws Exception {
    String sessionId = session.getId();
    configService.updateGlobalConfig(sessionId, globalConfig);
    return ApiResponse.ok("Глобальная конфигурация успешно обновлена", null);
  }

  @GetMapping("/backups")
  public ApiResponse<List<SambaBackupDto>> listBackups(HttpSession session) throws Exception {
    String sessionId = session.getId();
    List<SambaBackupDto> backups = configService.listBackups(sessionId);
    return ApiResponse.ok(backups);
  }

  @PostMapping("/backups/{filename}/restore")
  public ApiResponse<Void> restoreBackup(HttpSession session, @PathVariable String filename)
      throws Exception {
    String sessionId = session.getId();
    configService.restoreBackup(sessionId, filename);
    return ApiResponse.ok("Конфигурация восстановлена из " + filename, null);
  }
}
