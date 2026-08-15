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

@DisplayName("Тесты DTO для создания пользователя Samba")
class SambaUserCreateDtoTest {

    private Validator validator;
    private SambaUserCreateDto dto;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        dto = new SambaUserCreateDto();
        dto.setUsername("john_doe");
        dto.setPassword("secret123");
    }

    // =============================================
    // ТЕСТЫ ЗНАЧЕНИЙ ПО УМОЛЧАНИЮ
    // =============================================

    @Test
    @DisplayName("fullName должен быть null по умолчанию")
    void testFullNameDefaultNull() {
        assertThat(dto.getFullName()).isNull();
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: username
    // =============================================

    @Test
    @DisplayName("Валидное имя пользователя должно проходить валидацию")
    void testValidUsername() {
        assertThat(validateUsername("john")).isEmpty();
        assertThat(validateUsername("john_doe")).isEmpty();
        assertThat(validateUsername("john123")).isEmpty();
        assertThat(validateUsername("JOHN")).isEmpty();
        assertThat(validateUsername("user_name")).isEmpty();
    }

    @Test
    @DisplayName("Имя длиной ровно 3 символа должно проходить валидацию (граница минимума)")
    void testUsernameMinimumLength() {
        assertThat(validateUsername("abc")).isEmpty();
    }

    @Test
    @DisplayName("Имя длиной ровно 20 символов должно проходить валидацию (граница максимума)")
    void testUsernameMaximumLength() {
        assertThat(validateUsername("abcdefghijklmnopqrst")).isEmpty();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: username
    // =============================================

    @ParameterizedTest
    @DisplayName("Имя короче 3 символов должно вызывать ошибку")
    @ValueSource(strings = {"ab", "a"})
    void testUsernameTooShort(String shortUsername) {
        dto.setUsername(shortUsername);
        dto.setPassword("secret123");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("от 3 до 20 символов"));
    }

    @Test
    @DisplayName("Имя длиной 2 символа должно вызывать ошибку (граница минимума - 1)")
    void testUsernameJustBelowMinimum() {
        dto.setUsername("ab");
        dto.setPassword("secret123");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("от 3 до 20 символов"));
    }

    @ParameterizedTest
    @DisplayName("Имя длиннее 20 символов должно вызывать ошибку")
    @ValueSource(strings = {
            "abcdefghijklmnopqrstu", // 21 символ
            "this_is_a_very_long_username" // 27 символов
    })
    void testUsernameTooLong(String longUsername) {
        dto.setUsername(longUsername);
        dto.setPassword("secret123");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("от 3 до 20 символов"));
    }

    @Test
    @DisplayName("Имя длиной 21 символ должно вызывать ошибку (граница максимума + 1)")
    void testUsernameJustAboveMaximum() {
        dto.setUsername("abcdefghijklmnopqrstu"); // 21 символ
        dto.setPassword("secret123");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("от 3 до 20 символов"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустое или null имя пользователя должно вызывать ошибку @NotBlank")
    void testUsernameRequired(String invalidUsername) {
        dto.setUsername(invalidUsername);
        dto.setPassword("secret123");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getMessage().contains("Имя пользователя обязательно"));
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: password
    // =============================================

    @Test
    @DisplayName("Валидный пароль должен проходить валидацию")
    void testValidPassword() {
        dto.setPassword("123456");
        assertThat(validator.validate(dto)).isEmpty();

        dto.setPassword("secret");
        assertThat(validator.validate(dto)).isEmpty();

        dto.setPassword("my_strong_password_123");
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("Пароль длиной ровно 6 символов должен проходить валидацию (граница минимума)")
    void testPasswordMinimumLength() {
        dto.setUsername("john");
        dto.setPassword("123456");
        assertThat(validator.validate(dto)).isEmpty();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: password
    // =============================================

    @Test
    @DisplayName("Пароль короче 6 символов должен вызывать ошибку")
    void testPasswordTooShort() {
        dto.setUsername("john");
        dto.setPassword("12345");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"))
                .anyMatch(v -> v.getMessage().contains("не менее 6 символов"));
    }

    @Test
    @DisplayName("Пароль длиной 5 символов должен вызывать ошибку (граница минимума - 1)")
    void testPasswordJustBelowMinimum() {
        dto.setUsername("john");
        dto.setPassword("12345");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"))
                .anyMatch(v -> v.getMessage().contains("не менее 6 символов"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустой или null пароль должен вызывать ошибку @NotBlank")
    void testPasswordRequired(String invalidPassword) {
        dto.setUsername("john");
        dto.setPassword(invalidPassword);

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

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
        dto.setUsername("admin");
        dto.setPassword("secure_pass");
        dto.setFullName("John Doe");

        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "admin")
                .hasFieldOrPropertyWithValue("password", "secure_pass")
                .hasFieldOrPropertyWithValue("fullName", "John Doe");
    }

    @Test
    @DisplayName("Должна быть возможность установить null для fullName")
    void testFullNameCanBeNull() {
        dto.setFullName(null);
        assertThat(dto.getFullName()).isNull();

        dto.setFullName("John Doe");
        assertThat(dto.getFullName()).isEqualTo("John Doe");

        dto.setFullName(null);
        assertThat(dto.getFullName()).isNull();
    }

    // =============================================
    // ТЕСТЫ КОМПЛЕКСНОЙ ВАЛИДАЦИИ
    // =============================================

    @Test
    @DisplayName("Полностью валидный DTO не должен иметь ошибок валидации")
    void testFullyValidDto() {
        dto.setUsername("john_doe");
        dto.setPassword("secret123");
        dto.setFullName("John Doe");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Несколько ошибок валидации должны возвращаться все сразу")
    void testMultipleValidationErrors() {
        dto.setUsername("ab"); // меньше 3 символов
        dto.setPassword("12345"); // меньше 6 символов

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(2);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Имя должно быть от 3 до 20 символов",
                        "Пароль должен быть не менее 6 символов"
                );

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("username", "password");
    }

    @Test
    @DisplayName("Пустой username и пустой password должны возвращать все ошибки")
    void testEmptyFieldsMultipleErrors() {
        dto.setUsername("");
        dto.setPassword("");

        Set<ConstraintViolation<SambaUserCreateDto>> violations = validator.validate(dto);

        // Ожидаем 4 ошибки:
        // - username: @NotBlank + @Size
        // - password: @NotBlank + @Size
        assertThat(violations).hasSize(4);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Имя пользователя обязательно",
                        "Имя должно быть от 3 до 20 символов",
                        "Пароль обязателен",
                        "Пароль должен быть не менее 6 символов"
                );
    }

    // =============================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // =============================================

    /**
     * Проверяет только поле username и возвращает список нарушений
     */
    private Set<ConstraintViolation<SambaUserCreateDto>> validateUsername(String username) {
        dto.setUsername(username);
        dto.setPassword("valid_password123");
        return validator.validate(dto);
    }
}