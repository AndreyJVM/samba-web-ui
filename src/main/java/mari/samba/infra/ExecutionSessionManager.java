package mari.samba.infra;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.config.ExecutionProperties;
import mari.samba.config.ExecutionProperties.Mode;
import mari.samba.core.SambaCommandException;
import mari.samba.core.SshSessionExpiredException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ExecutionSessionManager implements CommandExecutor {

    private final Map<String, Mode> sessionModes = new ConcurrentHashMap<>();
    private final ExecutionProperties properties;
    private final LocalCommandExecutor localExecutor;
    private final SshSessionManager sshExecutor;

    public ExecutionSessionManager(
            ExecutionProperties properties, LocalCommandExecutor localExecutor, SshSessionManager sshExecutor) {
        this.properties = properties;
        this.localExecutor = localExecutor;
        this.sshExecutor = sshExecutor;
    }

    public void createSession(String sessionId, ConnectionRequestDto request) throws Exception {
        disconnect(sessionId);
        Mode mode = effectiveMode();
        if (mode == Mode.LOCAL) {
            localExecutor.authenticate(request);
        } else {
            sshExecutor.createSession(sessionId, request);
        }
        sessionModes.put(sessionId, mode);
    }

    public void disconnect(String sessionId) {
        Mode mode = sessionModes.remove(sessionId);
        if (mode == Mode.REMOTE) {
            sshExecutor.disconnect(sessionId);
        }
    }

    public Mode effectiveMode() {
        return properties.effectiveMode();
    }

    public Mode sessionMode(String sessionId) {
        return sessionModes.getOrDefault(sessionId, effectiveMode());
    }

    @Override
    public String execute(String sessionId, String command) throws SambaCommandException {
        return executorFor(sessionId).execute(sessionId, command);
    }

    @Override
    public String execute(String sessionId, String command, String inputData) throws SambaCommandException {
        return executorFor(sessionId).execute(sessionId, command, inputData);
    }

    @Override
    public CommandResult executeCommand(String sessionId, String command, String inputData)
            throws SambaCommandException {
        return executorFor(sessionId).executeCommand(sessionId, command, inputData);
    }

    private CommandExecutor executorFor(String sessionId) {
        Mode mode = sessionModes.get(sessionId);
        if (mode == null) {
            throw new SshSessionExpiredException("Execution session not found or expired: " + sessionId);
        }
        return mode == Mode.LOCAL ? localExecutor : sshExecutor;
    }
}
