package mari.samba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    // Разрешаем доступ к SPA фронтенду, статике и эндпоинтам логина
                    .requestMatchers(
                        "/",
                        "/ui/**", // SPA Routes (React)
                        "/assets/**", // Vite static assets
                        "/index.html",
                        "/favicon.ico",
                        "/api/auth/login", // REST API логин
                        "/error")
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
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/**")))
        // Разрешаем iframe с того же источника (если потребуется)
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    return http.build();
  }
}
