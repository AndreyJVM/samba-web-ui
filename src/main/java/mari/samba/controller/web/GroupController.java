package mari.samba.controller.web;

import mari.samba.model.SambaGroup;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private SambaGroupService groupService;

    @Autowired
    private SambaUserService userService;

    @GetMapping
    public String groupsPage(@RequestAttribute("sessionId") String sessionId, Model model) throws Exception {
        List<SambaGroup> groups = groupService.getAllGroups(sessionId);
        List<SambaUser> users = userService.getAllUsers(sessionId);
        
        // Передаем только список имен пользователей для удобства в селектах JS
        List<String> usernames = users.stream().map(SambaUser::getUsername).collect(Collectors.toList());

        model.addAttribute("groups", groups);
        model.addAttribute("allUsernames", usernames);
        
        return "groups";
    }
}
