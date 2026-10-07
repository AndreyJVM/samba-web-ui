package mari.samba.auth;

public interface BruteForceProtectionService {
    void registerFailedLogin(String ipAddress);

    void resetFailedLogin(String ipAddress);

    boolean isBlocked(String ipAddress);
}
