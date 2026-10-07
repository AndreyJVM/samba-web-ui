package mari.samba.core;

public class SambaCommandException extends RuntimeException {
    public SambaCommandException(String message) {
        super(message);
    }

    public SambaCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
