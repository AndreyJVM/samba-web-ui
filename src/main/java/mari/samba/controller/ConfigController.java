package mari.samba.controller;

import com.jcraft.jsch.Session;
import jakarta.servlet.http.HttpSession;
import mari.samba.dto.SambaBackupDto;
import mari.samba.service.SambaConfigService;
import mari.samba.service.SshSessionManager;
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
    private SshSessionManager sessionManager;

    @Autowired
    private SambaConfigService configService;

    @GetMapping
    public String showConfig(HttpSession httpSession, Model model) {
        try {
            Session session = sessionManager.getSession(httpSession.getId());

            String currentConfig = configService.getSmbConfContent(session);
            List<SambaBackupDto> backups = configService.listBackups(session);

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
            Session session = sessionManager.getSession(httpSession.getId());
            configService.restoreBackup(session, filename);
            return "redirect:/config?restored=true";
        } catch (Exception e) {
            return "redirect:/config?error=" + e.getMessage();
        }
    }
}