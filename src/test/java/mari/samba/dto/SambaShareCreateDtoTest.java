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

@DisplayName("Тесты DTO для создания Samba-шары")
class SambaShareCreateDtoTest {

    private Validator validator;
    private SambaShareCreateDto dto;

    @BeforeEach
    void setUp() {
        // Инициализируем валидатор
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        // Создаём свежий DTO перед каждым тестом
        dto = new SambaShareCreateDto();
        dto.setName("testshare");
        dto.setPath("/srv/samba/test");
    }

    // =============================================
    // ТЕСТЫ ЗНАЧЕНИЙ ПО УМОЛЧАНИЮ
    // =============================================

    @Test
    @DisplayName("Должны быть установлены значения по умолчанию для булевых полей")
    void testDefaultValues() {
        assertThat(dto)
                .hasFieldOrPropertyWithValue("readOnly", true)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", true);
    }

    @Test
    @DisplayName("Строковые поля должны быть null по умолчанию (кроме name и path)")
    void testStringFieldsDefaultNull() {
        assertThat(dto.getComment()).isNull();
        assertThat(dto.getValidUsers()).isNull();
        assertThat(dto.getWriteList()).isNull();
        assertThat(dto.getCreateMask()).isNull();
        assertThat(dto.getDirectoryMask()).isNull();
        assertThat(dto.getForceUser()).isNull();
        assertThat(dto.getForceGroup()).isNull();
        assertThat(dto.getMaxConnections()).isNull();
        assertThat(dto.getHostsAllow()).isNull();
        assertThat(dto.getHostsDeny()).isNull();
    }

    // =============================================
    // ТЕСТЫ ВАЛИДАЦИИ: name
    // =============================================

    @Test
    @DisplayName("Валидное имя должно проходить валидацию")
    void testValidName() {
        // Проверяем разные валидные имена
        assertThat(validateName("share1")).isEmpty();
        assertThat(validateName("my_share")).isEmpty();
        assertThat(validateName("test-share")).isEmpty();
        assertThat(validateName("SambaShare123")).isEmpty();
        assertThat(validateName("a")).isEmpty(); // минимальная длина
    }

    @ParameterizedTest
    @DisplayName("Некорректное имя должно вызывать ошибку валидации")
    @ValueSource(strings = {
            "share name",  // пробел
            "share@name",  // спецсимвол
            "тест",        // кириллица
            "share.name",  // точка
            "share#name",  // решётка
            "name?test"    // вопросительный знак
    })
    void testInvalidName(String invalidName) {
        dto.setName(invalidName);

        Set<ConstraintViolation<SambaShareCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"))
                .anyMatch(v -> v.getMessage().contains("только буквы, цифры, подчеркивания и дефисы"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустое или null имя должно вызывать ошибку @NotBlank")
    void testNameRequired(String invalidName) {
        dto.setName(invalidName);

        Set<ConstraintViolation<SambaShareCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"))
                .anyMatch(v -> v.getMessage().contains("Имя шары обязательно"));
    }

    // =============================================
    // ТЕСТЫ ВАЛИДАЦИИ: path
    // =============================================

    @Test
    @DisplayName("Валидный путь должен проходить валидацию")
    void testValidPath() {
        dto.setPath("/srv/samba/test");
        assertThat(validator.validate(dto)).isEmpty();

        dto.setPath("/opt/data");
        assertThat(validator.validate(dto)).isEmpty();

        dto.setPath("/home/user/share");
        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Пустой или null путь должен вызывать ошибку @NotBlank")
    void testPathRequired(String invalidPath) {
        dto.setPath(invalidPath);

        Set<ConstraintViolation<SambaShareCreateDto>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("path"))
                .anyMatch(v -> v.getMessage().contains("Путь к папке обязателен"));
    }

    // =============================================
    // ТЕСТЫ ГЕТТЕРОВ И СЕТТЕРОВ
    // =============================================

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать все поля")
    void testAllSetters() {
        // Arrange
        dto.setComment("Test comment");
        dto.setReadOnly(false);
        dto.setGuestOk(true);
        dto.setBrowseable(false);
        dto.setValidUsers("user1,user2");
        dto.setWriteList("admin");
        dto.setCreateMask("0644");
        dto.setDirectoryMask("0755");
        dto.setForceUser("nobody");
        dto.setForceGroup("nogroup");
        dto.setMaxConnections("10");
        dto.setHostsAllow("192.168.1.0/24");
        dto.setHostsDeny("0.0.0.0/0");

        // Assert
        assertThat(dto)
                .hasFieldOrPropertyWithValue("comment", "Test comment")
                .hasFieldOrPropertyWithValue("readOnly", false)
                .hasFieldOrPropertyWithValue("guestOk", true)
                .hasFieldOrPropertyWithValue("browseable", false)
                .hasFieldOrPropertyWithValue("validUsers", "user1,user2")
                .hasFieldOrPropertyWithValue("writeList", "admin")
                .hasFieldOrPropertyWithValue("createMask", "0644")
                .hasFieldOrPropertyWithValue("directoryMask", "0755")
                .hasFieldOrPropertyWithValue("forceUser", "nobody")
                .hasFieldOrPropertyWithValue("forceGroup", "nogroup")
                .hasFieldOrPropertyWithValue("maxConnections", "10")
                .hasFieldOrPropertyWithValue("hostsAllow", "192.168.1.0/24")
                .hasFieldOrPropertyWithValue("hostsDeny", "0.0.0.0/0");
    }

    // =============================================
    // ТЕСТЫ КОМПЛЕКСНОЙ ВАЛИДАЦИИ
    // =============================================

    @Test
    @DisplayName("Полностью валидный DTO не должен иметь ошибок валидации")
    void testFullyValidDto() {
        // Arrange
        dto.setName("documents");
        dto.setPath("/srv/documents");
        dto.setComment("Общая папка");
        dto.setReadOnly(false);
        dto.setGuestOk(true);
        dto.setValidUsers("user1,user2");
        dto.setCreateMask("0644");
        dto.setDirectoryMask("0755");

        // Act
        Set<ConstraintViolation<SambaShareCreateDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Несколько ошибок валидации должны возвращаться все сразу")
    void testMultipleValidationErrors() {
        // Arrange - имя пустое, путь пустой
        dto.setName("");
        dto.setPath("");

        // Act
        Set<ConstraintViolation<SambaShareCreateDto>> violations = validator.validate(dto);

        // Assert - ожидаем 3 ошибки (1 на path, 2 на name)
        assertThat(violations)
                .hasSize(3)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Путь к папке обязателен",
                        "Имя шары обязательно",
                        "Имя может содержать только буквы, цифры, подчеркивания и дефисы"
                );
    }

    // =============================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // =============================================

    /**
     * Проверяет только поле name и возвращает список нарушений
     */
    private Set<ConstraintViolation<SambaShareCreateDto>> validateName(String name) {
        dto.setName(name);
        return validator.validate(dto);
    }
}