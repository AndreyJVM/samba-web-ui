package mari.samba.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import mari.samba.config.SambaProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class BruteForceProtectionServiceImpl implements BruteForceProtectionService {

    private final int maxAttempts;
    private final Cache<String, Integer> ipAttemptsCache;
    private final Cache<String, Integer> userAttemptsCache;

    public BruteForceProtectionServiceImpl(SambaProperties properties) {
        this.maxAttempts = properties.security().maxLoginAttempts();
        Duration blockDuration = properties.security().loginBlockDuration();

        this.ipAttemptsCache =
                Caffeine.newBuilder().expireAfterWrite(blockDuration).build();
        this.userAttemptsCache =
                Caffeine.newBuilder().expireAfterWrite(blockDuration).build();
    }

    @Override
    public void registerFailedLogin(@NonNull String ipAddress, @NonNull String username) {
        ipAttemptsCache.asMap().merge(ipAddress, 1, Integer::sum);

        if (username != null && !username.isBlank()) {
            userAttemptsCache.asMap().merge(username, 1, Integer::sum);
        }
    }

    @Override
    public void resetFailedLogin(@NonNull String ipAddress, @NonNull String username) {
        ipAttemptsCache.invalidate(ipAddress);
        if (username != null && !username.isBlank()) {
            userAttemptsCache.invalidate(username);
        }
    }

    @Override
    public boolean isBlocked(@NonNull String ipAddress, @NonNull String username) {
        Integer ipAttempts = ipAttemptsCache.getIfPresent(ipAddress);
        if (ipAttempts != null && ipAttempts >= maxAttempts) {
            return true;
        }

        if (username != null && !username.isBlank()) {
            Integer userAttempts = userAttemptsCache.getIfPresent(username);
            return userAttempts != null && userAttempts >= maxAttempts;
        }

        return false;
    }
}
