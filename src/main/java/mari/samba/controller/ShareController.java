package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaMonitoringService;
import mari.samba.service.SambaShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/shares")
public class ShareController {

    @Autowired
    private SambaShareService shareService;

    @Autowired
    private SambaMonitoringService monitoringService;

    @GetMapping
    public String listShares(HttpSession httpSession, Model model) {
        String sessionId = httpSession.getId();
        try {
            List<SambaShare> shares = shareService.getAllShares(sessionId);
            boolean isRunning = monitoringService.isServiceRunning(sessionId);

            model.addAttribute("shares", shares);
            model.addAttribute("isRunning", isRunning);
            model.addAttribute("sambaHost", httpSession.getAttribute("sambaHost"));
            return "shares/list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения списка шар: " + e.getMessage());
            return "shares/list";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("share", new SambaShareCreateDto());
        return "shares/create";
    }

    @PostMapping("/create")
    public String createShare(@Valid @ModelAttribute("share") SambaShareCreateDto dto,
                              BindingResult bindingResult,
                              HttpSession httpSession,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "shares/create";
        }

        try {
            shareService.createShare(httpSession.getId(), dto);
            return "redirect:/shares?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания шары: " + e.getMessage());
            return "shares/create";
        }
    }

    @GetMapping("/edit/{name}")
    public String showEditForm(@PathVariable String name, HttpSession httpSession, Model model) {
        try {
            SambaShare share = shareService.getShareByName(httpSession.getId(), name);

            SambaShareCreateDto dto = new SambaShareCreateDto();
            dto.setName(share.getName());
            dto.setPath(share.getPath());
            dto.setComment(share.getComment());
            dto.setReadOnly(share.isReadOnly());
            dto.setGuestOk(share.isGuestOk());
            dto.setBrowseable(share.isBrowseable());
            dto.setValidUsers(share.getValidUsers());
            dto.setWriteList(share.getWriteList());
            dto.setCreateMask(share.getCreateMask());
            dto.setDirectoryMask(share.getDirectoryMask());
            dto.setForceUser(share.getForceUser());
            dto.setForceGroup(share.getForceGroup());
            dto.setMaxConnections(share.getMaxConnections());
            dto.setHostsAllow(share.getHostsAllow());
            dto.setHostsDeny(share.getHostsDeny());

            model.addAttribute("share", dto);
            model.addAttribute("originalName", name);
            return "shares/edit";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки шары: " + e.getMessage());
            return "redirect:/shares";
        }
    }

    @PostMapping("/edit/{name}")
    public String updateShare(@PathVariable String name,
                              @Valid @ModelAttribute("share") SambaShareCreateDto dto,
                              BindingResult bindingResult,
                              HttpSession httpSession,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("originalName", name);
            return "shares/edit";
        }

        try {
            shareService.updateShare(httpSession.getId(), name, dto);
            return "redirect:/shares?updated=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка обновления шары: " + e.getMessage());
            model.addAttribute("originalName", name);
            return "shares/edit";
        }
    }

    @PostMapping("/delete/{name}")
    public String deleteShare(@PathVariable String name, HttpSession httpSession) {
        try {
            shareService.deleteShare(httpSession.getId(), name);
            return "redirect:/shares?deleted=true";
        } catch (Exception e) {
            return "redirect:/shares?error=" + e.getMessage();
        }
    }
}