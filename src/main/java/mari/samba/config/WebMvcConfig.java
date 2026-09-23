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
    // Тихонько прокидываем короткие адреса на наш индексный файл (без редиректов в браузере)
    registry.addViewController("/").setViewName("forward:/ui/index.html");
    registry.addViewController("/ui").setViewName("forward:/ui/index.html");
    registry.addViewController("/ui/").setViewName("forward:/ui/index.html");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/ui/**")
        .addResourceLocations("classpath:/static/ui/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requestedResource = location.createRelative(resourcePath);

                // Проверяем, что файл действительно существует и ВОЗМОЖНО является файлом
                // (есть точка-расширение, т.к. "createRelative" может вернуть саму папку)
                if (requestedResource.exists()
                    && requestedResource.isReadable()
                    && resourcePath.contains(".")) {
                  return requestedResource;
                }

                // Все запросы без точки (роуты, папки) отдают React SPA index.html
                return new ClassPathResource("/static/ui/index.html");
              }
            });
  }
}
