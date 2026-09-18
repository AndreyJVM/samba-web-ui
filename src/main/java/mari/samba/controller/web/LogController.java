package mari.samba.controller.web;

import mari.samba.service.SambaLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

@Controller
public class LogController {

    @Autowired
    private SambaLogService logService;

    @GetMapping("/logs")
    public String viewLogs(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        String logs = logService.getRecentLogs(sessionId, 500);
        model.addAttribute("logsData", logs);
        return "logs";
    }
}