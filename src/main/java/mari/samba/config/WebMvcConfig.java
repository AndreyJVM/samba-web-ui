package mari.samba.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // Перенаправляем корневой запрос на наш React frontend
    registry.addViewController("/").setViewName("forward:/ui/index.html");
    registry.addViewController("/ui").setViewName("forward:/ui/index.html");

    // Проксируем 404 роуты (которые не /api/) на Frontend SPA Router (например, /ui/dashboard)
    registry.addViewController("/ui/**").setViewName("forward:/ui/index.html");
  }
}
