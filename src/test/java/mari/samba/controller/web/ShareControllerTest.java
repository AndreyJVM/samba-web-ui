package mari.samba.controller.web;

import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
import mari.samba.service.SambaMonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void sharesDashboard_ShouldReturnCorrectTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(shareService.getAllShares(session.getId())).thenReturn(Collections.singletonList(new SambaShare()));
        when(monitoringService.isServiceRunning(session.getId())).thenReturn(true);

        mockMvc.perform(get("/shares").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/list"))
                .andExpect(model().attributeExists("shares", "isRunning"));
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
                .andExpect(model().attributeExists("share", "users", "groups"));
    }
}
