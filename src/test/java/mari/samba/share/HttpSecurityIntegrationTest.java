package mari.samba.share;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicReference;
import mari.samba.auth.AuthApiController;
import mari.samba.auth.AuthServiceImpl;
import mari.samba.auth.BruteForceProtectionService;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.config.ExecutionProperties.Mode;
import mari.samba.config.SecurityConfig;
import mari.samba.core.SambaCommandException;
import mari.samba.infra.ExecutionSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@WebMvcTest({AuthApiController.class, ShareApiController.class})
@Import({SecurityConfig.class, AuthServiceImpl.class})
class HttpSecurityIntegrationTest {
    private static final String LOGIN =
            """
            {"host":"samba-node","port":22,"username":"qa-user","authType":"password","password":"test-only"}
            """;
    private static final String SHARE =
            """
            {"name":"qa-share","path":"/srv/samba/qa-share"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExecutionSessionManager sessions;

    @MockitoBean
    private BruteForceProtectionService bruteForce;

    @MockitoBean
    private SambaShareService shares;

    @BeforeEach
    void setUpRemoteExecution() {
        when(sessions.effectiveMode()).thenReturn(Mode.REMOTE);
        when(sessions.sessionMode(anyString())).thenReturn(Mode.REMOTE);
    }

    @Test
    void anonymousReadsRequireAuthenticationWithoutRedirect() throws Exception {
        for (String path : new String[] {"/api/shares", "/api/auth/me"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
        }
        verifyNoInteractions(shares);
    }

    @Test
    void loginWithoutCsrfPersistsAuthenticationForReads() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user").value("qa-user"))
                .andExpect(jsonPath("$.data.host").value("samba-node"));
        mvc.perform(get("/api/shares").session(session)).andExpect(status().isOk());
        verify(sessions).createSession(eq(session.getId()), any(ConnectionRequestDto.class));
        verify(bruteForce).resetFailedLogin("127.0.0.1", "qa-user");
        verify(shares).getAllShares(session.getId());
    }

    @Test
    void sshFailureRejectsLoginAndRegistersFailure() throws Exception {
        doThrow(new SambaCommandException("SSH rejected test connection"))
                .when(sessions)
                .createSession(anyString(), any(ConnectionRequestDto.class));
        MvcResult result =
                mvc.perform(loginRequest()).andExpect(status().isUnauthorized()).andReturn();
        verify(bruteForce).registerFailedLogin("127.0.0.1", "qa-user");
        verify(bruteForce, never()).resetFailedLogin(anyString(), anyString());
        mvc.perform(get("/api/shares")
                        .session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @Test
    void blockedLoginDoesNotAttemptSsh() throws Exception {
        when(bruteForce.isBlocked("127.0.0.1", "qa-user")).thenReturn(true);
        MvcResult result = mvc.perform(loginRequest())
                .andExpect(status().isTooManyRequests())
                .andReturn();
        verify(sessions, never()).createSession(anyString(), any(ConnectionRequestDto.class));
        mvc.perform(get("/api/shares")
                        .session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @Test
    void authenticatedWriteWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(write("POST").session(login())).andExpect(status().isForbidden());
        verifyNoInteractions(shares);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void mismatchedCsrfRejectsWrites(String method) throws Exception {
        MockHttpSession session = login();
        Cookie token = csrfCookie(session);
        mvc.perform(write(method).session(session).cookie(token).header("X-XSRF-TOKEN", token.getValue() + "-wrong"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shares);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void matchingCookieAndHeaderAllowExactlyOneWrite(String method) throws Exception {
        MockHttpSession session = login();
        Cookie token = csrfCookie(session);
        mvc.perform(write(method).session(session).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk());
        switch (method) {
            case "POST" -> verify(shares).createShare(eq(session.getId()), any(SambaShareCreateDto.class));
            case "PUT" -> verify(shares)
                    .updateShare(eq(session.getId()), eq("qa-share"), any(SambaShareCreateDto.class));
            case "DELETE" -> verify(shares).deleteShare(session.getId(), "qa-share");
            default -> throw new IllegalArgumentException(method);
        }
        verifyNoMoreInteractions(shares);
    }

    @Test
    void validCsrfDoesNotAuthenticateAnonymousWrite() throws Exception {
        Cookie token = csrfCookie(login());
        mvc.perform(write("POST").cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @Test
    void logoutWithoutCsrfInvalidatesSessionAndDisconnectsOnce() throws Exception {
        MockHttpSession session = login();
        String sessionId = session.getId();
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        verify(sessions).disconnect(sessionId);
        verify(sessions).createSession(eq(sessionId), any(ConnectionRequestDto.class));
        mvc.perform(get("/api/shares")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @Test
    void anonymousLogoutDisconnectsAndInvalidatesSessionCreatedByMvc() throws Exception {
        AtomicReference<MockHttpSession> createdSession = new AtomicReference<>();
        AtomicReference<String> createdSessionId = new AtomicReference<>();
        doAnswer(invocation -> {
                    // MVC resolves the HttpSession argument by creating a session even for anonymous logout.
                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                    MockHttpSession session =
                            (MockHttpSession) attributes.getRequest().getSession(false);
                    assertNotNull(session);
                    createdSession.set(session);
                    createdSessionId.set(session.getId());
                    return null;
                })
                .when(sessions)
                .disconnect(anyString());

        mvc.perform(post("/api/auth/logout")).andExpect(status().isOk());

        assertNotNull(createdSession.get());
        verify(sessions).disconnect(createdSessionId.get());
        assertTrue(createdSession.get().isInvalid());
        mvc.perform(get("/api/shares")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shares);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCAL", "REMOTE"})
    void anonymousClientCanDiscoverExecutionMode(String mode) throws Exception {
        when(sessions.effectiveMode()).thenReturn(Mode.valueOf(mode));

        mvc.perform(get("/api/auth/environment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value(mode.toLowerCase(java.util.Locale.ROOT)));

        verify(sessions, never()).createSession(anyString(), any(ConnectionRequestDto.class));
        verifyNoInteractions(shares, bruteForce);
    }

    @Test
    void localLoginWithoutHostPersistsAuthenticationAndAllowsCsrfProtectedWrite() throws Exception {
        when(sessions.effectiveMode()).thenReturn(Mode.LOCAL);
        when(sessions.sessionMode(anyString())).thenReturn(Mode.LOCAL);
        MvcResult result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"username":"local-admin","password":"test-only"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user").value("local-admin"))
                .andExpect(jsonPath("$.data.host").value("Local server"));
        Cookie token = csrfCookie(session);
        mvc.perform(write("POST").session(session).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk());

        verify(sessions).createSession(eq(session.getId()), argThat(request -> request.getHost() == null));
        verify(shares).createShare(eq(session.getId()), any(SambaShareCreateDto.class));
        verify(bruteForce).resetFailedLogin("127.0.0.1", "local-admin");
    }

    @Test
    void localLoginWithoutPasswordRejectsRequestWithoutAuthenticating() throws Exception {
        when(sessions.effectiveMode()).thenReturn(Mode.LOCAL);
        MvcResult result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"username":"local-admin"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        mvc.perform(get("/api/shares")
                        .session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
        verify(sessions, never()).createSession(anyString(), any(ConnectionRequestDto.class));
        verifyNoInteractions(shares);
    }

    private MockHttpSession login() throws Exception {
        MvcResult result =
                mvc.perform(loginRequest()).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private MockHttpServletRequestBuilder loginRequest() {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN);
    }

    private Cookie csrfCookie(MockHttpSession session) throws Exception {
        Cookie token = mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertNotNull(token);
        assertFalse(token.getValue().isBlank());
        return token;
    }

    private MockHttpServletRequestBuilder write(String method) {
        return switch (method) {
            case "POST" -> post("/api/shares")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SHARE);
            case "PUT" -> put("/api/shares/qa-share")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SHARE);
            case "DELETE" -> delete("/api/shares/qa-share");
            default -> throw new IllegalArgumentException(method);
        };
    }
}
