package mari.samba.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты модели SambaShare")
class SambaShareTest {

    private SambaShare share;

    @BeforeEach
    void setUp() {
        share = new SambaShare();
    }

    // =============================================
    // ТЕСТЫ КОНСТРУКТОРОВ
    // =============================================

    @Test
    @DisplayName("Конструктор без параметров должен создавать пустой объект")
    void testDefaultConstructor() {
        SambaShare emptyShare = new SambaShare();

        assertThat(emptyShare)
                .isNotNull()
                .hasAllNullFieldsOrPropertiesExcept(
                        "readOnly", "guestOk", "browseable"
                );

        // Булевы поля по умолчанию false
        assertThat(emptyShare.isReadOnly()).isFalse();
        assertThat(emptyShare.isGuestOk()).isFalse();
        assertThat(emptyShare.isBrowseable()).isFalse();
    }

    @Test
    @DisplayName("Конструктор с name и path должен корректно инициализировать поля")
    void testConstructorWithNameAndPath() {
        // Arrange
        String expectedName = "public";
        String expectedPath = "/srv/samba/public";

        // Act
        SambaShare share = new SambaShare(expectedName, expectedPath);

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", expectedName)
                .hasFieldOrPropertyWithValue("path", expectedPath)
                .hasFieldOrPropertyWithValue("readOnly", true)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", true);

        // Остальные поля должны быть null
        assertThat(share.getComment()).isNull();
        assertThat(share.getValidUsers()).isNull();
        assertThat(share.getWriteList()).isNull();
        assertThat(share.getCreateMask()).isNull();
        assertThat(share.getDirectoryMask()).isNull();
        assertThat(share.getForceUser()).isNull();
        assertThat(share.getForceGroup()).isNull();
        assertThat(share.getMaxConnections()).isNull();
        assertThat(share.getHostsAllow()).isNull();
        assertThat(share.getHostsDeny()).isNull();
    }

    @Test
    @DisplayName("Конструктор с name и path должен устанавливать значения по умолчанию для булевых полей")
    void testConstructorSetsDefaultBooleanValues() {
        // Act
        SambaShare share = new SambaShare("test", "/tmp/test");

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("readOnly", true)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", true);
    }

    // =============================================
    // ПОЗИТИВНЫЕ ТЕСТЫ: ГЕТТЕРЫ И СЕТТЕРЫ
    // =============================================

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать все строковые поля")
    void testAllStringSetters() {
        // Arrange
        share.setName("documents");
        share.setPath("/var/lib/samba/documents");
        share.setComment("Общая папка для документов");
        share.setValidUsers("user1,user2,user3");
        share.setWriteList("admin,manager");
        share.setCreateMask("0644");
        share.setDirectoryMask("0755");
        share.setForceUser("nobody");
        share.setForceGroup("nogroup");
        share.setMaxConnections("10");
        share.setHostsAllow("192.168.1.0/24");
        share.setHostsDeny("0.0.0.0/0");

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", "documents")
                .hasFieldOrPropertyWithValue("path", "/var/lib/samba/documents")
                .hasFieldOrPropertyWithValue("comment", "Общая папка для документов")
                .hasFieldOrPropertyWithValue("validUsers", "user1,user2,user3")
                .hasFieldOrPropertyWithValue("writeList", "admin,manager")
                .hasFieldOrPropertyWithValue("createMask", "0644")
                .hasFieldOrPropertyWithValue("directoryMask", "0755")
                .hasFieldOrPropertyWithValue("forceUser", "nobody")
                .hasFieldOrPropertyWithValue("forceGroup", "nogroup")
                .hasFieldOrPropertyWithValue("maxConnections", "10")
                .hasFieldOrPropertyWithValue("hostsAllow", "192.168.1.0/24")
                .hasFieldOrPropertyWithValue("hostsDeny", "0.0.0.0/0");
    }

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать булевы поля")
    void testAllBooleanSetters() {
        // Act & Assert - проверяем оба состояния
        share.setReadOnly(true);
        assertThat(share.isReadOnly()).isTrue();
        share.setReadOnly(false);
        assertThat(share.isReadOnly()).isFalse();

        share.setGuestOk(true);
        assertThat(share.isGuestOk()).isTrue();
        share.setGuestOk(false);
        assertThat(share.isGuestOk()).isFalse();

        share.setBrowseable(true);
        assertThat(share.isBrowseable()).isTrue();
        share.setBrowseable(false);
        assertThat(share.isBrowseable()).isFalse();
    }

