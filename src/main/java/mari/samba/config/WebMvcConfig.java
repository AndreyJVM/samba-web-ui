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
    // РўРёС…РѕРЅСЊРєРѕ РїСЂРѕРєРёРґС‹РІР°РµРј РєРѕСЂРѕС‚РєРёРµ Р°РґСЂРµСЃР° РЅР° РЅР°С€
    // РёРЅРґРµРєСЃРЅС‹Р№ С„Р°Р№Р» (Р±РµР· СЂРµРґРёСЂРµРєС‚РѕРІ РІ Р±СЂР°СѓР·РµСЂРµ)
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

                // РџСЂРѕРІРµСЂСЏРµРј, С‡С‚Рѕ С„Р°Р№Р» РґРµР№СЃС‚РІРёС‚РµР»СЊРЅРѕ
                // СЃСѓС‰РµСЃС‚РІСѓРµС‚ Рё Р’РћР—РњРћР–РќРћ СЏРІР»СЏРµС‚СЃСЏ С„Р°Р№Р»РѕРј
                // (РµСЃС‚СЊ С‚РѕС‡РєР°-СЂР°СЃС€РёСЂРµРЅРёРµ, С‚.Рє. "createRelative" РјРѕР¶РµС‚
                // РІРµСЂРЅСѓС‚СЊ СЃР°РјСѓ РїР°РїРєСѓ)
                if (requestedResource.exists()
                    && requestedResource.isReadable()
                    && resourcePath.contains(".")) {
                  return requestedResource;
                }

                // Р’СЃРµ Р·Р°РїСЂРѕСЃС‹ Р±РµР· С‚РѕС‡РєРё (СЂРѕСѓС‚С‹, РїР°РїРєРё) РѕС‚РґР°СЋС‚
                // React SPA index.html
                return new ClassPathResource("/static/ui/index.html");
              }
            });
  }
}
