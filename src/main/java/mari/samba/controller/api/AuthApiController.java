package mari.samba.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.dto.common.ApiResponse;
import mari.samba.exception.SambaCommandException;
import mari.samba.service.AuthService;
import mari.samba.service.AuthServiceImpl;
import mari.samba.service.BruteForceProtectionService;
import mari.samba.service.infra.SshSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  private final AuthService authService;
  private final SshSessionManager sessionManager;
  private final BruteForceProtectionService bruteForceService;

  public AuthApiController(
      SshSessionManager sessionManager,
      BruteForceProtectionService bruteForceService,
      @Autowired(required = false) AuthService authService) {
    this.sessionManager = sessionManager;
    this.bruteForceService = bruteForceService;
    this.authService =
        authService != null ? authService : new AuthServiceImpl(sessionManager, bruteForceService);
  }

  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isEmpty()) {
      return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0].trim();
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Void>> connect(
      @Valid @RequestBody ConnectionRequestDto request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);

    try {
      HttpSession session = httpRequest.getSession(true);
      String sessionId = session.getId();

      authService.login(sessionId, request, clientIp);

      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());

      String hostDisplay =
          request.getHost()
              + (request.getResolvedPort() != 22 ? ":" + request.getResolvedPort() : "");
      session.setAttribute("sambaHost", hostDisplay);
      session.setAttribute("sambaUser", request.getUsername());

      return ResponseEntity.ok(ApiResponse.ok("Authentication successful", null));

    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(ApiResponse.error(e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    } catch (SambaCommandException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Login error: " + e.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser(
      HttpServletRequest request, HttpSession httpSession) {

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

    Map<String, String> data = authService.getCurrentUserInfo(host, user);
    return ResponseEntity.ok(ApiResponse.ok(data));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
    if (session != null) {
      authService.logout(session.getId());
      session.invalidate();
    } else {
      authService.logout(null);
    }
    return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
  }
}
