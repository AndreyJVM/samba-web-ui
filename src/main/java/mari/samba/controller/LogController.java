package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Map;

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

    /**
     * AJAX-эндпоинт для автообновления логов без перезагрузки всей страницы
     */
    @GetMapping("/logs/raw")
    @ResponseBody
    public ResponseEntity<Map<String, String>> fetchRawLogs(@RequestParam(defaultValue = "100") int lines,
                                                            HttpSession httpSession) {
        String sessionId = httpSession.getId();
        try {
            String logs = logService.getRecentLogs(sessionId, lines);
            return ResponseEntity.ok(Collections.singletonMap("logs", logs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}