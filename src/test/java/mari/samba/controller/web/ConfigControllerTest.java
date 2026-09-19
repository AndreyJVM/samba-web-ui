package mari.samba.controller.web;

import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.service.SambaConfigService;
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

@WebMvcTest(ConfigController.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SambaConfigService configService;

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void showConfig_ShouldReturnViewTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(configService.getSmbConfContent(session.getId())).thenReturn("[global]\nworkgroup=WORKGROUP");
        when(configService.listBackups(session.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/config").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("config/view"))
                .andExpect(model().attributeExists("currentConfig", "backups"));
    }

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void showGlobalConfig_ShouldReturnGlobalTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(configService.getGlobalConfig(session.getId())).thenReturn(new SambaGlobalConfigDto());

        mockMvc.perform(get("/config/global").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("config/global"))
                // Изменено: проверяем, что в моделе есть атрибут "globalConfig", а не "config"
                .andExpect(model().attributeExists("globalConfig"));
    }
}
