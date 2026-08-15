package mari.samba.controller;

import com.jcraft.jsch.Session;
import mari.samba.dto.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import mari.samba.service.SshSessionManager;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private SshSessionManager sessionManager;

    @Autowired
    private SambaUserService userService;

    /**
     * Список всех пользователей
     */
    @GetMapping
    public String listUsers(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            List<SambaUser> users = userService.getAllUsers(session);

            model.addAttribute("users", users);
            return "users/list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения списка пользователей: " + e.getMessage());
            return "users/list";
        }
    }

    /**
     * Страница создания пользователя
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new SambaUserCreateDto());
        return "users/create";
    }

    /**
     * Создание пользователя
     */
    @PostMapping("/create")
    public String createUser(@Valid SambaUserCreateDto dto,
                             HttpSession httpSession,
                             Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);

            // Проверяем, существует ли пользователь
            if (userService.userExists(session, dto.getUsername())) {
                model.addAttribute("error", "Пользователь " + dto.getUsername() + " уже существует");
                model.addAttribute("user", dto);
                return "users/create";
            }

            userService.createUser(session, dto.getUsername(), dto.getPassword(), dto.getFullName());
            return "redirect:/users?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания пользователя: " + e.getMessage());
            model.addAttribute("user", dto);
            return "users/create";
        }
    }

    /**
     * Удаление пользователя
     */
    @PostMapping("/delete/{username}")
    public String deleteUser(@PathVariable String username, HttpSession httpSession) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            userService.deleteUser(session, username);
            return "redirect:/users?deleted=true";
        } catch (Exception e) {
            return "redirect:/users?error=" + e.getMessage();
        }
    }

    /**
     * Страница смены пароля
     */
    @GetMapping("/change-password/{username}")
    public String showChangePasswordForm(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "users/change-password";
    }

    /**
     * Смена пароля
     */
    @PostMapping("/change-password/{username}")
    public String changePassword(@PathVariable String username,
                                 @RequestParam String newPassword,
                                 HttpSession httpSession,
                                 Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            userService.changePassword(session, username, newPassword);
            return "redirect:/users?password-changed=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка смены пароля: " + e.getMessage());
            model.addAttribute("username", username);
            return "users/change-password";
        }
    }
}