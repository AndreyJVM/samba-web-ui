package mari.samba.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты модели SambaUser")
class SambaUserTest {

    private SambaUser user;

    @BeforeEach
    void setUp() {
        user = new SambaUser();
    }

    // =============================================
    // ТЕСТЫ КОНСТРУКТОРОВ
    // =============================================

    @Test
    @DisplayName("Конструктор без параметров должен создавать пустой объект")
    void testDefaultConstructor() {
        SambaUser emptyUser = new SambaUser();

        assertThat(emptyUser)
                .isNotNull()
                .hasAllNullFieldsOrPropertiesExcept("accountEnabled");

        // Булево поле по умолчанию false
        assertThat(emptyUser.isAccountEnabled()).isFalse();
    }

    @Test
    @DisplayName("Конструктор с username должен корректно инициализировать поля")
    void testConstructorWithUsername() {
        // Arrange
        String expectedUsername = "john_doe";

        // Act
        SambaUser user = new SambaUser(expectedUsername);

        // Assert
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", expectedUsername)
                .hasFieldOrPropertyWithValue("accountEnabled", true);

        // Остальные поля должны быть null
        assertThat(user.getFullName()).isNull();
        assertThat(user.getLastChange()).isNull();
        assertThat(user.getPasswordHash()).isNull();
    }

