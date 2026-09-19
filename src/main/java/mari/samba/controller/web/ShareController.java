package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
import mari.samba.service.SambaMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shares")
public class ShareController {

    @Autowired
    private SambaShareService shareService;

    @Autowired
    private SambaUserService userService;
    
    @Autowired
    private SambaGroupService groupService;

    @Autowired
    private SambaMonitoringService monitoringService;

    @GetMapping
    public String sharesDashboard(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        List<SambaShare> shares = shareService.getAllShares(sessionId);
        
        boolean isRunning = monitoringService.isServiceRunning(sessionId); 
        model.addAttribute("isRunning", isRunning);
        model.addAttribute("shares", shares);
        return "shares/list"; 
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        model.addAttribute("share", new SambaShareCreateDto());
        model.addAttribute("users", userService.getAllUsers(sessionId));
        model.addAttribute("groups", groupService.getAllGroups(sessionId));
        model.addAttribute("isEdit", false);
        return "shares/form"; 
    }

    @PostMapping("/create")
    public String createShare(HttpSession session,
                              @ModelAttribute("share") SambaShareCreateDto dto,
                              RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        shareService.createShare(sessionId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + dto.getName() + "' успешно создана!");
        return "redirect:/shares";
    }

    @GetMapping("/edit/{name}")
    public String showEditForm(HttpSession session,
                               @PathVariable String name,
                               Model model) throws Exception {
        String sessionId = session.getId();
        SambaShare share = shareService.getShareByName(sessionId, name);
        if (share == null) {
            throw new IllegalArgumentException("Шара с именем '" + name + "' не найдена.");
        }

        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName(share.getName());
        dto.setPath(share.getPath());
        dto.setComment(share.getComment());
        dto.setBrowseable(share.isBrowseable());
        dto.setReadOnly(share.isReadOnly());
        dto.setValidUsers(share.getValidUsers());
        dto.setForceUser(share.getForceUser());

        model.addAttribute("share", dto);
        model.addAttribute("users", userService.getAllUsers(sessionId));
        model.addAttribute("groups", groupService.getAllGroups(sessionId));
        model.addAttribute("isEdit", true);

        return "shares/form"; 
    }

    @PostMapping("/edit/{name}")
    public String updateShare(HttpSession session,
                              @PathVariable String name,
                              @ModelAttribute("share") SambaShareCreateDto dto,
                              RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        shareService.updateShare(sessionId, name, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + name + "' успешно обновлена!");
        return "redirect:/shares";
    }

    @PostMapping("/delete/{name}")
    public String deleteShare(HttpSession session,
                              @PathVariable String name,
                              RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        shareService.deleteShare(sessionId, name);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + name + "' успешно удалена!");
        return "redirect:/shares";
    }
}