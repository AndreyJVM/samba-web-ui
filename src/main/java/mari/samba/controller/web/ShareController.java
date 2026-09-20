package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaMonitoringService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(WebRoutes.SHARES)
public class ShareController {

  @Autowired private SambaShareService shareService;

  @Autowired private SambaUserService userService;

  @Autowired private SambaGroupService groupService;

  @Autowired private SambaMonitoringService monitoringService;

  @GetMapping
  public String sharesDashboard(HttpSession session, Model model) throws Exception {
    String sessionId = session.getId();
    List<SambaShare> shares = shareService.getAllShares(sessionId);
    boolean isRunning = monitoringService.isServiceRunning(sessionId);
    model.addAttribute("shares", shares);
    model.addAttribute("isRunning", isRunning);
    return "shares/list";
  }

  @GetMapping(WebRoutes.SHARES_CREATE_MAPPING)
  public String showCreateForm(HttpSession session, Model model) throws Exception {
    String sessionId = session.getId();
    model.addAttribute("share", new SambaShareCreateDto());
    model.addAttribute("users", userService.getAllUsers(sessionId));
    model.addAttribute("groups", groupService.getAllGroups(sessionId));
    model.addAttribute("isEdit", false);
    return "shares/form";
  }

  @PostMapping(WebRoutes.SHARES_CREATE_MAPPING)
  public String createShare(
      HttpSession session,
      @ModelAttribute SambaShareCreateDto dto,
      RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    shareService.createShare(sessionId, dto);
    redirectAttributes.addFlashAttribute(
        "successMessage", "Общая папка '" + dto.getName() + "' создана!");
    return "redirect:" + WebRoutes.SHARES;
  }

  @GetMapping(WebRoutes.SHARES_EDIT_MAPPING)
  public String showEditForm(HttpSession session, @PathVariable String sharename, Model model)
      throws Exception {
    String sessionId = session.getId();
    SambaShare share = shareService.getShareByName(sessionId, sharename);
    model.addAttribute("share", share);
    model.addAttribute("users", userService.getAllUsers(sessionId));
    model.addAttribute("groups", groupService.getAllGroups(sessionId));
    model.addAttribute("isEdit", true);
    return "shares/form";
  }

  @PostMapping(WebRoutes.SHARES_EDIT_MAPPING)
  public String updateShare(
      HttpSession session,
      @PathVariable String sharename,
      @ModelAttribute SambaShareCreateDto dto,
      RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    dto.setName(sharename); // Принудительно сохраняем имя, так как оно в пути
    shareService.updateShare(sessionId, sharename, dto);
    redirectAttributes.addFlashAttribute(
        "successMessage", "Настройки папки '" + sharename + "' обновлены!");
    return "redirect:" + WebRoutes.SHARES;
  }

  @PostMapping(WebRoutes.SHARES_DELETE_MAPPING)
  public String deleteShare(
      HttpSession session, @RequestParam String sharename, RedirectAttributes redirectAttributes)
      throws Exception {
    String sessionId = session.getId();
    shareService.deleteShare(sessionId, sharename);
    redirectAttributes.addFlashAttribute("successMessage", "Шара '" + sharename + "' удалена.");
    return "redirect:" + WebRoutes.SHARES;
  }
}
