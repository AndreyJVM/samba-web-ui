package mari.samba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import mari.samba.core.SambaCommandException;
import mari.samba.infra.ExecutionSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private ExecutionSessionManager sessions;

    @Mock
    private BruteForceProtectionService bruteForce;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new AuthServiceImpl(sessions, bruteForce);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingPasswordDoesNotCreateSession(String password) {
        ConnectionRequestDto request =
                new ConnectionRequestDto("server", 22, "admin", "password", password, null, null);

        assertThatThrownBy(() -> service.login("session", request, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");

        verifyNoInteractions(sessions);
        verify(bruteForce).isBlocked("127.0.0.1", "admin");
        verifyNoMoreInteractions(bruteForce);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingPrivateKeyDoesNotCreateSession(String privateKey) {
        ConnectionRequestDto request = new ConnectionRequestDto("server", 22, "admin", "key", null, privateKey, null);

        assertThatThrownBy(() -> service.login("session", request, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Private key is required");

        verifyNoInteractions(sessions);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingHostDoesNotCreateSession(String host) {
        ConnectionRequestDto request = new ConnectionRequestDto(host, 22, "admin", "password", "test-only", null, null);
        assertThatThrownBy(() -> service.login("session", request, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Host is required");
        verifyNoInteractions(sessions);
        verify(bruteForce, never()).resetFailedLogin(anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void connectionFailureRegistersAttemptWithoutAuthenticating() throws Exception {
        ConnectionRequestDto request =
                new ConnectionRequestDto("server", 22, "admin", "password", "test-only", null, null);
        SambaCommandException failure = new SambaCommandException("Connection rejected");
        doThrow(failure).when(sessions).createSession("session", request);

        assertThatThrownBy(() -> service.login("session", request, "127.0.0.1")).isSameAs(failure);

        verify(bruteForce).registerFailedLogin("127.0.0.1", "admin");
        verify(bruteForce, never()).resetFailedLogin(anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void logoutWithoutSessionClearsAuthenticationWithoutDisconnecting() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

        service.logout(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(sessions, bruteForce);
    }
}
