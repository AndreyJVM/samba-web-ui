package mari.samba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    // Доступ открыт к SPA Приложению, ассетам и странице логина
                    .requestMatchers(
                        "/",
                        "/ui/**", // SPA Routes (React)
                        "/assets/**", // Vite static assets
                        "/index.html",
                        "/favicon.ico",
                        "/api/auth/login", // REST API Логина
                        "/error")
                    .permitAll()
                    // Все остальные пути (включая /api/**) только для авторизованных
                    .anyRequest()
                    .authenticated())
        // Отключаем классические способы логина Spring Security
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        // Включаем CSRF защитy (через Cookie) для предотвращения атак на JSESSIONID
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/api/auth/login", "/api/auth/logout"))
        // Настройка обработки ошибок (401)
        .exceptionHandling(
            exceptions ->
                exceptions
                    // Выдаем JSON 401 для всех не авторизованных путей
                    .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/**")))
        // Для iframe и встраивания (если требуется)
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    return http.build();
  }
}
