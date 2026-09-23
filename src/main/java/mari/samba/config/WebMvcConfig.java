package mari.samba.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // Перенаправляем корневой запрос / сразу на /ui/
    registry.addViewController("/").setViewName("redirect:/ui/");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Настраиваем отдачу статики и SPA-роутинг для React
    registry
        .addResourceHandler("/ui/**")
        .addResourceLocations("classpath:/static/ui/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                // Пытаемся найти реально существующий файл (js, css, index.html)
                Resource requestedResource = location.createRelative(resourcePath);
                if (requestedResource.exists() && requestedResource.isReadable()) {
                  return requestedResource;
                }
                // Если файла нет (т.е. это роут React'а, например /ui/users), возвращаем index.html
                return new ClassPathResource("/static/ui/index.html");
              }
            });
  }
}
