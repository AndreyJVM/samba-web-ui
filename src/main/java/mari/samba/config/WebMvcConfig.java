package mari.samba.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui/").setViewName("forward:/ui/index.html");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/ui/**")
                .addResourceLocations("classpath:/static/ui/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                            throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        if (requestedResource.exists()
                                && requestedResource.isReadable()
                                && resourcePath.contains(".")) {
                            return requestedResource;
                        }

                        // Fallback for React/Vue SPA routing
                        return new ClassPathResource("/static/ui/index.html");
                    }
                });
    }
}
