package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.SambaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        model.addAttribute("content", content);
        return "config/editor";
    }

    @PostMapping("/save")
    public String saveConfig(HttpSession session, @RequestParam String content) throws Exception {
        String sessionId = session.getId();
        configService.updateSmbConf(sessionId, content);
        return "redirect:/config?saved";
    }
}