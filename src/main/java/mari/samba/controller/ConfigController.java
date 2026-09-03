package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.SambaBackupDto;
import mari.samba.dto.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/global")
    public String showGlobalConfig(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        try {
            SambaGlobalConfigDto globalConfig = configService.getGlobalConfig(sessionId);
            model.addAttribute("globalConfig", globalConfig);
            return "config/global";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки глобальных настроек: " + e.getMessage());
            return "redirect:/config";
        }
    }

    @PostMapping("/global")
    public String updateGlobalConfig(@Valid @ModelAttribute("globalConfig") SambaGlobalConfigDto dto,
                                     BindingResult bindingResult,
                                     HttpSession httpSession,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            return "config/global";
        }

        String sessionId = httpSession.getId();
        try {
            configService.updateGlobalConfig(sessionId, dto);
            return "redirect:/config/global?saved=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка сохранения глобальных настроек: " + e.getMessage());
            return "config/global";
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