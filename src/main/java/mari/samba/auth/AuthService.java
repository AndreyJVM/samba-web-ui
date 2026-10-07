package mari.samba.auth;

import java.util.Map;

public interface AuthService {

    void login(String sessionId, ConnectionRequestDto request, String clientIp) throws Exception;

    void logout(String sessionId);

    Map<String, String> getCurrentUserInfo(String sambaHost, String sambaUser);
}
