package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import mari.samba.dto.user.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(WebRoutes.USERS)
public class UserController {

  @Autowired private SambaUserService userService;

  @GetMapping
  public String listUsers(HttpSession session, Model model) throws Exception {
    String sessionId = session.getId();
    List<SambaUser> users = userService.getAllUsers(sessionId);
    model.addAttribute("users", users);
    return "users/list";
  }

  @GetMapping(WebRoutes.USERS_CREATE_MAPPING)
  public String showCreateUserForm(Model model) {
    model.addAttribute("user", new SambaUserCreateDto());
    return "users/create";
  }

  @PostMapping(WebRoutes.USERS_CREATE_MAPPING)
  public String createUser(
      HttpSession session,
      @ModelAttribute("user") SambaUserCreateDto userDto,
      RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    userService.createUser(
        sessionId, userDto.getUsername(), userDto.getPassword(), userDto.getFullName());
    redirectAttributes.addFlashAttribute(
        "successMessage", "Пользователь '" + userDto.getUsername() + "' успешно создан!");
    return "redirect:" + WebRoutes.USERS;
  }

  @GetMapping(WebRoutes.USERS_CHANGE_PASSWORD_MAPPING)
  public String showChangePasswordForm(@PathVariable String username, Model model) {
    model.addAttribute("username", username);
    return "users/change-password";
  }

  @PostMapping(WebRoutes.USERS_DELETE_MAPPING)
  public String deleteUser(
      HttpSession session, @RequestParam String username, RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    userService.deleteUser(sessionId, username);
    redirectAttributes.addFlashAttribute(
        "successMessage", "Пользователь '" + username + "' успешно удален!");
    return "redirect:" + WebRoutes.USERS;
  }

  @PostMapping(WebRoutes.USERS_PASSWORD_MAPPING)
  public String changePassword(
      HttpSession session,
      @RequestParam String username,
      @RequestParam String newPassword,
      RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    userService.changePassword(sessionId, username, newPassword);
    redirectAttributes.addFlashAttribute(
        "successMessage", "Пароль для пользователя '" + username + "' обновлен!");
    return "redirect:" + WebRoutes.USERS;
  }
}
