package mari.samba.controller.web;

import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.service.infra.SshSessionManager;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
        model.addAttribute("connectionRequest", new ConnectionRequestDto());
        return "index";
    }

    /**
     * Подключение к Samba серверу через SSH
     */
    @PostMapping("/connect")
    public String connect(@Valid @ModelAttribute("connectionRequest") ConnectionRequestDto request,
                          BindingResult bindingResult,
                          HttpSession httpSession,
                          Model model) {
        // Проверка базовой валидации (хост, логин, порт)
        if (bindingResult.hasErrors()) {
            return "index";
        }

        // Проверка обязательности пароля или ключа
        if (!request.isKeyAuth() && (request.getPassword() == null || request.getPassword().isBlank())) {
            model.addAttribute("error", "Введите пароль для подключения");
            return "index";
        }

        try {
            String sessionId = httpSession.getId();
            sessionManager.createSession(sessionId, request);

            httpSession.setAttribute("sambaHost", request.getHost() + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : ""));
            httpSession.setAttribute("sambaUser", request.getUsername());

            return "redirect:/shares";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения: " + e.getMessage());
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