package mari.samba.service.infra;

import mari.samba.exception.SambaCommandException;

public interface CommandExecutor {

  String execute(String sessionId, String command) throws SambaCommandException;

  String execute(String sessionId, String command, String inputData) throws SambaCommandException;
}
