package mari.samba.controller.web;

import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaMonitoringService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShareController.class)
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SambaShareService shareService;

    @MockitoBean
    private SambaUserService userService;

    @MockitoBean
    private SambaGroupService groupService;

    @MockitoBean
    private SambaMonitoringService monitoringService;

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void sharesDashboard_ShouldReturnListTemplateAndRenderFormsCorrectly() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(shareService.getAllShares(session.getId())).thenReturn(Collections.emptyList());
        when(monitoringService.isServiceRunning(session.getId())).thenReturn(true);

        mockMvc.perform(get("/shares").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/list"))
                .andExpect(model().attributeExists("shares", "isRunning"))
                // Ensure delete share form and restart form bind correctly with CSRF tokens
                .andExpect(content().string(containsString("action=\"/shares/delete\"")))
                .andExpect(content().string(containsString("action=\"/status/control\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void showCreateForm_ShouldReturnCorrectTemplateAndInjectUsers() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(userService.getAllUsers(session.getId())).thenReturn(Collections.emptyList());
        when(groupService.getAllGroups(session.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/shares/create").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/form"))
                .andExpect(model().attributeExists("share", "users", "groups", "isEdit"))
                .andExpect(model().attribute("isEdit", false))
                // Form action verifies it targets create endpoint and renders CSRF implicit mapping
                .andExpect(content().string(containsString("action=\"/shares/create\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void showEditForm_ShouldReturnTemplateWithShareData() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SambaShare share = new SambaShare();
        share.setName("OldShare");
        when(shareService.getShareByName(session.getId(), "OldShare")).thenReturn(share);

        mockMvc.perform(get("/shares/edit/OldShare").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/form"))
                .andExpect(model().attributeExists("share", "users", "groups", "isEdit"))
                .andExpect(model().attribute("isEdit", true))
                // Editing must hit the dynamic edit route
                .andExpect(content().string(containsString("action=\"/shares/edit/OldShare\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
