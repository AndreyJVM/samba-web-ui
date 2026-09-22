package mari.samba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    // Разрешаем доступ к статике, странице логина и swagger api-docs
                    .requestMatchers(
                        "/",
                        "/connect",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**")
                    .permitAll()
                    // Все остальные запросы должны быть аутентифицированы
                    .anyRequest()
                    .authenticated())
        // Мы отключаем дефолтную страницу логина Spring Security,
        // потому что у нас используется своя форма на главной (/)
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        // Временно отключаем CSRF для API (на следующих этапах перейдем на Stateless/JWT)
        .csrf(csrf -> csrf.disable())
        // Применяем настройки logout (можно потом привязать к /disconnect)
        .logout(
            logout ->
                logout
                    .logoutUrl("/disconnect")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"));

    return http.build();
  }
}
