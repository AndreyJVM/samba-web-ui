package mari.samba.infra;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mari.samba.auth.ConnectionRequestDto;
import mari.samba.core.SambaCommandException;
import mari.samba.core.SshSessionExpiredException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ExecutionSessionManager implements CommandExecutor {

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final SshSessionManager sshExecutor;

    public ExecutionSessionManager(SshSessionManager sshExecutor) {
        this.sshExecutor = sshExecutor;
    }

    public void createSession(String sessionId, ConnectionRequestDto request) throws Exception {
        disconnect(sessionId);
        sshExecutor.createSession(sessionId, request);
        sessions.add(sessionId);
    }

    public void disconnect(String sessionId) {
        if (sessionId != null && sessions.remove(sessionId)) {
            sshExecutor.disconnect(sessionId);
        }
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
        if (sessionId == null || !sessions.contains(sessionId)) {
            throw new SshSessionExpiredException("Execution session not found or expired. Please log in again.");
        }
        return sshExecutor;
    }
}
