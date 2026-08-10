package mari.samba.controller;

import mari.samba.dto.SshConnectionRequest;
import mari.samba.service.SshSessionManager;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private SshSessionManager sessionManager;

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

            // После успешного подключения перенаправляем на список шар
            return "redirect:/shares";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения: " + e.getMessage());
            model.addAttribute("connectionRequest", request);
            return "index";
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
}