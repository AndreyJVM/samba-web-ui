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

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Void>> connect(
      @Valid @RequestBody ConnectionRequestDto request, HttpServletRequest httpRequest) {

    if (!request.isKeyAuth()
        && (request.getPassword() == null || request.getPassword().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Пароль обязателен для аутентификации без ключа"));
    }

    try {
      HttpSession httpSession = httpRequest.getSession(true);
      String sessionId = httpSession.getId();

      // 1. Создаем SSH-подключение
      sessionManager.createSession(sessionId, request);

      // 2. Инициализируем Spring Security Authenticate
      List<SimpleGrantedAuthority> authorities =
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(request.getUsername(), null, authorities);

      // 3. Сохраняем в контекст
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // 4. Обязательно сохраняем контекст в сессию
      httpSession.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());

      // 5. Визуальные метки для логов / фронтенда
      httpSession.setAttribute(
          "sambaHost",
          request.getHost()
              + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : ""));
      httpSession.setAttribute("sambaUser", request.getUsername());

      return ResponseEntity.ok(ApiResponse.ok("Успешно подключено к серверу", null));
    } catch (Exception e) {
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
    return ResponseEntity.ok(ApiResponse.ok("Успешно отключено", null));
  }
}