    // =============================================
    // НЕГАТИВНЫЕ ТЕСТЫ: ГЕТТЕРЫ И СЕТТЕРЫ
    // =============================================

    @Test
    @DisplayName("Должна быть возможность установить null для всех строковых полей")
    void testCanSetNullForAllStringFields() {
        // Arrange
        share.setName(null);
        share.setPath(null);
        share.setComment(null);
        share.setValidUsers(null);
        share.setWriteList(null);
        share.setCreateMask(null);
        share.setDirectoryMask(null);
        share.setForceUser(null);
        share.setForceGroup(null);
        share.setMaxConnections(null);
        share.setHostsAllow(null);
        share.setHostsDeny(null);

        // Assert
        assertThat(share.getName()).isNull();
        assertThat(share.getPath()).isNull();
        assertThat(share.getComment()).isNull();
        assertThat(share.getValidUsers()).isNull();
        assertThat(share.getWriteList()).isNull();
        assertThat(share.getCreateMask()).isNull();
        assertThat(share.getDirectoryMask()).isNull();
        assertThat(share.getForceUser()).isNull();
        assertThat(share.getForceGroup()).isNull();
        assertThat(share.getMaxConnections()).isNull();
        assertThat(share.getHostsAllow()).isNull();
        assertThat(share.getHostsDeny()).isNull();
    }

    @Test
    @DisplayName("Должна быть возможность установить пустые строки для всех полей")
    void testCanSetEmptyStrings() {
        // Arrange
        share.setName("");
        share.setPath("");
        share.setComment("");
        share.setValidUsers("");
        share.setWriteList("");
        share.setCreateMask("");
        share.setDirectoryMask("");
        share.setForceUser("");
        share.setForceGroup("");
        share.setMaxConnections("");
        share.setHostsAllow("");
        share.setHostsDeny("");

        // Assert
        assertThat(share.getName()).isEmpty();
        assertThat(share.getPath()).isEmpty();
        assertThat(share.getComment()).isEmpty();
        assertThat(share.getValidUsers()).isEmpty();
        assertThat(share.getWriteList()).isEmpty();
        assertThat(share.getCreateMask()).isEmpty();
        assertThat(share.getDirectoryMask()).isEmpty();
        assertThat(share.getForceUser()).isEmpty();
        assertThat(share.getForceGroup()).isEmpty();
        assertThat(share.getMaxConnections()).isEmpty();
        assertThat(share.getHostsAllow()).isEmpty();
        assertThat(share.getHostsDeny()).isEmpty();
    }

    // =============================================
    // ТЕСТЫ ДЛЯ СПЕЦИФИЧНЫХ ПОЛЕЙ
    // =============================================

    @ParameterizedTest
    @DisplayName("Имя шары может содержать любые символы (без валидации)")
    @ValueSource(strings = {
            "share",
            "my_share",
            "test-share",
            "share name with spaces",
            "share@#$%",
            "русское_имя",
            "123456"
    })
    void testNameCanBeAnyString(String name) {
        // Act
        share.setName(name);

        // Assert
        assertThat(share.getName()).isEqualTo(name);
    }

    @ParameterizedTest
    @DisplayName("Путь может быть любым (без валидации)")
    @ValueSource(strings = {
            "/srv/samba/share",
            "C:\\samba\\share",
            "relative/path",
            "/path with spaces",
            "/path/with/special/@#$%",
            "/путь/по-русски"
    })
    void testPathCanBeAnyString(String path) {
        // Act
        share.setPath(path);

        // Assert
        assertThat(share.getPath()).isEqualTo(path);
    }

    @ParameterizedTest
    @DisplayName("Маски создания должны корректно устанавливаться")
    @ValueSource(strings = {
            "0644",
            "0755",
            "0777",
            "0660",
            "0000"
    })
    void testCreateMaskValues(String mask) {
        // Act
        share.setCreateMask(mask);
        share.setDirectoryMask(mask);

        // Assert
        assertThat(share.getCreateMask()).isEqualTo(mask);
        assertThat(share.getDirectoryMask()).isEqualTo(mask);
    }

    @ParameterizedTest
    @DisplayName("Список пользователей должен корректно устанавливаться")
    @ValueSource(strings = {
            "user1",
            "user1,user2,user3",
            "admin,root",
            "user1, user2, user3" // с пробелами
    })
    void testUserLists(String userList) {
        // Act
        share.setValidUsers(userList);
        share.setWriteList(userList);

        // Assert
        assertThat(share.getValidUsers()).isEqualTo(userList);
        assertThat(share.getWriteList()).isEqualTo(userList);
    }

