package mari.samba.controller.web;

import mari.samba.service.infra.SshSessionManager;
import mari.samba.dto.auth.ConnectionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security constraints for AuthController explicitly to simulate unauthenticated access tests easily
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SshSessionManager sessionManager;

    @Test
    void index_WhenNotAuthenticated_ShouldReturnIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void index_WhenAuthenticated_ShouldRedirectToShares() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shares"));
    }

    @Test
    void connect_WithValidCredentials_ShouldRedirectToShares() throws Exception {
        doNothing().when(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));

        mockMvc.perform(post("/connect")
                        .with(csrf())
                        .param("host", "127.0.0.1")
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shares"));
    }

    @Test
    void connect_WithInvalidCredentials_ShouldReturnIndexWithError() throws Exception {
        doThrow(new RuntimeException("Auth failed")).when(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));

        mockMvc.perform(post("/connect")
                        .with(csrf())
                        .param("host", "127.0.0.1")
                        .param("username", "baduser")
                        .param("password", "badpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void disconnect_ShouldInvalidateSessionAndRedirect() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("sshSessionId", "dummy-session-123");
        doNothing().when(sessionManager).disconnect(anyString());

        mockMvc.perform(get("/disconnect").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?disconnected"));
    }
}
