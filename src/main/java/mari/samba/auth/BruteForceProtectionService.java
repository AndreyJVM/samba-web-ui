package mari.samba.auth;

public interface BruteForceProtectionService {
    void registerFailedLogin(String ipAddress, String username);

    void resetFailedLogin(String ipAddress, String username);

    boolean isBlocked(String ipAddress, String username);
}
