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
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private SambaConfigService configService;

    @GetMapping
    public String showConfig(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        String content = configService.getSmbConfContent(sessionId);
        model.addAttribute("currentConfig", content);
        model.addAttribute("backups", configService.listBackups(sessionId));
        return "config/view";
    }
    
    @GetMapping("/global")
    public String showGlobalConfig(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        SambaGlobalConfigDto globalConfig = configService.getGlobalConfig(sessionId);
        model.addAttribute("config", globalConfig);
        return "config/global";
    }

    @PostMapping("/global")
    public String updateGlobalConfig(HttpSession session, @ModelAttribute SambaGlobalConfigDto config) throws Exception {
        String sessionId = session.getId();
        configService.updateGlobalConfig(sessionId, config);
        return "redirect:/config/global?saved";
    }
    
    @PostMapping("/restore")
    public String restoreConfig(HttpSession session, @RequestParam String filename) throws Exception {
        String sessionId = session.getId();
        configService.restoreBackup(sessionId, filename);
        return "redirect:/config?restored";
    }
}
