package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.service.infra.SshSessionManager;
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

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private SshSessionManager sessionManager;
  @MockitoBean private mari.samba.service.BruteForceProtectionService bruteForceService;

  @Test
  void testConnect_Success() throws Exception {
    ConnectionRequestDto dto =
        new ConnectionRequestDto("192.168.1.100", 22, "root", "password", "Secret@123", null, null);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(sessionManager).createSession(anyString(), any(ConnectionRequestDto.class));
  }

  @Test
  void testConnect_MissingPasswordForPasswordAuth_ShouldReturnBadRequest() throws Exception {
    ConnectionRequestDto dto =
        new ConnectionRequestDto("192.168.1.100", 22, "root", "password", null, null, null);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Р СџР В°РЎР‚Р С•Р В»РЎРЉ Р С•Р В±РЎРЏР В·Р В°РЎвЂљР ВµР В»Р ВµР Р… Р Т‘Р В»РЎРЏ Р В°РЎС“РЎвЂљР ВµР Р…РЎвЂљР С‘РЎвЂћР С‘Р С”Р В°РЎвЂ Р С‘Р С‘ Р В±Р ВµР В· Р С”Р В»РЎР‹РЎвЂЎР В°"));

    verifyNoInteractions(sessionManager);
  }

  @Test
  void testGetCurrentUser() throws Exception {
    mockMvc
        .perform(
            get("/api/auth/me")
                .sessionAttr("sambaHost", "192.168.1.100")
                .sessionAttr("sambaUser", "root"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.host").value("192.168.1.100"))
        .andExpect(jsonPath("$.data.user").value("root"));
  }

  @Test
  void testLogout() throws Exception {
    mockMvc
        .perform(post("/api/auth/logout").sessionAttr("dummy", "dummy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").isString());

    verify(sessionManager).disconnect(anyString());
  }
}

