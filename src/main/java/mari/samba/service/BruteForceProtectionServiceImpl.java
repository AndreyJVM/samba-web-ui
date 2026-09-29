package mari.samba.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import mari.samba.config.SambaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BruteForceProtectionServiceImpl implements BruteForceProtectionService {

  private final int maxAttempts;
  private final Cache<String, Integer> attemptsCache;

  public BruteForceProtectionServiceImpl(@Autowired(required = false) SambaProperties properties) {
    this.maxAttempts =
        properties != null && properties.security() != null
            ? properties.security().maxLoginAttempts()
            : 5;
    Duration blockDuration =
        properties != null && properties.security() != null
            ? properties.security().loginBlockDuration()
            : Duration.ofMinutes(3);
    this.attemptsCache = Caffeine.newBuilder().expireAfterWrite(blockDuration).build();
  }

  @Override
  public void registerFailedLogin(String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    attemptsCache.put(ipAddress, (attempts != null ? attempts : 0) + 1);
  }

  @Override
  public void resetFailedLogin(String ipAddress) {
    attemptsCache.invalidate(ipAddress);
  }

  @Override
  public boolean isBlocked(String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    return attempts != null && attempts >= maxAttempts;
  }
}
