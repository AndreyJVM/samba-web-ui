package mari.samba.auth;

import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface AuthService {

    void login(@NonNull String sessionId, @NonNull ConnectionRequestDto request, @NonNull String clientIp)
            throws Exception;

    void logout(@Nullable String sessionId);

    @NonNull Map<String, String> getCurrentUserInfo(@Nullable String sambaHost, @Nullable String sambaUser);
}
