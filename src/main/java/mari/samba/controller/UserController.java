package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private SambaUserService userService;

    @GetMapping
    public String listUsers(HttpSession httpSession, Model model) {
        try {
            List<SambaUser> users = userService.getAllUsers(httpSession.getId());
            model.addAttribute("users", users);
            return "users/list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения списка пользователей: " + e.getMessage());
            return "users/list";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new SambaUserCreateDto());
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("user") SambaUserCreateDto dto,
                             BindingResult bindingResult,
                             HttpSession httpSession,
                             Model model) {
        if (bindingResult.hasErrors()) {
            return "users/create";
        }

        String sessionId = httpSession.getId();
        try {
            if (userService.userExists(sessionId, dto.getUsername())) {
                model.addAttribute("error", "Пользователь '" + dto.getUsername() + "' уже существует в системе");
                return "users/create";
            }

            userService.createUser(sessionId, dto.getUsername(), dto.getPassword(), dto.getFullName());
            return "redirect:/users?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания пользователя: " + e.getMessage());
            return "users/create";
        }
    }

    @PostMapping("/delete/{username}")
    public String deleteUser(@PathVariable String username, HttpSession httpSession) {
        try {
            userService.deleteUser(httpSession.getId(), username);
            return "redirect:/users?deleted=true";
        } catch (Exception e) {
            return "redirect:/users?error=" + e.getMessage();
        }
    }

    @GetMapping("/change-password/{username}")
    public String showChangePasswordForm(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "users/change-password";
    }

    @PostMapping("/change-password/{username}")
    public String changePassword(@PathVariable String username,
                                 @RequestParam String newPassword,
                                 HttpSession httpSession,
                                 Model model) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            model.addAttribute("error", "Пароль должен быть не менее 6 символов");
            model.addAttribute("username", username);
            return "users/change-password";
        }

        try {
            userService.changePassword(httpSession.getId(), username, newPassword.trim());
            return "redirect:/users?passwordChanged=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка смены пароля: " + e.getMessage());
            model.addAttribute("username", username);
            return "users/change-password";
        }
    }
}