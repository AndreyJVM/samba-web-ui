package mari.samba.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public String handleSessionExpired(IllegalStateException ex) {
        log.warn("Сессия устарела или не найдена: {}", ex.getMessage());
        return "redirect:/?disconnected=true";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model, RedirectAttributes redirectAttributes) {
        log.error("Непредвиденная ошибка: ", ex);
        model.addAttribute("error", ex.getMessage());
        return "error"; // или вывод на текущую страницу через flash attributes
    }
}