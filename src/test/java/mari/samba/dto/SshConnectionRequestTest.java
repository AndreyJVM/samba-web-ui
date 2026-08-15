package mari.samba.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты DTO для SSH-подключения")
class SshConnectionRequestTest {

    private Validator validator;
    private SshConnectionRequest request;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        request = new SshConnectionRequest();
        request.setHost("192.168.1.100");
        request.setUsername("admin");
        request.setPassword("secret123");
    }

    // =============================================
    // ТЕСТЫ ЗНАЧЕНИЙ ПО УМОЛЧАНИЮ
    // =============================================

    @Test
    @DisplayName("privateKey должен быть null по умолчанию")
    void testPrivateKeyDefaultNull() {
        assertThat(request.getPrivateKey()).isNull();
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: host (IP-адреса)
    // =============================================

    @Test
    @DisplayName("Валидные IPv4 адреса должны проходить валидацию")
    void testValidIPv4Addresses() {
        assertThat(validateHost("192.168.1.1")).isEmpty();
        assertThat(validateHost("10.0.0.1")).isEmpty();
        assertThat(validateHost("172.16.0.1")).isEmpty();
        assertThat(validateHost("0.0.0.0")).isEmpty();
        assertThat(validateHost("255.255.255.255")).isEmpty();
    }

    @Test
    @DisplayName("Валидные доменные имена должны проходить валидацию")
    void testValidDomainNames() {
        assertThat(validateHost("server.example.com")).isEmpty();
        assertThat(validateHost("my-server.local")).isEmpty();
        assertThat(validateHost("sub.domain.example.com")).isEmpty();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: host (IP-адреса)
    // =============================================

    @ParameterizedTest
    @DisplayName("Некорректные IPv4 адреса должны вызывать ошибку")
    @ValueSource(strings = {
            "256.1.1.1",      // больше 255
            "192.168.1",      // неполный
            "192.168.1.1.1",  // слишком много октетов
            "192.168.1.256",  // больше 255
            "192.168.1.",     // заканчивается точкой
            ".192.168.1.1"    // начинается с точки
    })
    void testInvalidIPAddresses(String invalidHost) {
        request.setHost(invalidHost);
        request.setUsername("admin");
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("host"))
                .anyMatch(v -> v.getMessage().contains("корректный IP адрес или домен"));
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: host (доменные имена)
    // =============================================

    @ParameterizedTest
    @DisplayName("Некорректные доменные имена должны вызывать ошибку")
    @ValueSource(strings = {
            "domain@test",    // спецсимвол
            "domain name",    // пробел
            "домен.рф",       // кириллица
            "domain..com",    // двойная точка (теперь не проходит!)
            "-domain",        // начинается с дефиса (теперь не проходит!)
            "domain-",        // заканчивается дефисом (теперь не проходит!)
            ".domain",        // начинается с точки (теперь не проходит!)
            "domain."         // заканчивается точкой (теперь не проходит!)
    })
    void testInvalidDomainNames(String invalidHost) {
        request.setHost(invalidHost);
        request.setUsername("admin");
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("host"))
                .anyMatch(v -> v.getMessage().contains("корректный IP адрес или домен"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустой или null хост должен вызывать ошибку @NotBlank")
    void testHostRequired(String invalidHost) {
        request.setHost(invalidHost);
        request.setUsername("admin");
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("host"))
                .anyMatch(v -> v.getMessage().contains("IP адрес обязателен"));
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: username
    // =============================================

    @Test
    @DisplayName("Валидные имена пользователей должны проходить валидацию")
    void testValidUsernames() {
        request.setHost("192.168.1.1");
        request.setPassword("secret123");

        request.setUsername("admin");
        assertThat(validator.validate(request)).isEmpty();

        request.setUsername("root");
        assertThat(validator.validate(request)).isEmpty();

        request.setUsername("john_doe");
        assertThat(validator.validate(request)).isEmpty();

        request.setUsername("user123");
        assertThat(validator.validate(request)).isEmpty();

        request.setUsername("john.doe");
        assertThat(validator.validate(request)).isEmpty();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: username
    // =============================================

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустой или null username должен вызывать ошибку @NotBlank")
    void testUsernameRequired(String invalidUsername) {
        request.setHost("192.168.1.1");
        request.setUsername(invalidUsername);
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("Имя пользователя обязательно"));
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: password
    // =============================================

    @Test
    @DisplayName("Валидные пароли должны проходить валидацию")
    void testValidPasswords() {
        request.setHost("192.168.1.1");
        request.setUsername("admin");

        request.setPassword("secret123");
        assertThat(validator.validate(request)).isEmpty();

        request.setPassword("my_strong_password");
        assertThat(validator.validate(request)).isEmpty();

        request.setPassword("password123!@#");
        assertThat(validator.validate(request)).isEmpty();

        request.setPassword("p");
        assertThat(validator.validate(request)).isEmpty();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: password
    // =============================================

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустой или null пароль должен вызывать ошибку @NotBlank")
    void testPasswordRequired(String invalidPassword) {
        request.setHost("192.168.1.1");
        request.setUsername("admin");
        request.setPassword(invalidPassword);

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"))
                .anyMatch(v -> v.getMessage().contains("Пароль обязателен"));
    }

    // =============================================
    // ТЕСТЫ ГЕТТЕРОВ И СЕТТЕРОВ
    // =============================================

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать все поля")
    void testAllSetters() {
        request.setHost("10.0.0.1");
        request.setUsername("deploy");
        request.setPassword("secure_pass");
        request.setPrivateKey("ssh-rsa AAAAB3NzaC1yc2EAAA...");

        assertThat(request)
                .hasFieldOrPropertyWithValue("host", "10.0.0.1")
                .hasFieldOrPropertyWithValue("username", "deploy")
                .hasFieldOrPropertyWithValue("password", "secure_pass")
                .hasFieldOrPropertyWithValue("privateKey", "ssh-rsa AAAAB3NzaC1yc2EAAA...");
    }

    @Test
    @DisplayName("privateKey может быть null или пустым (опциональное поле)")
    void testPrivateKeyOptional() {
        request.setPrivateKey(null);
        assertThat(request.getPrivateKey()).isNull();

        request.setHost("192.168.1.1");
        request.setUsername("admin");
        request.setPassword("secret123");
        request.setPrivateKey(null);
        assertThat(validator.validate(request)).isEmpty();

        request.setPrivateKey("ssh-rsa key...");
        assertThat(request.getPrivateKey()).isEqualTo("ssh-rsa key...");
        assertThat(validator.validate(request)).isEmpty();
    }

    // =============================================
    // ТЕСТЫ КОМПЛЕКСНОЙ ВАЛИДАЦИИ
    // =============================================

    @Test
    @DisplayName("Полностью валидный DTO не должен иметь ошибок валидации")
    void testFullyValidDto() {
        request.setHost("server.example.com");
        request.setUsername("deploy_user");
        request.setPassword("strong_password_123");
        request.setPrivateKey("ssh-rsa AAAAB3NzaC1yc2EAAA...");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Невалидный host и пустые username/password должны возвращать 3 ошибки")
    void testInvalidHostAndEmptyCredentials() {
        request.setHost("invalid host");
        request.setUsername("");
        request.setPassword("");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(3);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Введите корректный IP адрес или домен",
                        "Имя пользователя обязательно",
                        "Пароль обязателен"
                );
    }

    @Test
    @DisplayName("Пустой host и пустые username/password должны возвращать 4 ошибки")
    void testEmptyHostAndEmptyCredentials() {
        request.setHost("");
        request.setUsername("");
        request.setPassword("");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(4);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "IP адрес обязателен",
                        "Введите корректный IP адрес или домен",
                        "Имя пользователя обязательно",
                        "Пароль обязателен"
                );
    }

    @Test
    @DisplayName("Некорректный хост вызывает только ошибку хоста, остальное валидно")
    void testOnlyHostInvalid() {
        request.setHost("invalid@@host");
        request.setUsername("admin");
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("host");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Введите корректный IP адрес или домен");
    }

    @Test
    @DisplayName("Пустой хост и пустой username должны возвращать соответствующие ошибки")
    void testHostAndUsernameEmpty() {
        request.setHost("");
        request.setUsername("");
        request.setPassword("secret123");

        Set<ConstraintViolation<SshConnectionRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(3);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "IP адрес обязателен",
                        "Введите корректный IP адрес или домен",
                        "Имя пользователя обязательно"
                );
    }

    // =============================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // =============================================

    private Set<ConstraintViolation<SshConnectionRequest>> validateHost(String host) {
        request.setHost(host);
        request.setUsername("admin");
        request.setPassword("secret123");
        return validator.validate(request);
    }
}