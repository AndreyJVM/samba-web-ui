package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.SambaMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/status")
public class MonitoringController {

    @Autowired
    private SambaMonitoringService monitoringService;

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        String sessionId = session.getId();
        boolean isRunning = monitoringService.isServiceRunning(sessionId);
        model.addAttribute("isRunning", isRunning);
        
        // Добавляем метрики диска
        model.addAttribute("diskUsage", monitoringService.getDiskUsage(sessionId));

        if (isRunning) {
            model.addAttribute("connections", monitoringService.getActiveConnections(sessionId));
            model.addAttribute("openFiles", monitoringService.getOpenFiles(sessionId));
        }

        return "status/dashboard";
    }

    @PostMapping("/control")
    public String controlService(HttpSession session, @RequestParam String action) throws Exception {
        String sessionId = session.getId();
        monitoringService.controlService(sessionId, action);
        return "redirect:/status";
    }
    
    @PostMapping("/kill")
    public String killSession(HttpSession session, @RequestParam String pid) throws Exception {
        String sessionId = session.getId();
        monitoringService.killSession(sessionId, pid);
        return "redirect:/status";
    }
}