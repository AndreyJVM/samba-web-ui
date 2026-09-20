package mari.samba.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import mari.samba.model.SambaGroup;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaGroupService;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SambaGroupService groupService;

  @MockitoBean private SambaUserService userService;

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void listGroups_ShouldReturnListTemplateAndRenderCsrfTokens() throws Exception {
    MockHttpSession session = new MockHttpSession();
    when(groupService.getAllGroups(session.getId()))
        .thenReturn(
            Collections.singletonList(new SambaGroup("test_group", Collections.emptyList())));

    mockMvc
        .perform(get("/groups").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("groups/list"))
        .andExpect(model().attributeExists("groups"))
        // Ensure create and delete forms have proper actions and CSRF appended
        .andExpect(content().string(containsString("action=\"/groups/create\"")))
        .andExpect(content().string(containsString("action=\"/groups/delete\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void editMembers_ShouldReturnTemplateWithGroupAndUsersAndRenderCsrfToken() throws Exception {
    MockHttpSession session = new MockHttpSession();
    when(groupService.getAllGroups(session.getId()))
        .thenReturn(
            Collections.singletonList(new SambaGroup("target_group", Collections.emptyList())));
    when(userService.getAllUsers(session.getId()))
        .thenReturn(Collections.singletonList(new SambaUser("user1")));

    mockMvc
        .perform(get("/groups/target_group/members").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("groups/members"))
        .andExpect(model().attributeExists("group", "allUsers"))
        // Ensure the dynamic path bound Action works and CSRF is present
        .andExpect(content().string(containsString("action=\"/groups/target_group/members\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }
}
