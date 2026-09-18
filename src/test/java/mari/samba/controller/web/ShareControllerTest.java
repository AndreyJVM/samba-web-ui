package mari.samba.controller.web;

import mari.samba.config.SshAuthInterceptor;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaShareService;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
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
    private SshAuthInterceptor authInterceptor;

    @Test
    void sharesDashboard_ShouldReturnCorrectTemplate() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(shareService.getAllShares("test-session")).thenReturn(Collections.singletonList(new SambaShare()));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/shares")
                        .session(session)
                        .requestAttr("sessionId", "test-session"))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/list"))
                .andExpect(model().attributeExists("shares"));
    }

    @Test
    void showCreateForm_ShouldReturnCorrectTemplateAndInjectUsers() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(userService.getAllUsers("test")).thenReturn(Collections.emptyList());
        when(groupService.getAllGroups("test")).thenReturn(Collections.emptyList());
        
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/shares/create")
                        .session(session)
                        .requestAttr("sessionId", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("shares/form")) // Будем использовать единый шаблон form.html
                .andExpect(model().attributeExists("share", "users", "groups"));
    }
}
