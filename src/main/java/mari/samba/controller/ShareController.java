package mari.samba.controller;

import com.jcraft.jsch.Session;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import mari.samba.service.SshSessionManager;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/shares")  // Все методы этого контроллера будут доступны по пути /shares/...
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
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
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
    public String createShare(@Valid SambaShareCreateDto dto,
                              HttpSession httpSession,
                              Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            shareService.createShare(session, dto);
            return "redirect:/shares?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания шары: " + e.getMessage());
            model.addAttribute("share", dto);
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
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            SambaShare share = shareService.getShareByName(session, name);

            // Конвертируем в DTO для формы
            SambaShareCreateDto dto = new SambaShareCreateDto();
            dto.setName(share.getName());
            dto.setPath(share.getPath());
            dto.setComment(share.getComment());
            dto.setReadOnly(share.isReadOnly());
            dto.setGuestOk(share.isGuestOk());
            dto.setValidUsers(share.getValidUsers());

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
                              @Valid SambaShareCreateDto dto,
                              HttpSession httpSession,
                              Model model) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            shareService.updateShare(session, name, dto);
            return "redirect:/shares?updated=true";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка обновления шары: " + e.getMessage());
            model.addAttribute("share", dto);
            return "shares/edit";
        }
    }

    /**
     * Удаление шары
     */
    @PostMapping("/delete/{name}")
    public String deleteShare(@PathVariable String name,
                              HttpSession httpSession) {
        String sessionId = httpSession.getId();
        if (!sessionManager.isConnected(sessionId)) {
            return "redirect:/?disconnected=true";
        }

        try {
            Session session = sessionManager.getSession(sessionId);
            shareService.deleteShare(session, name);
            return "redirect:/shares?deleted=true";
        } catch (Exception e) {
            // Логируем ошибку
            return "redirect:/shares?error=" + e.getMessage();
        }
    }
}