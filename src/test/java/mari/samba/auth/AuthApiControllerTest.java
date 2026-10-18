package mari.samba.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import mari.samba.config.ExecutionProperties.Mode;
import mari.samba.infra.ExecutionSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExecutionSessionManager sessionManager;

    @MockitoBean
    private mari.samba.auth.BruteForceProtectionService bruteForceService;

    @BeforeEach
    void setUpExecutionMode() {
        when(sessionManager.effectiveMode()).thenReturn(Mode.REMOTE);
        when(sessionManager.sessionMode(anyString())).thenReturn(Mode.REMOTE);
    }

    @Test
    void reportsRemoteEnvironment() throws Exception {
        mockMvc.perform(get("/api/auth/environment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("remote"));
    }

    @Test
    void testConnect_Success() throws Exception {
        ConnectionRequestDto dto =
                new ConnectionRequestDto("192.168.1.100", 22, "root", "password", "Secret@123", null, null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));
    }

    @Test
    void testConnect_DockerHostname_Success() throws Exception {
        ConnectionRequestDto dto = new ConnectionRequestDto("samba-node", 22, "admin", "password", "admin", null, null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));
    }

    @Test
    void testConnect_LocalModeWithoutHost_Success() throws Exception {
        when(sessionManager.effectiveMode()).thenReturn(Mode.LOCAL);
        when(sessionManager.sessionMode(anyString())).thenReturn(Mode.LOCAL);
        ConnectionRequestDto dto = new ConnectionRequestDto(null, 22, "admin", "password", "secret", null, null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(request().sessionAttribute("sambaHost", "Local server"));

        verify(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));
    }

    @Test
    void testConnect_MissingPasswordForPasswordAuth_ShouldReturnBadRequest() throws Exception {
        ConnectionRequestDto dto = new ConnectionRequestDto("192.168.1.100", 22, "root", "password", null, null, null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(sessionManager);
    }

    @Test
    void testConnect_MissingPrivateKeyForKeyAuth_ShouldReturnBadRequest() throws Exception {
        ConnectionRequestDto dto = new ConnectionRequestDto("192.168.1.100", 22, "root", "key", null, null, null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Private key is required when using key authentication."));

        verifyNoInteractions(sessionManager);
    }

    @Test
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .sessionAttr("sambaHost", "192.168.1.100")
                        .sessionAttr("sambaUser", "root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.host").value("192.168.1.100"))
                .andExpect(jsonPath("$.data.user").value("root"));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout").sessionAttr("dummy", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isString());

        verify(sessionManager).disconnect(anyString());
    }
}