    @Test
    @DisplayName("Конструктор с username должен устанавливать accountEnabled = true")
    void testConstructorSetsAccountEnabledTrue() {
        // Act
        SambaUser user = new SambaUser("admin");

        // Assert
        assertThat(user.isAccountEnabled()).isTrue();
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: ГЕТТЕРЫ И СЕТТЕРЫ
    // =============================================

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать все строковые поля")
    void testAllStringSetters() {
        // Arrange
        String username = "john_doe";
        String fullName = "John Doe";
        String lastChange = "2024-01-15";
        String passwordHash = "$2y$10$abcdefghijklmnopqrstuvwxyz123456";

        // Act
        user.setUsername(username);
        user.setFullName(fullName);
        user.setLastChange(lastChange);
        user.setPasswordHash(passwordHash);

        // Assert
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", username)
                .hasFieldOrPropertyWithValue("fullName", fullName)
                .hasFieldOrPropertyWithValue("lastChange", lastChange)
                .hasFieldOrPropertyWithValue("passwordHash", passwordHash);
    }

    @Test
    @DisplayName("Сеттер должен корректно устанавливать булево поле accountEnabled")
    void testAccountEnabledSetter() {
        // Act & Assert - проверяем оба состояния
        user.setAccountEnabled(true);
        assertThat(user.isAccountEnabled()).isTrue();

        user.setAccountEnabled(false);
        assertThat(user.isAccountEnabled()).isFalse();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: ГЕТТЕРЫ И СЕТТЕРЫ
    // =============================================

    @Test
    @DisplayName("Должна быть возможность установить null для всех строковых полей")
    void testCanSetNullForAllStringFields() {
        // Arrange
        user.setUsername(null);
        user.setFullName(null);
        user.setLastChange(null);
        user.setPasswordHash(null);

        // Assert
        assertThat(user.getUsername()).isNull();
        assertThat(user.getFullName()).isNull();
        assertThat(user.getLastChange()).isNull();
        assertThat(user.getPasswordHash()).isNull();
    }

    @Test
    @DisplayName("Должна быть возможность установить пустые строки для всех полей")
    void testCanSetEmptyStrings() {
        // Arrange
        user.setUsername("");
        user.setFullName("");
        user.setLastChange("");
        user.setPasswordHash("");

        // Assert
        assertThat(user.getUsername()).isEmpty();
        assertThat(user.getFullName()).isEmpty();
        assertThat(user.getLastChange()).isEmpty();
        assertThat(user.getPasswordHash()).isEmpty();
    }

    // =============================================
    // ТЕСТЫ ДЛЯ СПЕЦИФИЧНЫХ ПОЛЕЙ
    // =============================================

    @ParameterizedTest
    @DisplayName("Имя пользователя может содержать различные символы (без валидации)")
    @ValueSource(strings = {
            "john",
            "john_doe",
            "john.doe",
            "john-doe",
            "john123",
            "JOHN",
            "John_Doe",
            "user@domain",    // хоть и странно, но модель это допускает
            "русское_имя"
    })
    void testUsernameCanBeAnyString(String username) {
        // Act
        user.setUsername(username);

        // Assert
        assertThat(user.getUsername()).isEqualTo(username);
    }

    @ParameterizedTest
    @DisplayName("Полное имя может содержать различные символы")
    @ValueSource(strings = {
            "John Doe",
            "Иван Петров",
            "J. Doe",
            "Dr. John Doe Jr.",
            "John-Doe",
            "John Doe (admin)"
    })
    void testFullNameCanBeAnyString(String fullName) {
        // Act
        user.setFullName(fullName);

        // Assert
        assertThat(user.getFullName()).isEqualTo(fullName);
    }

    @Test
    @DisplayName("Дата изменения должна корректно устанавливаться в формате строки")
    void testLastChangeAsString() {
        // Arrange
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Act
        user.setLastChange(today);

        // Assert
        assertThat(user.getLastChange()).isEqualTo(today);

        // Act
        user.setLastChange(yesterday);

        // Assert
        assertThat(user.getLastChange()).isEqualTo(yesterday);
    }

    @Test
    @DisplayName("Хеш пароля может быть любой строкой")
    void testPasswordHash() {
        // Arrange
        String bcryptHash = "$2y$10$abcdefghijklmnopqrstuvwxyz123456";
        String shaHash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        String plainText = "password"; // модель допускает

        // Act & Assert
        user.setPasswordHash(bcryptHash);
        assertThat(user.getPasswordHash()).isEqualTo(bcryptHash);

        user.setPasswordHash(shaHash);
        assertThat(user.getPasswordHash()).isEqualTo(shaHash);

        user.setPasswordHash(plainText);
        assertThat(user.getPasswordHash()).isEqualTo(plainText);
    }

    // =============================================
    // ТЕСТЫ КОМПЛЕКСНЫХ СЦЕНАРИЕВ
    // =============================================

    @Test
    @DisplayName("Полный объект SambaUser должен корректно хранить все данные")
    void testFullyPopulatedUser() {
        // Arrange
        user.setUsername("admin");
        user.setFullName("Administrator");
        user.setAccountEnabled(true);
        user.setLastChange("2024-01-15");
        user.setPasswordHash("$2y$10$secureHashHere");

        // Assert
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "admin")
                .hasFieldOrPropertyWithValue("fullName", "Administrator")
                .hasFieldOrPropertyWithValue("accountEnabled", true)
                .hasFieldOrPropertyWithValue("lastChange", "2024-01-15")
                .hasFieldOrPropertyWithValue("passwordHash", "$2y$10$secureHashHere");
    }

    @Test
    @DisplayName("Активный пользователь должен иметь accountEnabled = true")
    void testActiveUser() {
        // Arrange
        user.setUsername("john");
        user.setAccountEnabled(true);
        user.setFullName("John Doe");
        user.setLastChange(LocalDate.now().toString());

        // Assert
        assertThat(user)
                .hasFieldOrPropertyWithValue("accountEnabled", true);
    }

    @Test
    @DisplayName("Отключённый пользователь должен иметь accountEnabled = false")
    void testDisabledUser() {
        // Arrange
        user.setUsername("deleted_user");
        user.setAccountEnabled(false);
        user.setFullName("Deleted User");

        // Assert
        assertThat(user.isAccountEnabled()).isFalse();
    }

    @Test
    @DisplayName("Пользователь должен корректно хранить дату последнего изменения пароля")
    void testPasswordLastChange() {
        // Arrange
        String oldDate = "2023-12-01";
        String newDate = "2024-01-15";

        // Act
        user.setLastChange(oldDate);
        assertThat(user.getLastChange()).isEqualTo(oldDate);

        // Act - обновляем дату
        user.setLastChange(newDate);
        assertThat(user.getLastChange()).isEqualTo(newDate);
    }

    @Test
    @DisplayName("Пользователь с минимальными данными (только username)")
    void testMinimalUser() {
        // Act
        SambaUser minimalUser = new SambaUser("guest");

        // Assert
        assertThat(minimalUser)
                .hasFieldOrPropertyWithValue("username", "guest")
                .hasFieldOrPropertyWithValue("accountEnabled", true);
        assertThat(minimalUser.getFullName()).isNull();
        assertThat(minimalUser.getLastChange()).isNull();
        assertThat(minimalUser.getPasswordHash()).isNull();
    }

    // =============================================
    // ТЕСТЫ НА МУТАБЕЛЬНОСТЬ (изменяемость)
    // =============================================

    @Test
    @DisplayName("Изменение одного поля не должно влиять на другие поля")
    void testFieldIndependence() {
        // Arrange
        user.setUsername("john");
        user.setFullName("John Doe");
        user.setAccountEnabled(true);
        user.setLastChange("2024-01-01");
        user.setPasswordHash("hash1");

        // Act - меняем только имя
        user.setUsername("john_doe");

        // Assert - остальные поля не изменились
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "john_doe")
                .hasFieldOrPropertyWithValue("fullName", "John Doe")
                .hasFieldOrPropertyWithValue("accountEnabled", true)
                .hasFieldOrPropertyWithValue("lastChange", "2024-01-01")
                .hasFieldOrPropertyWithValue("passwordHash", "hash1");
    }

