package mari.samba.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.exception.SambaCommandException;
import mari.samba.service.BruteForceProtectionService;
import mari.samba.service.infra.SshSessionManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  private final SshSessionManager sessionManager;
  private final BruteForceProtectionService bruteForceService;

  public AuthApiController(
      SshSessionManager sessionManager, BruteForceProtectionService bruteForceService) {
    this.sessionManager = sessionManager;
    this.bruteForceService = bruteForceService;
  }

  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isEmpty()) {
      return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0];
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Void>> connect(
      @Valid @RequestBody ConnectionRequestDto request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);

    if (bruteForceService.isBlocked(clientIp)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(
              ApiResponse.error(
                  "Слишком много неудачных попыток входа (макс 5). Подождите 3 минуты."));
    }

    if (!request.isKeyAuth()
        && (request.getPassword() == null || request.getPassword().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Пароль обязателен для аутентификации без ключа"));
    }

    try {
      HttpSession session = httpRequest.getSession(true);
      String sessionId = session.getId();
      sessionManager.createSession(sessionId, request);

      List<SimpleGrantedAuthority> authorities =
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(request.getUsername(), null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);

      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());

      session.setAttribute(
          "sambaHost",
          request.getHost()
              + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : ""));
      session.setAttribute("sambaUser", request.getUsername());

      bruteForceService.resetFailedLogin(clientIp);

      return ResponseEntity.ok(ApiResponse.ok("Успешно подключено к серверу", null));

    } catch (SambaCommandException e) {
      bruteForceService.registerFailedLogin(clientIp);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              ApiResponse.error(
                  "Ошибка подключения: "
                      + e.getMessage()
                      + ". Убедитесь, что сервер включен, а учетные данные верны."));
    } catch (IllegalArgumentException e) {
      bruteForceService.registerFailedLogin(clientIp);
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      bruteForceService.registerFailedLogin(clientIp);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Внутренняя ошибка сервера: " + e.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser(
      HttpServletRequest request, HttpSession httpSession) {

    // ВАЖНО: В Spring Security 6.x CSRF-токен по умолчанию отложенный (deferred).
    // Популярная проблема SPA: кука XSRF-TOKEN не отсылается в браузер, пока токен не будет
    // запрошен явно.
    // Запрашиваем его здесь (при загрузке интерфейса), чтобы Spring гарантированно прописал куку.
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      csrfToken.getToken();
    }

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
  public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
    if (session != null) {
      String sessionId = session.getId();
      sessionManager.disconnect(sessionId);
      session.invalidate();
    }
    return ResponseEntity.ok(ApiResponse.ok("Успешно отключено", null));
  }
}
