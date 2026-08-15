package mari.samba.dto;

import mari.samba.model.SambaShare;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class SambaShareTest {

    @Test
    @DisplayName("Конструктор с name и path должен корректно инициализировать поля")
    void testConstructorWithNameAndPath() {
        // Arrange & Act
        SambaShare share = new SambaShare("public", "/srv/samba/public");

        // Assert using AssertJ
        assertThat(share)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "public")
                .hasFieldOrPropertyWithValue("path", "/srv/samba/public")
                .hasFieldOrPropertyWithValue("readOnly", true)
                .hasFieldOrPropertyWithValue("guestOk", false)
                .hasFieldOrPropertyWithValue("browseable", true);
    }

    @Test
    @DisplayName("Сеттеры должны корректно устанавливать значения")
    void testSetters() {
        // Arrange
        SambaShare share = new SambaShare();

        // Act
        share.setName("test");
        share.setPath("/test");
        share.setComment("Test comment");
        share.setReadOnly(false);
        share.setGuestOk(true);
        share.setBrowseable(false);

        // Assert
        assertThat(share.getName()).isEqualTo("test");
        assertThat(share.getPath()).isEqualTo("/test");
        assertThat(share.getComment()).isEqualTo("Test comment");
        assertThat(share.isReadOnly()).isFalse();
        assertThat(share.isGuestOk()).isTrue();
        assertThat(share.isBrowseable()).isFalse();
    }
}
