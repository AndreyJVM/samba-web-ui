package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(WebRoutes.CONFIG)
public class ConfigController {

  @Autowired private SambaConfigService configService;

  @GetMapping
  public String showConfig(HttpSession session, Model model) throws Exception {
    String sessionId = session.getId();
    String content = configService.getSmbConfContent(sessionId);
    model.addAttribute("currentConfig", content);
    model.addAttribute("backups", configService.listBackups(sessionId));
    return "config/view";
  }

  @GetMapping(WebRoutes.CONFIG_GLOBAL_MAPPING)
  public String showGlobalConfig(HttpSession session, Model model) throws Exception {
    String sessionId = session.getId();
    SambaGlobalConfigDto globalConfig = configService.getGlobalConfig(sessionId);
    model.addAttribute("globalConfig", globalConfig);
    return "config/global";
  }

  @PostMapping(WebRoutes.CONFIG_GLOBAL_MAPPING)
  public String updateGlobalConfig(
      HttpSession session, @ModelAttribute("globalConfig") SambaGlobalConfigDto globalConfig)
      throws Exception {
    String sessionId = session.getId();
    configService.updateGlobalConfig(sessionId, globalConfig);
    return "redirect:" + WebRoutes.CONFIG + WebRoutes.CONFIG_GLOBAL_MAPPING + "?saved";
  }

  @PostMapping(WebRoutes.CONFIG_RESTORE_MAPPING)
  public String restoreConfig(HttpSession session, @RequestParam String filename) throws Exception {
    String sessionId = session.getId();
    configService.restoreBackup(sessionId, filename);
    return "redirect:" + WebRoutes.CONFIG + "?restored";
  }
}
