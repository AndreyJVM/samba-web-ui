package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.user.SambaUserCreateDto;
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
    public String listUsers(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        List<SambaUser> users = userService.getAllUsers(sessionId);
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new SambaUserCreateDto());
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(HttpSession session,
                             @ModelAttribute("user") SambaUserCreateDto userDto,
                             RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        userService.createUser(sessionId, userDto.getUsername(), userDto.getPassword(), userDto.getFullName());
        redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + userDto.getUsername() + "' успешно создан!");
        return "redirect:/users";
    }
    
    @GetMapping("/change-password/{username}")
    public String showChangePasswordForm(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "users/change-password";
    }

    @PostMapping("/delete")
    public String deleteUser(HttpSession session,
                             @RequestParam String username,
                             RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        userService.deleteUser(sessionId, username);
        redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + username + "' успешно удален!");
        return "redirect:/users";
    }

    @PostMapping("/password")
    public String changePassword(HttpSession session,
                                 @RequestParam String username,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        userService.changePassword(sessionId, username, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Пароль для пользователя '" + username + "' обновлен!");
        return "redirect:/users";
    }
}
