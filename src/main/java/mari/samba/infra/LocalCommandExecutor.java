package mari.samba.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.config.ExecutionProperties;
import mari.samba.core.SambaCommandException;
import org.springframework.stereotype.Component;

@Component
public class LocalCommandExecutor implements CommandExecutor {

    private final ExecutionProperties properties;
    private final LocalCommandPolicy commandPolicy;
    private final PrivilegedHelperClient helperClient;

    public LocalCommandExecutor(
            ExecutionProperties properties, LocalCommandPolicy commandPolicy, PrivilegedHelperClient helperClient) {
        this.properties = properties;
        this.commandPolicy = commandPolicy;
        this.helperClient = helperClient;
    }

    public void authenticate(ConnectionRequestDto request) {
        if (request.isKeyAuth()) {
            throw new IllegalArgumentException("Local mode supports password authentication only.");
        }
        if (properties.localPassword() == null || properties.localPassword().isBlank()) {
            throw new IllegalStateException("Local authentication is not configured.");
        }
        if (!secureEquals(properties.localUsername(), request.getUsername())
                || !secureEquals(properties.localPassword(), request.getPassword())) {
            throw new SambaCommandException("Invalid local credentials.");
        }
    }

    @Override
    public String execute(String sessionId, String command) {
        return execute(sessionId, command, null);
    }

    @Override
    public String execute(String sessionId, String command, String inputData) {
        CommandResult result = executeCommand(sessionId, command, inputData);
        if (!result.isSuccess()) {
            throw new SambaCommandException("Local command failed with code " + result.exitCode() + ".");
        }
        return result.stdout();
    }

    @Override
    public CommandResult executeCommand(String sessionId, String command) {
        return executeCommand(sessionId, command, null);
    }

    @Override
    public CommandResult executeCommand(String sessionId, String command, String inputData) {
        PrivilegedCommand privilegedCommand = commandPolicy.classify(command, inputData);
        return helperClient.execute(privilegedCommand);
    }

    private static boolean secureEquals(String expected, String actual) {
        byte[] expectedBytes = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
