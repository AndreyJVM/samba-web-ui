package mari.samba.controller.web;

import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private SambaUserService userService;

    @GetMapping
    public String loadUsersDashboard(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        List<SambaUser> users = userService.getAllUsers(sessionId);
        model.addAttribute("users", users);
        return "users/dashboard";
    }

    @PostMapping("/create")
    public String createUser(@RequestAttribute("sessionId") String sessionId,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String fullName,
                             RedirectAttributes redirectAttributes) throws Exception {
        if (userService.userExists(sessionId, username)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь '" + username + "' уже существует в Linux.");
            return "redirect:/users";
        }
        
        userService.createUser(sessionId, username, password, fullName);
        redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + username + "' успешно создан!");
        return "redirect:/users";
    }

    @PostMapping("/delete")
    public String deleteUser(@RequestAttribute("sessionId") String sessionId,
                             @RequestParam String username,
                             RedirectAttributes redirectAttributes) throws Exception {
        if ("root".equals(username) || "samba-admin".equals(username)) {
            throw new IllegalArgumentException("Удаление служебных профилей запрещено.");
        }
        
        userService.deleteUser(sessionId, username);
        redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + username + "' успешно удален!");
        return "redirect:/users";
    }

    @PostMapping("/chpasswd")
    public String changePassword(@RequestAttribute("sessionId") String sessionId,
                                 @RequestParam String username,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) throws Exception {
        // Базовая валидация на уровне контроллера
        if (newPassword == null || newPassword.length() < 3) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 3 символа.");
        }
        
        userService.changePassword(sessionId, username, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Пароль для пользователя '" + username + "' обновлен.");
        return "redirect:/users";
    }
}