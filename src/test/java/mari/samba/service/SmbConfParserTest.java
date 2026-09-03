package mari.samba.service;

import mari.samba.dto.SambaGlobalConfigDto;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmbConfParserTest {

    private SmbConfParser parser;

    @BeforeEach
    void setUp() {
        parser = new SmbConfParser();
    }

    private final String sampleConfig = """
            # Global settings
            [global]
               workgroup = TESTGROUP
               server string = Test File Server
               security = user
               map to guest = Bad User
               interfaces = 192.168.1.0/24
               bind interfaces only = yes
               load printers = no
               disable netbios = yes
               server min protocol = SMB2
               server max protocol = SMB3
            
            [public]
               path = /srv/samba/public
               comment = Public share
               read only = no
               guest ok = yes
               browseable = yes
            
            [private]
               path = /srv/samba/private
               comment = Private files
               read only = yes
               guest ok = no
               browseable = no
               valid users = admin, @managers
            """;

    @Test
    @DisplayName("Должен корректно парсить секцию [global]")
    void shouldParseGlobalConfig() {
        SambaGlobalConfigDto global = parser.parseGlobalConfig(sampleConfig);

        assertThat(global.getWorkgroup()).isEqualTo("TESTGROUP");
        assertThat(global.getServerString()).isEqualTo("Test File Server");
        assertThat(global.getSecurity()).isEqualTo("user");
        assertThat(global.getMapToGuest()).isEqualTo("Bad User");
        assertThat(global.getInterfaces()).isEqualTo("192.168.1.0/24");
        assertThat(global.isBindInterfacesOnly()).isTrue();
        assertThat(global.isLoadPrinters()).isFalse();
        assertThat(global.isDisableNetbios()).isTrue();
        assertThat(global.getServerMinProtocol()).isEqualTo("SMB2");
        assertThat(global.getServerMaxProtocol()).isEqualTo("SMB3");
    }

    @Test
    @DisplayName("Должен извлекать только пользовательские шары, игнорируя служебные секции")
    void shouldParseOnlyUserShares() {
        List<SambaShare> shares = parser.parseShares(sampleConfig);

        assertThat(shares).hasSize(2);

        SambaShare publicShare = shares.stream()
                .filter(s -> s.getName().equals("public"))
                .findFirst()
                .orElse(null);

        assertThat(publicShare).isNotNull();
        assertThat(publicShare.getPath()).isEqualTo("/srv/samba/public");
        assertThat(publicShare.isReadOnly()).isFalse();
        assertThat(publicShare.isGuestOk()).isTrue();
        assertThat(publicShare.isBrowseable()).isTrue();

        SambaShare privateShare = shares.stream()
                .filter(s -> s.getName().equals("private"))
                .findFirst()
                .orElse(null);

        assertThat(privateShare).isNotNull();
        assertThat(privateShare.getPath()).isEqualTo("/srv/samba/private");
        assertThat(privateShare.isReadOnly()).isTrue();
        assertThat(privateShare.isGuestOk()).isFalse();
        assertThat(privateShare.getValidUsers()).isEqualTo("admin, @managers");
    }

    @Test
    @DisplayName("Должен корректно генерировать блок шары из DTO")
    void shouldBuildShareSection() {
        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName("finance");
        dto.setPath("/srv/samba/finance");
        dto.setComment("Finance Docs");
        dto.setReadOnly(true);
        dto.setGuestOk(false);
        dto.setBrowseable(true);
        dto.setValidUsers("accountant");

        String section = parser.buildShareSection(dto);

        assertThat(section).contains("[finance]");
        assertThat(section).contains("path = /srv/samba/finance");
        assertThat(section).contains("comment = Finance Docs");
        assertThat(section).contains("read only = yes");
        assertThat(section).contains("guest ok = no");
        assertThat(section).contains("valid users = accountant");
    }

    @Test
    @DisplayName("Должен удалять указанную секцию из конфигурации")
    void shouldRemoveSection() {
        String result = parser.removeSection(sampleConfig, "public");

        assertThat(result).doesNotContain("[public]");
        assertThat(result).doesNotContain("/srv/samba/public");
        assertThat(result).contains("[global]");
        assertThat(result).contains("[private]");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при попытке удалить несуществующую секцию")
    void shouldThrowExceptionWhenRemovingNonExistentSection() {
        assertThatThrownBy(() -> parser.removeSection(sampleConfig, "non_existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    @DisplayName("Должен корректно обновлять секцию [global], не затрагивая шары")
    void shouldUpdateGlobalSection() {
        SambaGlobalConfigDto newGlobal = new SambaGlobalConfigDto();
        newGlobal.setWorkgroup("NEWCORP");
        newGlobal.setServerString("New Server");

        String updatedConfig = parser.updateGlobalSection(sampleConfig, newGlobal);

        assertThat(updatedConfig).contains("workgroup = NEWCORP");
        assertThat(updatedConfig).contains("server string = New Server");
        assertThat(updatedConfig).doesNotContain("workgroup = TESTGROUP");
        assertThat(updatedConfig).contains("[public]");
        assertThat(updatedConfig).contains("[private]");
    }
}