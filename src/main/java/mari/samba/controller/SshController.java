package mari.samba.controller;

import mari.samba.dto.SshConnectionRequest;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaConfigService;
import mari.samba.service.SshSessionManager;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class SshController {

    @Autowired
    private SshSessionManager sessionManager;

    @Autowired
    private SambaConfigService sambaConfigService;

    /**
     * Главная страница — форма подключения к Samba серверу
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("connectionRequest", new SshConnectionRequest());
        return "index";
    }

    /**
     * Подключение к Samba серверу через SSH
     */
    @PostMapping("/connect")
    public String connect(@Valid SshConnectionRequest request, HttpSession httpSession, Model model) {
        try {
            String sessionId = httpSession.getId();
            sessionManager.createSession(sessionId, request.getHost(), request.getUsername(), request.getPassword());

            httpSession.setAttribute("sambaHost", request.getHost());
            httpSession.setAttribute("sambaUser", request.getUsername());

            return "redirect:/shares";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения: " + e.getMessage());
            model.addAttribute("connectionRequest", request);
            return "index";
        }
    }

    /**
     * Список всех шар (после успешного подключения)
     */
    @GetMapping("/shares")
    public String listShares(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            var session = sessionManager.getSession(sessionId);
            String configContent = sambaConfigService.getSmbConfContent(session);
            List<SambaShare> shares = sambaConfigService.parseShares(configContent);

            model.addAttribute("shares", shares);
            model.addAttribute("sambaHost", httpSession.getAttribute("sambaHost"));
            return "shares";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения списка шар: " + e.getMessage());
            return "shares";
        }
    }

    /**
     * Разорвать SSH-соединение
     */
    @GetMapping("/disconnect")
    public String disconnect(HttpSession httpSession) {
        String sessionId = httpSession.getId();
        sessionManager.disconnect(sessionId);
        httpSession.invalidate();
        return "redirect:/";
    }
    @GetMapping("/login")
    public String login() {
        return "redirect:/";
    }
}