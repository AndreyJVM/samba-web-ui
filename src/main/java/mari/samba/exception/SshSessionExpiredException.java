package mari.samba.exception;

public class SshSessionExpiredException extends RuntimeException {
    public SshSessionExpiredException(String message) {
        super(message);
    }
}