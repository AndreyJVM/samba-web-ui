package mari.samba.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mari.samba.infra.ExecutionSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final ExecutionSessionManager sessionManager;
    private final BruteForceProtectionService bruteForceService;

    public AuthServiceImpl(
            @NonNull ExecutionSessionManager sessionManager, @NonNull BruteForceProtectionService bruteForceService) {
        this.sessionManager = sessionManager;
        this.bruteForceService = bruteForceService;
    }

    @Override
    public void login(@NonNull String sessionId, @NonNull ConnectionRequestDto request, @NonNull String clientIp)
            throws Exception {

        if (bruteForceService.isBlocked(clientIp, request.getUsername())) {
            log.warn("Blocked login attempt from IP {} due to too many failed attempts", clientIp);
            throw new SecurityException("Too many failed login attempts. IP temporarily blocked.");
        }

        validateCredentials(request);
        connectSsh(sessionId, request, clientIp);

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(request.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        bruteForceService.resetFailedLogin(clientIp, request.getUsername());
        log.info("User '{}' successfully authenticated for host '{}'", request.getUsername(), request.getHost());
    }

    private static void validateCredentials(ConnectionRequestDto request) {
        if (!request.isKeyAuth()
                && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new IllegalArgumentException("Password is required when not using key authentication.");
        }

        if (request.isKeyAuth()
                && (request.getPrivateKey() == null || request.getPrivateKey().isBlank())) {
            throw new IllegalArgumentException("Private key is required when using key authentication.");
        }

        if (request.getHost() == null || request.getHost().isBlank()) {
            throw new IllegalArgumentException("Host is required for remote connections.");
        }
    }

    private void connectSsh(String sessionId, ConnectionRequestDto request, String clientIp) throws Exception {
        try {
            sessionManager.createSession(sessionId, request);
        } catch (Exception e) {
            bruteForceService.registerFailedLogin(clientIp, request.getUsername());
            log.warn(
                    "Failed authentication attempt for user '{}' from IP {}: {}",
                    request.getUsername(),
                    clientIp,
                    e.getMessage());
            throw e;
        }
    }

    @Override
    public void logout(@Nullable String sessionId) {
        if (sessionId != null) {
            sessionManager.disconnect(sessionId);
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    @NonNull public Map<String, String> getCurrentUserInfo(@Nullable String sambaHost, @Nullable String sambaUser) {
        Map<String, String> userInfo = new HashMap<>(2);
        userInfo.put("host", sambaHost != null ? sambaHost : "Not connected");
        userInfo.put("user", sambaUser != null ? sambaUser : "Unknown");
        return userInfo;
    }
}
