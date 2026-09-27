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
    log.warn(
        "SSH-СЃРµСЃСЃРёСЏ СЂР°Р·РѕСЂРІР°РЅР° РґР»СЏ URI [{}]: {}",
        request.getRequestURI(),
        ex.getMessage());

    try {
      session.invalidate();
    } catch (IllegalStateException ignored) {
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ApiResponse.error(
                "SSH-СЃРµСЃСЃРёСЏ РёСЃС‚РµРєР»Р°. РўСЂРµР±СѓРµС‚СЃСЏ РїРѕРІС‚РѕСЂРЅС‹Р№ РІС…РѕРґ."));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn(
        "РћС€РёР±РєР° РІР°Р»РёРґР°С†РёРё РґР»СЏ URI [{}]: {}",
        request.getRequestURI(),
        ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(
      Exception ex, HttpServletRequest request) {
    log.error(
        "РќРµРїСЂРµРґРІРёРґРµРЅРЅР°СЏ РѕС€РёР±РєР° РїСЂРё РѕР±СЂР°Р±РѕС‚РєРµ [{}]: ",
        request.getRequestURI(),
        ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                ex.getMessage() != null
                    ? ex.getMessage()
                    : "Р’РЅСѓС‚СЂРµРЅРЅСЏСЏ РѕС€РёР±РєР° СЃРµСЂРІРµСЂР°"));
  }
}
