package mari.samba.user;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import mari.samba.config.SecurityConfig;
import mari.samba.core.SambaCommandException;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserApiController.class)
@Import({SecurityConfig.class, SambaUserService.class})
class SystemUserLookupIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CommandExecutor commands;

    @Test
    void anonymousLookupDoesNotExecuteSystemCommands() throws Exception {
        mvc.perform(get("/api/users/system")).andExpect(status().isUnauthorized());

        verifyNoInteractions(commands);
    }

    @Test
    @WithMockUser
    void authenticatedLookupReturnsOnlyAccountNamesForCurrentSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(commands.execute(session.getId(), LinuxCommands.listSystemUsers()))
                .thenReturn("alice:x:1000:1000:Alice:/home/alice:/bin/bash\n");

        mvc.perform(get("/api/users/system").session(session))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":[\"alice\"]}"));

        verify(commands).execute(session.getId(), "getent passwd");
        verifyNoMoreInteractions(commands);
    }

    @Test
    @WithMockUser
    void failedLookupReturnsErrorInsteadOfEmptySuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(commands.execute(session.getId(), LinuxCommands.listSystemUsers()))
                .thenThrow(new SambaCommandException("Account database unavailable"));

        mvc.perform(get("/api/users/system").session(session))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
