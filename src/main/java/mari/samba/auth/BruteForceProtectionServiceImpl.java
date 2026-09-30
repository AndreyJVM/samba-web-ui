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
  private final Cache<String, Integer> attemptsCache;

  public BruteForceProtectionServiceImpl(SambaProperties properties) {
    this.maxAttempts = properties.security().maxLoginAttempts();
    Duration blockDuration = properties.security().loginBlockDuration();

    this.attemptsCache = Caffeine.newBuilder().expireAfterWrite(blockDuration).build();
  }

  @Override
  public void registerFailedLogin(@NonNull String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    attemptsCache.put(ipAddress, (attempts != null ? attempts : 0) + 1);
  }

  @Override
  public void resetFailedLogin(@NonNull String ipAddress) {
    attemptsCache.invalidate(ipAddress);
  }

  @Override
  public boolean isBlocked(@NonNull String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    return attempts != null && attempts >= maxAttempts;
  }
}
