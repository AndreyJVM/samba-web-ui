package mari.samba.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaShareServiceTest {

  @Mock private CommandExecutor commandExecutor;
  @Mock private SambaConfigService configService;
  @Mock private FileSystemService fileSystemService;

  @InjectMocks private SambaShareService shareService;

  private final String SESSION_ID = "test-session-id";

  @Test
  void createShare_ShouldCreateDirectoryAndSetPermissions() throws Exception {
    // Arrange
    SambaShareCreateDto dto =
        SambaShareCreateDto.builder()
            .name("testShare")
            .path("/srv/samba/test")
            .comment("Test comment")
            .forceUser("samba-admin")
            .validUsers("user1, user2")
            .readOnly(false)
            .browseable(true)
            .build();

    when(configService.getSmbConfContent(SESSION_ID)).thenReturn("");
    when(configService.parseShares("")).thenReturn(Collections.emptyList());

    when(configService.buildShareSection(dto))
        .thenReturn("[testShare]\n  path = /srv/samba/test\n");

    // Act
    shareService.createShare(SESSION_ID, dto);

    // Assert
    verify(fileSystemService).requireAllowedPath("/srv/samba/test");
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.mkdir("/srv/samba/test"));
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.chmod("0775", "/srv/samba/test"));
    verify(commandExecutor)
        .execute(SESSION_ID, LinuxCommands.chownRecursive("samba-admin", "/srv/samba/test"));

    verify(configService).updateSmbConf(SESSION_ID, "\n[testShare]\n  path = /srv/samba/test\n");
  }

  @Test
  void createShare_ShouldThrowException_WhenShareAlreadyExists() throws Exception {
    // Arrange
    SambaShareCreateDto dto =
        SambaShareCreateDto.builder().name("existingShare").path("/srv/samba/existing").build();

    SambaShare existingShare = SambaShare.builder().name("existingShare").build();

    when(configService.getSmbConfContent(SESSION_ID)).thenReturn("");
    when(configService.parseShares("")).thenReturn(Collections.singletonList(existingShare));

    // Act & Assert
    assertThatThrownBy(() -> shareService.createShare(SESSION_ID, dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Share already exists");

    verify(commandExecutor, never()).execute(anyString(), anyString());
  }
}
