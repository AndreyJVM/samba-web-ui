package mari.samba.controller.web;

import mari.samba.model.SambaGroup;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SambaGroupService groupService;

    @MockitoBean
    private SambaUserService userService;

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void listGroups_ShouldReturnListTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(groupService.getAllGroups(session.getId())).thenReturn(Collections.singletonList(new SambaGroup("test_group", Collections.emptyList())));

        mockMvc.perform(get("/groups").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("groups/list"))
                .andExpect(model().attributeExists("groups"));
    }
}