    // =============================================
    // ТЕСТЫ КОМПЛЕКСНЫХ СЦЕНАРИЕВ
    // =============================================

    @Test
    @DisplayName("Полный объект SambaShare должен корректно хранить все данные")
    void testFullyPopulatedShare() {
        // Arrange
        share.setName("documents");
        share.setPath("/srv/documents");
        share.setComment("Corporate documents");
        share.setReadOnly(false);
        share.setGuestOk(false);
        share.setBrowseable(true);
        share.setValidUsers("john,mary,admin");
        share.setWriteList("admin,manager");
        share.setCreateMask("0644");
        share.setDirectoryMask("0755");
        share.setForceUser("samba");
        share.setForceGroup("samba");
        share.setMaxConnections("50");
        share.setHostsAllow("192.168.1.0/24");
        share.setHostsDeny("0.0.0.0/0");

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", "documents")
                .hasFieldOrPropertyWithValue("path", "/srv/documents")
                .hasFieldOrPropertyWithValue("comment", "Corporate documents")
                .hasFieldOrPropertyWithValue("readOnly", false)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", true)
                .hasFieldOrPropertyWithValue("validUsers", "john,mary,admin")
                .hasFieldOrPropertyWithValue("writeList", "admin,manager")
                .hasFieldOrPropertyWithValue("createMask", "0644")
                .hasFieldOrPropertyWithValue("directoryMask", "0755")
                .hasFieldOrPropertyWithValue("forceUser", "samba")
                .hasFieldOrPropertyWithValue("forceGroup", "samba")
                .hasFieldOrPropertyWithValue("maxConnections", "50")
                .hasFieldOrPropertyWithValue("hostsAllow", "192.168.1.0/24")
                .hasFieldOrPropertyWithValue("hostsDeny", "0.0.0.0/0");
    }

    @Test
    @DisplayName("Должна быть возможность создать публичную шару с гостевой доступом")
    void testPublicGuestShare() {
        // Act
        share.setName("public");
        share.setPath("/srv/public");
        share.setReadOnly(true);
        share.setGuestOk(true);
        share.setBrowseable(true);

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", "public")
                .hasFieldOrPropertyWithValue("readOnly", true)
                .hasFieldOrPropertyWithValue("guestOk", true)
                .hasFieldOrPropertyWithValue("browseable", true);
    }

    @Test
    @DisplayName("Должна быть возможность создать приватную шару с ограниченным доступом")
    void testPrivateRestrictedShare() {
        // Act
        share.setName("private");
        share.setPath("/srv/private");
        share.setReadOnly(false);
        share.setGuestOk(false);
        share.setBrowseable(false);
        share.setValidUsers("admin,manager");
        share.setHostsAllow("192.168.1.0/24");

        // Assert
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", "private")
                .hasFieldOrPropertyWithValue("readOnly", false)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", false)
                .hasFieldOrPropertyWithValue("validUsers", "admin,manager")
                .hasFieldOrPropertyWithValue("hostsAllow", "192.168.1.0/24");
    }

    // =============================================
    // ТЕСТЫ НА МУТАБЕЛЬНОСТЬ (изменяемость)
    // =============================================

    @Test
    @DisplayName("Изменение одного поля не должно влиять на другие поля")
    void testFieldIndependence() {
        // Arrange
        share.setName("original");
        share.setPath("/original/path");
        share.setReadOnly(true);

        // Act - меняем только имя
        share.setName("new_name");

        // Assert - остальные поля не изменились
        assertThat(share)
                .hasFieldOrPropertyWithValue("name", "new_name")
                .hasFieldOrPropertyWithValue("path", "/original/path")
                .hasFieldOrPropertyWithValue("readOnly", true);
    }

    @Test
    @DisplayName("Объект должен сохранять состояние между изменениями")
    void testStatePersistence() {
        // Act
        share.setName("share1");
        share.setPath("/path1");

        // Assert
        assertThat(share.getName()).isEqualTo("share1");
        assertThat(share.getPath()).isEqualTo("/path1");

        // Act - изменяем
        share.setName("share2");

        // Assert
        assertThat(share.getName()).isEqualTo("share2");
        assertThat(share.getPath()).isEqualTo("/path1"); // не изменился
    }
}