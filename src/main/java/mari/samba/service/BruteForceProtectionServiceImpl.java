package mari.samba.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class BruteForceProtectionServiceImpl implements BruteForceProtectionService {

  private static final int MAX_ATTEMPTS = 5;

  // Кэш, который автоматически удаляет записи спустя 3 минуты после последней записи
  private final Cache<String, Integer> attemptsCache =
      Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();

  @Override
  public void registerFailedLogin(String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    if (attempts == null) {
      attempts = 0;
    }
    attemptsCache.put(ipAddress, attempts + 1);
  }

  @Override
  public void resetFailedLogin(String ipAddress) {
    attemptsCache.invalidate(ipAddress);
  }

  @Override
  public boolean isBlocked(String ipAddress) {
    Integer attempts = attemptsCache.getIfPresent(ipAddress);
    return attempts != null && attempts >= MAX_ATTEMPTS;
  }
}
