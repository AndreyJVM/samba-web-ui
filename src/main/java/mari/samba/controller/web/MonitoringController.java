package mari.samba.controller.web;

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
            // Собираем все метрики
            boolean isRunning = monitoringService.isServiceRunning(sessionId);
            List<Map<String, String>> connections = monitoringService.getActiveConnections(sessionId);
            List<Map<String, String>> openFiles = monitoringService.getOpenFiles(sessionId);
            Map<String, String> diskUsage = monitoringService.getDiskUsage(sessionId);

            model.addAttribute("isRunning", isRunning);
            model.addAttribute("connections", connections);
            model.addAttribute("openFiles", openFiles);
            model.addAttribute("diskUsage", diskUsage);

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

    @PostMapping("/status/kill")
    public String killSession(@RequestParam String pid, HttpSession httpSession) {
        try {
            monitoringService.killSession(httpSession.getId(), pid);
            return "redirect:/status?killSuccess=true&pid=" + pid;
        } catch (Exception e) {
            return "redirect:/status?error=Ошибка завершения процесса: " + e.getMessage();
        }
    }
}