package mari.samba.controller.web;

import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.service.infra.SshSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;
import java.util.List;

@Controller
public class AuthController {

    @Autowired
    private SshSessionManager sessionManager;

    @GetMapping(WebRoutes.ROOT)
    public String home(Model model) {
        // Если пользователь уже авторизован, можно редиректить в shares
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:" + WebRoutes.SHARES;
        }
        
        model.addAttribute("connectionRequest", new ConnectionRequestDto());
        return "index";
    }

    @PostMapping(WebRoutes.CONNECT)
    public String connect(@Valid @ModelAttribute("connectionRequest") ConnectionRequestDto request,
                          BindingResult bindingResult,
                          HttpServletRequest httpRequest,
                          Model model) {
        if (bindingResult.hasErrors()) {
            return "index";
        }

        if (!request.isKeyAuth() && (request.getPassword() == null || request.getPassword().isBlank())) {
            model.addAttribute("error", "Введите пароль для подключения");
            return "index";
        }

        try {
            // Чтобы избежать Session Fixation, Spring обычно меняет айди сессии при логине, 
            // но мы можем оставить стандартную сессию или позволить Security пересоздать её
            HttpSession httpSession = httpRequest.getSession();
            String sessionId = httpSession.getId();
            
            // 1. Создаем SSH-подключение
            sessionManager.createSession(sessionId, request);

            // 2. Инициализируем Spring Security Authenticate
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), null, authorities);
            
            // 3. Сохраняем в контекст
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 4. Обязательно сохраняем контекст в сессию, чтобы он не пропал при редиректе
            httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            // 5. Сохраняем визуальную метку
            httpSession.setAttribute("sambaHost", request.getHost() + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : ""));
            httpSession.setAttribute("sambaUser", request.getUsername());

            return "redirect:" + WebRoutes.SHARES;
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping(WebRoutes.DISCONNECT)
    public String disconnect(HttpSession httpSession) {
        // В SecurityConfig настроили, что /disconnect очищает контектст и сессию,
        // но нам также нужно разорвать SSH-физическое соединение.
        if (httpSession != null) {
            String sessionId = httpSession.getId();
            sessionManager.disconnect(sessionId);
            httpSession.invalidate();
        }
        return "redirect:" + WebRoutes.ROOT + "?disconnected";
    }
}
