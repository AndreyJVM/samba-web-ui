package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.config.SambaBackupDto;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private SambaConfigService configService;

    @GetMapping
    public String viewConfig(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        String content = configService.getSmbConfContent(sessionId);
        List<SambaBackupDto> backups = configService.listBackups(sessionId);
        
        model.addAttribute("currentConfig", content); // Исправлено: шаблон ожидает currentConfig
        model.addAttribute("backups", backups); // Исправлено: добавляем список бэкапов для шаблона
        return "config/view";
    }

    @PostMapping("/save")
    public String saveConfig(@RequestAttribute("sessionId") String sessionId,
                             @RequestParam String content,
                             RedirectAttributes redirectAttributes) throws Exception {
        configService.createBackup(sessionId);
        configService.updateSmbConf(sessionId, content);
        redirectAttributes.addFlashAttribute("successMessage", "Конфигурация успешно сохранена! (Бэкап создан)");
        return "redirect:/config";
    }

    @GetMapping("/global")
    public String viewGlobalSettings(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        SambaGlobalConfigDto config = configService.getGlobalConfig(sessionId);
        model.addAttribute("globalConfig", config);
        return "config/global";
    }

    @PostMapping("/global/save")
    public String saveGlobalSettings(@RequestAttribute("sessionId") String sessionId,
                                     SambaGlobalConfigDto dto,
                                     RedirectAttributes redirectAttributes) throws Exception {
        configService.createBackup(sessionId);
        configService.updateGlobalConfig(sessionId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Глобальные настройки успешно сохранены!");
        return "redirect:/config/global";
    }
}