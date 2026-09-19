package mari.samba.controller.web;

import mari.samba.service.infra.CommandExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandExecutor commandExecutor;

    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void viewLogs_ShouldReturnViewTemplate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(commandExecutor.execute(eq(session.getId()), anyString())).thenReturn("log data test");

        mockMvc.perform(get("/logs").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("logs/view"))
                .andExpect(model().attributeExists("smbdLogs"));
    }
}
