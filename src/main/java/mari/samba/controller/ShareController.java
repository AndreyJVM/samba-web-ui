package mari.samba.controller;

import com.jcraft.jsch.Session;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import mari.samba.service.SshSessionManager;
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
    private SshSessionManager sessionManager;

    @Autowired
    private SambaShareService shareService;

    /**
     * Список всех шар
     */
    @GetMapping
    public String listShares(HttpSession httpSession, Model model) {
        try {
            Session session = sessionManager.getSession(httpSession.getId());
            List<SambaShare> shares = shareService.getAllShares(session);

            model.addAttribute("shares", shares);
            model.addAttribute("sambaHost", httpSession.getAttribute("sambaHost"));
            return "shares/list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка получения списка шар: " + e.getMessage());
            return "shares/list";
        }
    }

    /**
     * Страница создания новой шары
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("share", new SambaShareCreateDto());
        return "shares/create";
    }

    /**
     * Создание новой шары
     */
    @PostMapping("/create")
    public String createShare(@Valid @ModelAttribute("share") SambaShareCreateDto dto,
                              BindingResult bindingResult,
                              HttpSession httpSession,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "shares/create";
        }

        try {
            Session session = sessionManager.getSession(httpSession.getId());
            shareService.createShare(session, dto);
            return "redirect:/shares?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания шары: " + e.getMessage());
            return "shares/create";
        }
    }

    /**
     * Страница редактирования шары
     */
    @GetMapping("/edit/{name}")
    public String showEditForm(@PathVariable String name,
                               HttpSession httpSession,
                               Model model) {
        try {
            Session session = sessionManager.getSession(httpSession.getId());
            SambaShare share = shareService.getShareByName(session, name);

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

    /**
     * Обновление шары
     */
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
            Session session = sessionManager.getSession(httpSession.getId());
            shareService.updateShare(session, name, dto);
            return "redirect:/shares?updated=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка обновления шары: " + e.getMessage());
            model.addAttribute("originalName", name);
            return "shares/edit";
        }
    }

    /**
     * Удаление шары
     */
    @PostMapping("/delete/{name}")
    public String deleteShare(@PathVariable String name, HttpSession httpSession) {
        try {
            Session session = sessionManager.getSession(httpSession.getId());
            shareService.deleteShare(session, name);
            return "redirect:/shares?deleted=true";
        } catch (Exception e) {
            return "redirect:/shares?error=" + e.getMessage();
        }
    }
}