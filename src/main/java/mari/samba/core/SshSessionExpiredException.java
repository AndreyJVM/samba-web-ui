package mari.samba.core;

public class SshSessionExpiredException extends RuntimeException {
  public SshSessionExpiredException(String message) {
    super(message);
  }
}
