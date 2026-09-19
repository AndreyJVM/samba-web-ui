package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(WebRoutes.LOGS)
public class LogController {

    @Autowired
    private CommandExecutor commandExecutor;

    @GetMapping
    public String viewLogs(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        // Читаем последние 200 строк из лога smbd (путь может отличаться в разных ОС, обычно /var/log/samba/log.smbd)
        String logOutput = commandExecutor.execute(sessionId, "sudo tail -n 200 /var/log/samba/log.smbd || echo 'Лог файл не найден'");
        model.addAttribute("smbdLogs", logOutput);
        return "logs/view";
    }
}
