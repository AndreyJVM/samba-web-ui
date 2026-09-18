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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Обрабатываем IllegalArgumentException для передачи безопасных сообщений об ошибках
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex,
                                                 HttpServletRequest request,
                                                 RedirectAttributes redirectAttributes) {
        log.warn("Ошибка валидации для URI [{}]: {}", request.getRequestURI(), ex.getMessage());

        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        }

        // Если это POST/GET запрос из браузера, перенаправляем на предыдущую страницу
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + getPreviousPageByRequest(request).orElse("/shares");
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        log.error("Непредвиденная ошибка при обработке [{}]: ", request.getRequestURI(), ex);

        // API запросы должны получать аккуратный JSON даже при сбое
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Внутренняя ошибка сервера"));
        }

        // Используем HTTP Referer, чтобы вернуть пользователя на ту же страницу с ошибкой
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty() && request.getMethod().equalsIgnoreCase("POST")) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + referer;
        }

        // Если это GET-запрос и произошла ошибка рендера страницы
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    /**
     * Пытается найти URL предыдущей страницы, чтобы вернуть пользователя туда после ошибки.
     */
    private java.util.Optional<String> getPreviousPageByRequest(HttpServletRequest request) {
        return java.util.Optional.ofNullable(request.getHeader("Referer"));
    }
}