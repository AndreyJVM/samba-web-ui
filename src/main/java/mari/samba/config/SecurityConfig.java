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
                    // Разрешаем доступ к статике и странице логина
                    .requestMatchers("/", "/connect", "/css/**", "/js/**", "/images/**", "/error")
                    .permitAll()
                    // Все остальные запросы должны быть аутентифицированы
                    .anyRequest()
                    .authenticated())
        // Мы отключаем дефолтную страницу логина Spring Security,
        // потому что у нас используется своя форма на главной (/)
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        // Применяем настройки logout (можно потом привязать к /disconnect)
        .logout(
            logout ->
                logout
                    .logoutUrl("/disconnect")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"));

    // Примечание: CSRF включен по умолчанию, Thymeleaf сам будет вставлять
    // <input type="hidden" name="_csrf" value="..."> во все POST-формы

    return http.build();
  }
}
