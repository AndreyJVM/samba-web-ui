package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LogController {

    @Autowired
    private SambaLogService logService;

    @GetMapping("/logs")
    public String showLogsPage(@RequestParam(defaultValue = "100") int lines,
                               HttpSession httpSession,
                               Model model) {
        String sessionId = httpSession.getId();
        try {
            String logs = logService.getRecentLogs(sessionId, lines);
            model.addAttribute("logs", logs);
            model.addAttribute("selectedLines", lines);
            return "logs/view";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки логов: " + e.getMessage());
            model.addAttribute("logs", "");
            model.addAttribute("selectedLines", lines);
            return "logs/view";
        }
    }
}