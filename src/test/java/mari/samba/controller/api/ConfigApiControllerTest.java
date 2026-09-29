package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfigApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConfigApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private SambaConfigService configService;

  @Test
  void testGetGlobalConfig_ShouldReturnDto() throws Exception {
    SambaGlobalConfigDto configDto = SambaGlobalConfigDto.builder().build();
    when(configService.getGlobalConfig(anyString())).thenReturn(configDto);

    mockMvc
        .perform(get("/api/config/global"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void testUpdateGlobalConfig_ShouldReturnSuccess() throws Exception {
    SambaGlobalConfigDto payload = SambaGlobalConfigDto.builder().build();

    mockMvc
        .perform(
            put("/api/config/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(configService).updateGlobalConfig(anyString(), any(SambaGlobalConfigDto.class));
  }
}
