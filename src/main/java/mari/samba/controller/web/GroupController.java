package mari.samba.controller.web;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private SambaGroupService groupService;

    @Autowired
    private SambaUserService userService;

    @GetMapping
    public String listGroups(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        List<SambaGroup> groups = groupService.getAllGroups(sessionId);
        model.addAttribute("groups", groups);
        return "groups/list";
    }

    @PostMapping("/create")
    public String createGroup(HttpSession session, @RequestParam String name, RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        SambaGroupCreateDto dto = new SambaGroupCreateDto(name, "");
        groupService.createGroup(sessionId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Группа '" + name + "' успешно создана!");
        return "redirect:/groups";
    }

    @PostMapping("/delete")
    public String deleteGroup(HttpSession session, @RequestParam String name, RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        groupService.deleteGroup(sessionId, name);
        redirectAttributes.addFlashAttribute("successMessage", "Группа '" + name + "' успешно удалена!");
        return "redirect:/groups";
    }

    @GetMapping("/{groupname}/members")
    public String editMembers(HttpSession session, @PathVariable String groupname, Model model) throws Exception {
        String sessionId = session.getId();
        SambaGroup group = groupService.getAllGroups(sessionId).stream()
                .filter(g -> g.getName().equals(groupname))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена: " + groupname));

        model.addAttribute("group", group);
        model.addAttribute("allUsers", userService.getAllUsers(sessionId));
        return "groups/members";
    }

    @PostMapping("/{groupname}/members")
    public String updateMembers(HttpSession session,
                                @PathVariable String groupname,
                                @RequestParam(required = false) List<String> members,
                                RedirectAttributes redirectAttributes) throws Exception {
        String sessionId = session.getId();
        
        // Так как в сервисе нет пакетного setGroupMembers, мы сначала получаем старых
        // очищаем их, и добавляем новых, либо создадим отдельный метод в сервисе
        List<SambaGroup> groups = groupService.getAllGroups(sessionId);
        SambaGroup group = groups.stream().filter(g -> g.getName().equals(groupname)).findFirst().orElse(null);
        
        if (group != null) {
            // Удаляем старых
            for (String oldMember : group.getMembers()) {
                if (members == null || !members.contains(oldMember)) {
                    groupService.removeUserFromGroup(sessionId, oldMember, groupname);
                }
            }
            // Добавляем новых
            if (members != null) {
                for (String newMember : members) {
                    if (!group.getMembers().contains(newMember)) {
                        groupService.addUserToGroup(sessionId, newMember, groupname);
                    }
                }
            }
        }
        
        redirectAttributes.addFlashAttribute("successMessage", "Массив пользователей группы '" + groupname + "' обновлен!");
        return "redirect:/groups";
    }
}
