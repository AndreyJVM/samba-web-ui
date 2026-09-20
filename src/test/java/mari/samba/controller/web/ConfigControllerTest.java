package mari.samba.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfigController.class)
class ConfigControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SambaConfigService configService;

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void showConfig_ShouldReturnViewTemplate() throws Exception {
    MockHttpSession session = new MockHttpSession();
    when(configService.getSmbConfContent(session.getId()))
        .thenReturn("[global]\nworkgroup=WORKGROUP");
    when(configService.listBackups(session.getId())).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/config").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("config/view"))
        .andExpect(model().attributeExists("currentConfig", "backups"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void showGlobalConfig_ShouldReturnGlobalTemplate() throws Exception {
    MockHttpSession session = new MockHttpSession();
    when(configService.getGlobalConfig(session.getId())).thenReturn(new SambaGlobalConfigDto());

    mockMvc
        .perform(get("/config/global").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("config/global"))
        .andExpect(model().attributeExists("globalConfig"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void updateGlobalConfig_ShouldRedirectWithSavedParam() throws Exception {
    MockHttpSession session = new MockHttpSession();
    doNothing()
        .when(configService)
        .updateGlobalConfig(anyString(), any(SambaGlobalConfigDto.class));

    mockMvc
        .perform(
            post("/config/global").with(csrf()).session(session).param("workgroup", "NEWGROUP"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/config/global?saved"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void restoreConfig_ShouldRedirectWithRestoredParam() throws Exception {
    MockHttpSession session = new MockHttpSession();
    doNothing().when(configService).restoreBackup(anyString(), anyString());

    mockMvc
        .perform(
            post("/config/restore")
                .with(csrf())
                .session(session)
                .param("filename", "smb.conf.backup"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/config?restored"));
  }
}
