package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private CommandExecutor commandExecutor;

    @GetMapping
    public String viewLogs(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        // Берем последние 100 строк логов сервиса
        String smbdLogs = commandExecutor.execute(sessionId, "journalctl -u smbd -n 100 --no-pager");
        model.addAttribute("smbdLogs", smbdLogs);
        return "logs/view";
    }
}