package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.SambaShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShareApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShareApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private SambaShareService shareService;

  @Test
  void testGetAllShares_ShouldReturnShares() throws Exception {
    SambaShare share = new SambaShare();
    share.setName("public");
    share.setPath("/srv/samba/public");

    when(shareService.getAllShares(anyString())).thenReturn(List.of(share));

    mockMvc
        .perform(get("/api/shares"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].name").value("public"))
        .andExpect(jsonPath("$.data[0].path").value("/srv/samba/public"));
  }

  @Test
  void testGetShareByName_ShouldReturnShare() throws Exception {
    SambaShare share = new SambaShare();
    share.setName("private");

    when(shareService.getShareByName(anyString(), eq("private"))).thenReturn(share);

    mockMvc
        .perform(get("/api/shares/private"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.name").value("private"));
  }

  @Test
  void testCreateShare_ShouldReturnSuccess() throws Exception {
    SambaShareCreateDto dto = new SambaShareCreateDto();
    dto.setName("new-share");
    dto.setPath("/srv/samba/new");
    dto.setComment("New Share");

    mockMvc
        .perform(
            post("/api/shares")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Общая папка 'new-share' создана"));

    verify(shareService).createShare(anyString(), any(SambaShareCreateDto.class));
  }

  @Test
  void testUpdateShare_ShouldReturnSuccess() throws Exception {
    SambaShareCreateDto dto = new SambaShareCreateDto();
    dto.setName("data-share");
    dto.setPath("/srv/samba/data");

    mockMvc
        .perform(
            put("/api/shares/data-share")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Настройки папки 'data-share' обновлены"));

    verify(shareService).updateShare(anyString(), eq("data-share"), any(SambaShareCreateDto.class));
  }
}