    @Test
    @DisplayName("Объект должен сохранять состояние между изменениями")
    void testStatePersistence() {
        // Act
        user.setUsername("user1");
        user.setFullName("User One");

        // Assert
        assertThat(user.getUsername()).isEqualTo("user1");
        assertThat(user.getFullName()).isEqualTo("User One");

        // Act - изменяем
        user.setUsername("user2");

        // Assert - только имя изменилось
        assertThat(user.getUsername()).isEqualTo("user2");
        assertThat(user.getFullName()).isEqualTo("User One");
    }

    // =============================================
    // ТЕСТЫ ДЛЯ СЦЕНАРИЕВ ИСПОЛЬЗОВАНИЯ
    // =============================================

    @Test
    @DisplayName("Пользователь должен корректно создаваться через конструктор с username")
    void testFactoryMethodStyle() {
        // Act - симулируем создание пользователя через конструктор
        SambaUser newUser = new SambaUser("new_employee");
        newUser.setFullName("New Employee");
        newUser.setAccountEnabled(true);
        newUser.setLastChange(LocalDate.now().toString());

        // Assert
        assertThat(newUser)
                .hasFieldOrPropertyWithValue("username", "new_employee")
                .hasFieldOrPropertyWithValue("fullName", "New Employee")
                .hasFieldOrPropertyWithValue("accountEnabled", true);
    }

    @Test
    @DisplayName("Пароль пользователя должен обновляться через сеттер")
    void testPasswordUpdate() {
        // Arrange
        String oldHash = "old_hash_123";
        String newHash = "new_hash_456";

        // Act
        user.setPasswordHash(oldHash);
        assertThat(user.getPasswordHash()).isEqualTo(oldHash);

        // Act - обновляем пароль
        user.setPasswordHash(newHash);
        assertThat(user.getPasswordHash()).isEqualTo(newHash);
    }

    @Test
    @DisplayName("Учётная запись пользователя может быть включена/отключена")
    void testAccountEnableDisable() {
        // Arrange
        user.setAccountEnabled(true);
        assertThat(user.isAccountEnabled()).isTrue();

        // Act - отключаем
        user.setAccountEnabled(false);
        assertThat(user.isAccountEnabled()).isFalse();

        // Act - включаем
        user.setAccountEnabled(true);
        assertThat(user.isAccountEnabled()).isTrue();
    }
}