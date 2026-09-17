package mari.samba.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.exception.SshSessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SshSessionExpiredException.class)
    public Object handleSshSessionExpired(SshSessionExpiredException ex,
                                          HttpServletRequest request,
                                          HttpSession session) {
        log.warn("SSH-сессия разорвана для URI [{}]: {}", request.getRequestURI(), ex.getMessage());

        // Инвалидируем HTTP-сессию браузера, чтобы стереть устаревшие данные авторизации
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // Сессия уже могла быть инвалидирована
        }

        // Если запрос пришел от фонового REST API (наш fetch/ajax опрос дисков)
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("SSH-сессия истекла. Требуется повторный вход."));
        }

        // Если это обычный переход по страницам в браузере
        return "redirect:/?disconnected=true";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex,
                                         HttpServletRequest request,
                                         Model model) {
        log.error("Непредвиденная ошибка при обработке [{}]: ", request.getRequestURI(), ex);

        // API запросы должны получать аккуратный JSON даже при сбое
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Внутренняя ошибка сервера"));
        }

        model.addAttribute("error", ex.getMessage());
        return "error";
    }
}