package mari.samba.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import mari.samba.dto.common.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    // Разрешаем доступ к SPA фронтенду, статике и эндпоинтам логина/swagger
                    .requestMatchers(
                        "/",
                        "/ui/**", // SPA Routes (React)
                        "/assets/**", // Vite static assets
                        "/index.html",
                        "/favicon.ico",
                        "/api/auth/login", // Новый REST API логин
                        "/error",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**")
                    .permitAll()
                    // Все остальные запросы (включая /api/**) должны быть аутентифицированы
                    .anyRequest()
                    .authenticated())
        // Отключаем дефолтную страницу логина Spring Security
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        // Отключаем CSRF для REST API
        .csrf(csrf -> csrf.disable())
        // Настройка обработки ошибок авторизации (401)
        .exceptionHandling(
            exceptions ->
                exceptions
                    // Возвращаем JSON 401 для ВСЕХ неавторизованных запросов
                    .defaultAuthenticationEntryPointFor(
                    (request, response, authException) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType("application/json;charset=UTF-8");
                      ApiResponse<Void> apiResponse = ApiResponse.error("Требуется авторизация");
                      ObjectMapper mapper = new ObjectMapper();
                      response.getWriter().write(mapper.writeValueAsString(apiResponse));
                    },
                    new AntPathRequestMatcher("/**")))
        // Настройка логаута (для обратной совместимости, хотя лучше использовать POST
        // /api/auth/logout)
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout") // Переопределили на новый URL
                    .logoutSuccessHandler(
                        (request, response, authentication) -> {
                          response.setStatus(HttpServletResponse.SC_OK);
                          response.setContentType("application/json;charset=UTF-8");
                          ApiResponse<Void> apiResponse = ApiResponse.ok("Успешно отключено", null);
                          ObjectMapper mapper = new ObjectMapper();
                          response.getWriter().write(mapper.writeValueAsString(apiResponse));
                        })
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"));

    return http.build();
  }
}
