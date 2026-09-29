package mari.samba.service;

import java.util.Map;
import mari.samba.dto.auth.ConnectionRequestDto;

public interface AuthService {

  void login(String sessionId, ConnectionRequestDto request, String clientIp) throws Exception;

  void logout(String sessionId);

  Map<String, String> getCurrentUserInfo(String sambaHost, String sambaUser);
}
