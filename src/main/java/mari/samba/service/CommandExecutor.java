package mari.samba.service;

public interface CommandExecutor {

    /**
     * Выполнить shell-команду на удаленном узле.
     */
    String execute(String sessionId, String command) throws Exception;

    /**
     * Выполнить shell-команду с передачей входных данных в stdin (для паролей, файлов).
     */
    String execute(String sessionId, String command, String inputData) throws Exception;
}