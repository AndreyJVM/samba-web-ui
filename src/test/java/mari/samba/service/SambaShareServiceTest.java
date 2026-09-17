package mari.samba.service;

import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SambaShareServiceTest {

    @Mock
    private CommandExecutor commandExecutor;

    @Mock
    private SambaConfigService configService;

    @InjectMocks
    private SambaShareService shareService;

    private final String SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() {
        // leniency - allow stubbing to be unused in some tests
    }

    @Test
    void createShare_ShouldCreateDirectoryAndSetPermissions() throws Exception {
        // Arrange
        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName("testShare");
        dto.setPath("/srv/samba/test");
        dto.setComment("Test comment");
        dto.setForceUser("samba-admin"); // В DTO поле называется forceUser, а не owner
        dto.setValidUsers("user1, user2");
        dto.setReadOnly(false);
        dto.setBrowseable(true); // В DTO используется буква 'e': browseable

        // При чтении текущего конфига возвращаем пустоту
        when(configService.getSmbConfContent(SESSION_ID)).thenReturn("");
        when(configService.parseShares("")).thenReturn(Collections.emptyList());
        
        // Мокаем сборку конфиг секции
        when(configService.buildShareSection(dto)).thenReturn("[testShare]\n  path = /srv/samba/test\n");

        // Act
        shareService.createShare(SESSION_ID, dto);

        // Assert
        // Проверяем, что были вызваны команды настройки директории через commandExecutor.execute() (а не executeSafe)
        verify(commandExecutor).execute(SESSION_ID, LinuxCommands.mkdir("/srv/samba/test"));
        verify(commandExecutor).execute(SESSION_ID, LinuxCommands.chmod("0775", "/srv/samba/test"));
        verify(commandExecutor).execute(SESSION_ID, LinuxCommands.chownRecursive("samba-admin", "/srv/samba/test"));
        
        // Проверяем, что была вызвана команда обновления конфига updateSmbConf (а не addShare)
        verify(configService).updateSmbConf(SESSION_ID, "\n[testShare]\n  path = /srv/samba/test\n");
    }

    @Test
    void createShare_ShouldThrowException_WhenShareAlreadyExists() throws Exception {
        // Arrange
        SambaShareCreateDto dto = new SambaShareCreateDto();
        dto.setName("existingShare");
        dto.setPath("/srv/samba/existing");

        SambaShare existingShare = new SambaShare();
        existingShare.setName("existingShare");

        when(configService.getSmbConfContent(SESSION_ID)).thenReturn("");
        when(configService.parseShares("")).thenReturn(Collections.singletonList(existingShare));

        // Act & Assert
        assertThatThrownBy(() -> shareService.createShare(SESSION_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Шара с именем 'existingShare' уже существует");

        // Если шара уже есть, мы не должны вызывать никаких линукс команд
        verify(commandExecutor, never()).execute(anyString(), anyString());
    }
}
