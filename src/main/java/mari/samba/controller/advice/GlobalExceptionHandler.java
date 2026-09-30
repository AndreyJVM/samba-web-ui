package mari.samba.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.stream.Collectors;
import mari.samba.dto.common.ApiResponse;
import mari.samba.exception.SambaCommandException;
import mari.samba.exception.SshSessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String DEFAULT_GENERIC_ERROR_MESSAGE = "An unexpected error occurred.";

  @ExceptionHandler(SshSessionExpiredException.class)
  public ResponseEntity<ApiResponse<Void>> handleSshSessionExpired(
      @NonNull SshSessionExpiredException ex,
      @NonNull HttpServletRequest request,
      HttpSession session) {

    log.warn("SSH session expired on {}: {}", request.getRequestURI(), ex.getMessage());

    if (session != null) {
      try {
        session.invalidate();
      } catch (IllegalStateException ignored) {
        // Session already invalidated, safe to ignore
      }
    }

    String msg =
        (ex.getMessage() != null && !ex.getMessage().isBlank())
            ? ex.getMessage()
            : "SSH session has expired or is invalid. Please log in again.";

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(msg));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      @NonNull MethodArgumentNotValidException ex, @NonNull HttpServletRequest request) {

    String errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));

    log.warn("Validation error on {}: {}", request.getRequestURI(), errors);
    return ResponseEntity.badRequest().body(ApiResponse.error("Validation error: " + errors));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
      @NonNull IllegalArgumentException ex, @NonNull HttpServletRequest request) {
    log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiResponse<Void>> handleSecurityException(
      @NonNull SecurityException ex, @NonNull HttpServletRequest request) {
    log.warn("Security violation on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(SambaCommandException.class)
  public ResponseEntity<ApiResponse<Void>> handleSambaCommandException(
      @NonNull SambaCommandException ex, @NonNull HttpServletRequest request) {
    log.error(
        "Samba command execution failure on {}: {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Samba command failed: " + ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(
      @NonNull Exception ex, @NonNull HttpServletRequest request) {
    log.error(
        "Unhandled exception processing {}: {}", request.getRequestURI(), ex.getMessage(), ex);

    String message =
        (ex.getMessage() != null && !ex.getMessage().isBlank())
            ? ex.getMessage()
            : DEFAULT_GENERIC_ERROR_MESSAGE;
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
  }
}
