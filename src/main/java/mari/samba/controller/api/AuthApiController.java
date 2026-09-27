package mari.samba.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.service.infra.SshSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  @Autowired private SshSessionManager sessionManager;

  // In-memory simplistic Rate Limiter for Brute-Force defense limit (IP -> Block Timestamp)
  private final ConcurrentHashMap<String, FailedAttemptNode> failedAttempts =
      new ConcurrentHashMap<>();
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final long LOCK_TIME_MS = 3 * 60 * 1000; // 3 minutes lockout

  private static class FailedAttemptNode {
    int attempts;
    long lastAttemptTime;

    FailedAttemptNode(int attempts, long lastAttemptTime) {
      this.attempts = attempts;
      this.lastAttemptTime = lastAttemptTime;
    }
  }

  private boolean isRateLimited(String ipAddress) {
    FailedAttemptNode node = failedAttempts.get(ipAddress);
    if (node == null) return false;

    if (System.currentTimeMillis() - node.lastAttemptTime > LOCK_TIME_MS) {
      failedAttempts.remove(ipAddress);
      return false;
    }
    return node.attempts >= MAX_FAILED_ATTEMPTS;
  }

  private void recordFailedAttempt(String ipAddress) {
    failedAttempts.compute(
        ipAddress,
        (ip, node) -> {
          if (node == null) {
            return new FailedAttemptNode(1, System.currentTimeMillis());
          }
          node.attempts++;
          node.lastAttemptTime = System.currentTimeMillis();
          return node;
        });
  }

  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
      return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0];
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Void>> connect(
      @Valid @RequestBody ConnectionRequestDto request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);

    if (isRateLimited(clientIp)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(
              ApiResponse.error(
                  "Security Policy: Too many failed login attempts. Try again in 3 minutes."));
    }

    if (!request.isKeyAuth()
        && (request.getPassword() == null || request.getPassword().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Пароль обязателен для стандартной аутентификации"));
    }

    try {
      HttpSession httpSession = httpRequest.getSession(true);
      String sessionId = httpSession.getId();

      // 1. Создаем SSH-сессию
      sessionManager.createSession(sessionId, request);

      // 2. Оформляем Spring Security Authenticate
      List<SimpleGrantedAuthority> authorities =
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(request.getUsername(), null, authorities);

      // 3. Записываем в контекст
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // 4. Устанавливаем куки-сессии (для интеграции с Spring Security API если нужно)
      httpSession.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());

      // 5. Полезные данные для отображения
      httpSession.setAttribute(
          "sambaHost",
          request.getHost()
              + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : ""));
      httpSession.setAttribute("sambaUser", request.getUsername());

      // Очистка при успешном входе
      failedAttempts.remove(clientIp);

      return ResponseEntity.ok(ApiResponse.ok("Успешно подключились к серверу!", null));
    } catch (Exception e) {
      recordFailedAttempt(clientIp);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("Ошибка подключения: " + e.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser(HttpSession httpSession) {
    if (httpSession == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String host = (String) httpSession.getAttribute("sambaHost");
    String user = (String) httpSession.getAttribute("sambaUser");

    if (host == null || user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Map<String, String> data = new HashMap<>();
    data.put("host", host);
    data.put("user", user);

    return ResponseEntity.ok(ApiResponse.ok(data));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> disconnect(HttpSession httpSession) {
    if (httpSession != null) {
      String sessionId = httpSession.getId();
      sessionManager.disconnect(sessionId);
      httpSession.invalidate();
    }
    return ResponseEntity.ok(ApiResponse.ok("Успешно отключились", null));
  }
}
