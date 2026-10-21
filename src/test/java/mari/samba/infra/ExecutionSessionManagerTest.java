package mari.samba.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Duration;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.config.ExecutionProperties;
import mari.samba.config.ExecutionProperties.Mode;
import mari.samba.core.SshSessionExpiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ExecutionSessionManagerTest {

    private final LocalCommandExecutor local = mock(LocalCommandExecutor.class);
    private final SshSessionManager ssh = mock(SshSessionManager.class);
    private final ConnectionRequestDto request =
            new ConnectionRequestDto("server", 22, "admin", "password", "secret", null, null);

    @Test
    void localSessionAuthenticatesAndRoutesCommandsLocally() throws Exception {
        ExecutionSessionManager manager = manager(Mode.LOCAL, "secret");
        when(local.execute("local-session", "command")).thenReturn("local-output");

        manager.createSession("local-session", request);

        assertThat(manager.sessionMode("local-session")).isEqualTo(Mode.LOCAL);
        assertThat(manager.execute("local-session", "command")).isEqualTo("local-output");
        verify(local).authenticate(request);
        verifyNoInteractions(ssh);
    }

    @Test
    void remoteSessionUsesSshAndDisconnectsIt() throws Exception {
        ExecutionSessionManager manager = manager(Mode.REMOTE, "");
        when(ssh.execute("remote-session", "command")).thenReturn("remote-output");

        manager.createSession("remote-session", request);
        assertThat(manager.execute("remote-session", "command")).isEqualTo("remote-output");
        manager.disconnect("remote-session");

        verify(ssh).createSession("remote-session", request);
        verify(ssh).disconnect("remote-session");
        verifyNoInteractions(local);
    }

    @Test
    void autoModeSelectsLocalOnlyWhenCredentialsAreConfigured() {
        assertThat(manager(Mode.AUTO, "secret").effectiveMode()).isEqualTo(Mode.LOCAL);
        assertThat(manager(Mode.AUTO, "").effectiveMode()).isEqualTo(Mode.REMOTE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"synthetic-http-session-token"})
    void unknownSessionCannotExecuteCommands(String sessionId) {
        ExecutionSessionManager manager = manager(Mode.LOCAL, "secret");

        assertThatThrownBy(() -> manager.execute(sessionId, "command"))
                .isInstanceOf(SshSessionExpiredException.class)
                .hasMessage("Execution session not found or expired. Please log in again.");
        verifyNoInteractions(local, ssh);
    }

    private ExecutionSessionManager manager(Mode mode, String password) {
        ExecutionProperties properties =
                new ExecutionProperties(mode, "admin", password, Duration.ofSeconds(1), "/run/shareguard/helper.sock");
        return new ExecutionSessionManager(properties, local, ssh);
    }
}
