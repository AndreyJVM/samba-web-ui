package mari.samba.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.exception.SshSessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(SshSessionExpiredException.class)
  public ResponseEntity<ApiResponse<Void>> handleSshSessionExpired(
      SshSessionExpiredException ex, HttpServletRequest request, HttpSession session) {
    log.warn("SSH-сессия разорвана для URI [{}]: {}", request.getRequestURI(), ex.getMessage());

    try {
      session.invalidate();
    } catch (IllegalStateException ignored) {
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("SSH-сессия истекла. Требуется повторный вход."));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Ошибка валидации для URI [{}]: {}", request.getRequestURI(), ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(
      Exception ex, HttpServletRequest request) {
    log.error("Непредвиденная ошибка при обработке [{}]: ", request.getRequestURI(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                ex.getMessage() != null ? ex.getMessage() : "Внутренняя ошибка сервера"));
  }
}
