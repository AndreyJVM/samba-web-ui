package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.SambaMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class MonitoringController {

    @Autowired
    private SambaMonitoringService monitoringService;

    @GetMapping("/status")
    public String statusDashboard(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        try {
            boolean isRunning = monitoringService.isServiceRunning(sessionId);
            List<Map<String, String>> connections = monitoringService.getActiveConnections(sessionId);

            model.addAttribute("isRunning", isRunning);
            model.addAttribute("connections", connections);
            return "status/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения статуса сервера: " + e.getMessage());
            return "status/dashboard";
        }
    }

    @PostMapping("/status/control")
    public String controlService(@RequestParam String action, HttpSession httpSession) {
        try {
            monitoringService.controlService(httpSession.getId(), action);
            return "redirect:/status?success=true";
        } catch (Exception e) {
            return "redirect:/status?error=" + e.getMessage();
        }
    }
}