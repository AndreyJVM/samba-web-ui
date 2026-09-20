package mari.samba.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Интерцепторы удалены, используем Spring Security

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  // Ранее здесь регистрировался SshAuthInterceptor
  // Теперь эта конфигурация может быть расширена для глобальных CORS или других настроек
}
