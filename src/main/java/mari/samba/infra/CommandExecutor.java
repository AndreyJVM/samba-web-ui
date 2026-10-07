package mari.samba.infra;

import mari.samba.core.SambaCommandException;

public interface CommandExecutor {

    String execute(String sessionId, String command) throws SambaCommandException;

    String execute(String sessionId, String command, String inputData) throws SambaCommandException;

    default CommandResult executeCommand(String sessionId, String command) throws SambaCommandException {
        return executeCommand(sessionId, command, null);
    }

    CommandResult executeCommand(String sessionId, String command, String inputData) throws SambaCommandException;
}
