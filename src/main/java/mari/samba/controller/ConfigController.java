package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.SambaBackupDto;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private SambaConfigService configService;

    @GetMapping
    public String showConfig(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        try {
            String currentConfig = configService.getSmbConfContent(sessionId);
            List<SambaBackupDto> backups = configService.listBackups(sessionId);

            model.addAttribute("currentConfig", currentConfig);
            model.addAttribute("backups", backups);
            return "config/view";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки конфигурации: " + e.getMessage());
            return "config/view";
        }
    }

    @PostMapping("/restore")
    public String restoreBackup(@RequestParam String filename, HttpSession httpSession) {
        try {
            configService.restoreBackup(httpSession.getId(), filename);
            return "redirect:/config?restored=true";
        } catch (Exception e) {
            return "redirect:/config?error=" + e.getMessage();
        }
    }
}