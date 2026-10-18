package mari.samba.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.config.ExecutionProperties;
import mari.samba.config.ExecutionProperties.Mode;
import mari.samba.core.SambaCommandException;
import org.junit.jupiter.api.Test;

class LocalCommandExecutorTest {

    private final LocalCommandPolicy commandPolicy = mock(LocalCommandPolicy.class);
    private final PrivilegedHelperClient helperClient = mock(PrivilegedHelperClient.class);

    @Test
    void authenticatesConfiguredLocalCredentials() {
        LocalCommandExecutor executor = executor("secret", Duration.ofSeconds(1));

        executor.authenticate(request("admin", "secret"));

        assertThatThrownBy(() -> executor.authenticate(request("admin", "wrong")))
                .isInstanceOf(SambaCommandException.class)
                .hasMessage("Invalid local credentials.");
    }

    @Test
    void rejectsKeyAuthenticationAndMissingConfiguration() {
        LocalCommandExecutor executor = executor("", Duration.ofSeconds(1));
        ConnectionRequestDto keyRequest =
                new ConnectionRequestDto(null, null, "admin", "key", null, "private-key", null);

        assertThatThrownBy(() -> executor.authenticate(keyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Local mode supports password authentication only.");
        assertThatThrownBy(() -> executor.authenticate(request("admin", "secret")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Local authentication is not configured.");
    }

    @Test
    void executesAllowedOperationThroughPrivilegedHelper() {
        LocalCommandExecutor executor = executor("secret", Duration.ofSeconds(1));
        PrivilegedCommand command =
                new PrivilegedCommand(PrivilegedOperation.WRITE_TEMP_CONFIG, List.of("/tmp/smb.conf.tmp"), "payload");
        when(commandPolicy.classify("generated-command", "payload")).thenReturn(command);
        when(helperClient.execute(command)).thenReturn(new CommandResult(0, "written", ""));

        CommandResult result = executor.executeCommand("session", "generated-command", "payload");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("written");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void reportsNonZeroExitWithoutIncludingCommand() {
        LocalCommandExecutor executor = executor("secret", Duration.ofMillis(50));
        PrivilegedCommand command =
                new PrivilegedCommand(PrivilegedOperation.SYSTEMD, List.of("restart", "smbd"), null);
        when(commandPolicy.classify("sensitive-command", null)).thenReturn(command);
        when(helperClient.execute(command)).thenReturn(new CommandResult(7, "", "operation failed"));

        assertThatThrownBy(() -> executor.execute("session", "sensitive-command"))
                .isInstanceOf(SambaCommandException.class)
                .hasMessageContaining("code 7")
                .hasMessageNotContaining("sensitive-command");
    }

    private LocalCommandExecutor executor(String password, Duration timeout) {
        ExecutionProperties properties =
                new ExecutionProperties(Mode.LOCAL, "admin", password, timeout, "/run/shareguard/helper.sock");
        return new LocalCommandExecutor(properties, commandPolicy, helperClient);
    }

    private static ConnectionRequestDto request(String username, String password) {
        return new ConnectionRequestDto(null, null, username, "password", password, null, null);
    }
}
