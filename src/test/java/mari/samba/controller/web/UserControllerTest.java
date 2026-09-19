package mari.samba.controller.web;

import mari.samba.model.SambaUser;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SambaUserService userService;

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void listUsers_ShouldReturnListTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SambaUser user = new SambaUser("test_user");
        user.setFullName("Test");
        user.setAccountEnabled(true);
        when(userService.getAllUsers(session.getId())).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/users").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"));
    }
}
