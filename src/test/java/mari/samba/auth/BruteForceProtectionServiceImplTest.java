package mari.samba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;
import mari.samba.config.SambaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionServiceImplTest {

    @Mock
    private SambaProperties properties;

    private BruteForceProtectionServiceImpl bruteForceService;

    @BeforeEach
    void setUp() {
        SambaProperties.Security security = new SambaProperties.Security(List.of(), 3, Duration.ofMinutes(5));
        when(properties.security()).thenReturn(security);
        bruteForceService = new BruteForceProtectionServiceImpl(properties);
    }

    @Test
    void testRegisterFailedLogin_ShouldBlockAfterMaxAttempts() {
        String ip = "192.168.1.100";

        bruteForceService.registerFailedLogin(ip); // 1
        assertThat(bruteForceService.isBlocked(ip)).isFalse();

        bruteForceService.registerFailedLogin(ip); // 2
        assertThat(bruteForceService.isBlocked(ip)).isFalse();

        bruteForceService.registerFailedLogin(ip); // 3 (Max)
        assertThat(bruteForceService.isBlocked(ip)).isTrue();
    }

    @Test
    void testResetFailedLogin_ShouldUnblock() {
        String ip = "10.0.0.1";
        bruteForceService.registerFailedLogin(ip);
        bruteForceService.registerFailedLogin(ip);
        bruteForceService.registerFailedLogin(ip);

        assertThat(bruteForceService.isBlocked(ip)).isTrue();

        bruteForceService.resetFailedLogin(ip);

        assertThat(bruteForceService.isBlocked(ip)).isFalse();
    }

    @Test
    void testDifferentIps_ShouldNotAffectEachOther() {
        String ip1 = "10.0.0.10";
        String ip2 = "10.0.0.11";

        bruteForceService.registerFailedLogin(ip1);
        bruteForceService.registerFailedLogin(ip1);
        bruteForceService.registerFailedLogin(ip1); // Block IP1

        assertThat(bruteForceService.isBlocked(ip1)).isTrue();
        assertThat(bruteForceService.isBlocked(ip2)).isFalse(); // IP2 is still good
    }
}
