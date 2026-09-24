package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    SambaGlobalConfigDto configDto = new SambaGlobalConfigDto();
    // Assuming no explicit setter was found, the default is instantiated. We can just test basic
    // return values.
    when(configService.getGlobalConfig(anyString())).thenReturn(configDto);

    mockMvc
        .perform(get("/api/config/global"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void testUpdateGlobalConfig_ShouldReturnSuccess() throws Exception {
    SambaGlobalConfigDto payload = new SambaGlobalConfigDto();

    mockMvc
        .perform(
            put("/api/config/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Глобальная конфигурация успешно обновлена"));

    verify(configService).updateGlobalConfig(anyString(), any(SambaGlobalConfigDto.class));
  }
}
