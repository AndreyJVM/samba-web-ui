package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
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

    @GetMapping
    public String sharesDashboard(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        List<SambaShare> shares = shareService.getAllShares(sessionId);
        model.addAttribute("shares", shares);
        return "shares/dashboard";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        model.addAttribute("share", new SambaShareCreateDto());
        model.addAttribute("users", userService.getAllUsers(sessionId));
        model.addAttribute("groups", groupService.getAllGroups(sessionId));
        return "shares/form";
    }

    @PostMapping("/create")
    public String createShare(@RequestAttribute("sessionId") String sessionId,
                              @ModelAttribute("share") SambaShareCreateDto dto,
                              RedirectAttributes redirectAttributes) throws Exception {
        shareService.createShare(sessionId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + dto.getName() + "' успешно создана!");
        return "redirect:/shares";
    }

    @GetMapping("/edit/{name}")
    public String showEditForm(@RequestAttribute("sessionId") String sessionId,
                               @PathVariable String name,
                               Model model) throws Exception {
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
    public String updateShare(@RequestAttribute("sessionId") String sessionId,
                              @PathVariable String name,
                              @ModelAttribute("share") SambaShareCreateDto dto,
                              RedirectAttributes redirectAttributes) throws Exception {
        shareService.updateShare(sessionId, name, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + name + "' успешно обновлена!");
        return "redirect:/shares";
    }

    @PostMapping("/delete/{name}")
    public String deleteShare(@RequestAttribute("sessionId") String sessionId,
                              @PathVariable String name,
                              RedirectAttributes redirectAttributes) throws Exception {
        shareService.deleteShare(sessionId, name);
        redirectAttributes.addFlashAttribute("successMessage", "Шара '" + name + "' успешно удалена!");
        return "redirect:/shares";
    }
}