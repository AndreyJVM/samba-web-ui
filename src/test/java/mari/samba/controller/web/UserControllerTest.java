package mari.samba.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SambaUserService userService;

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void listUsers_ShouldReturnListTemplateAndRenderCsrfToken() throws Exception {
    MockHttpSession session = new MockHttpSession();
    when(userService.getAllUsers(session.getId()))
        .thenReturn(Collections.singletonList(new SambaUser("test_user")));

    mockMvc
        .perform(get("/users").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("users/list"))
        .andExpect(model().attributeExists("users"))
        // Security checks: ensure delete form uses the correct POST mapping and contains CSRF
        .andExpect(content().string(containsString("action=\"/users/delete\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void showCreateUserForm_ShouldReturnCreateTemplate() throws Exception {
    mockMvc
        .perform(get("/users/create"))
        .andExpect(status().isOk())
        .andExpect(view().name("users/create"))
        .andExpect(model().attributeExists("user"))
        // Security check: ensure CSRF is present
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void createUser_ShouldRedirectAndFlashSuccess() throws Exception {
    MockHttpSession session = new MockHttpSession();
    doNothing().when(userService).createUser(anyString(), anyString(), anyString(), anyString());

    mockMvc
        .perform(
            post("/users/create")
                .with(csrf())
                .session(session)
                .param("username", "newtest")
                .param("password", "strongpass")
                .param("fullName", "Test User"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/users"))
        .andExpect(flash().attributeExists("successMessage"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void showChangePasswordForm_ShouldReturnTemplateWithCorrectActionAndCsrf() throws Exception {
    mockMvc
        .perform(get("/users/change-password/test_user"))
        .andExpect(status().isOk())
        .andExpect(view().name("users/change-password"))
        .andExpect(model().attributeExists("username"))
        .andExpect(model().attribute("username", "test_user"))
        // Security checks: ensure the form posts strictly to /users/password as mapped
        .andExpect(content().string(containsString("action=\"/users/password\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void changePassword_ShouldRedirectAndFlashSuccess() throws Exception {
    MockHttpSession session = new MockHttpSession();
    doNothing().when(userService).changePassword(anyString(), anyString(), anyString());

    mockMvc
        .perform(
            post("/users/password")
                .with(csrf())
                .session(session)
                .param("username", "test_user")
                .param("newPassword", "new_strong_pass"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/users"))
        .andExpect(flash().attributeExists("successMessage"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void deleteUser_ShouldRedirectAndFlashSuccess() throws Exception {
    MockHttpSession session = new MockHttpSession();
    doNothing().when(userService).deleteUser(anyString(), anyString());

    mockMvc
        .perform(post("/users/delete").with(csrf()).session(session).param("username", "bad_user"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/users"))
        .andExpect(flash().attributeExists("successMessage"));
  }
}
