package mari.samba.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

import java.util.List;
import mari.samba.dto.config.SambaBackupDto;
import mari.samba.dto.config.SambaGlobalConfigDto;
import mari.samba.dto.share.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import mari.samba.service.parser.SmbConfParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaConfigServiceTest {

  @Mock private CommandExecutor commandExecutor;
  @Mock private SmbConfParser smbConfParser;

  @InjectMocks private SambaConfigService sambaConfigService;

  @Nested
  class ContentOperations {

    @Test
    void getSmbConfContent_returnsFileContent() throws Exception {
      // given
      String expectedCmd = LinuxCommands.cat("/etc/samba/smb.conf");
      when(commandExecutor.execute("session-1", expectedCmd))
          .thenReturn("[global]\nworkgroup = WORKGROUP");

      // when
      String content = sambaConfigService.getSmbConfContent("session-1");

      // then
      assertThat(content).isEqualTo("[global]\nworkgroup = WORKGROUP");
    }

    @Test
    void parseShares_delegatesToParser() {
      // given
      String configData = "[share]\npath=/data";
      SambaShare mockShare = new SambaShare();
      mockShare.setName("share");
      when(smbConfParser.parseShares(configData)).thenReturn(List.of(mockShare));

      // when
      List<SambaShare> shares = sambaConfigService.parseShares(configData);

      // then
      assertThat(shares).hasSize(1);
      assertThat(shares.get(0).getName()).isEqualTo("share");
    }

    @Test
    void buildShareSection_delegatesToParser() {
      // given
      SambaShareCreateDto dto = new SambaShareCreateDto();
      when(smbConfParser.buildShareSection(dto)).thenReturn("[new_share]");

      // when
      String section = sambaConfigService.buildShareSection(dto);

      // then
      assertThat(section).isEqualTo("[new_share]");
    }

    @Test
    void removeShareSection_delegatesToParser() {
      // given
      String initial = "[share1]\n[share2]";
      when(smbConfParser.removeSection(initial, "share2")).thenReturn("[share1]\n");

      // when
      String updated = sambaConfigService.removeShareSection(initial, "share2");

      // then
      assertThat(updated).isEqualTo("[share1]\n");
    }
  }

  @Nested
  class BackupOperations {

    @Test
    void createBackup_executesCommandsSuccessfully() throws Exception {
      // given
      String sessionId = "sess-1";

      // when
      sambaConfigService.createBackup(sessionId);

      // then
      verify(commandExecutor).execute(eq(sessionId), eq(LinuxCommands.mkdir("/etc/samba/backups")));

      ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
      verify(commandExecutor, times(3)).execute(eq(sessionId), cmdCaptor.capture());
      List<String> executedCommands = cmdCaptor.getAllValues();

      assertThat(executedCommands.get(1))
          .startsWith("sudo cp '/etc/samba/smb.conf' '/etc/samba/backups/smb.conf.backup_");
      assertThat(executedCommands.get(2))
          .startsWith("ls -t '/etc/samba/backups'/smb.conf.backup_*");
    }

    @Test
    void listBackups_returnsParsedBackupList() throws Exception {
      // given
      String sessionId = "sess-1";
      String fakeLsOutput =
          "-rw-r--r-- 1 root root 1200 2024-10-15 14:30:00 /etc/samba/backups/smb.conf.backup_20241015_143000\n"
              + "-rw-r--r-- 1 root root 1150 2024-10-14 10:20:00 /etc/samba/backups/smb.conf.backup_20241014_102000";
      when(commandExecutor.execute(eq(sessionId), matches("ls -lh --time-style.*")))
          .thenReturn(fakeLsOutput);

      // when
      List<SambaBackupDto> backups = sambaConfigService.listBackups(sessionId);

      // then
      assertThat(backups).hasSize(2);
      assertThat(backups.get(0).filename()).isEqualTo("smb.conf.backup_20241015_143000");
      assertThat(backups.get(0).createdAt()).isEqualTo("2024-10-15 14:30:00");
      assertThat(backups.get(0).size()).isEqualTo("1200");
    }

    @Test
    void listBackups_whenCommandFails_returnsEmptyList() throws Exception {
      // given
      String sessionId = "sess-1";
      when(commandExecutor.execute(eq(sessionId), anyString()))
          .thenThrow(new RuntimeException("Command failed"));

      // when
      List<SambaBackupDto> backups = sambaConfigService.listBackups(sessionId);

      // then
      assertThat(backups).isEmpty();
    }

    @Test
    void restoreBackup_withValidName_executesRestoreAndRestartsService() throws Exception {
      // given
      String sessionId = "sess-1";
      String filename = "smb.conf.backup_20301015_143000";

      // when
      sambaConfigService.restoreBackup(sessionId, filename);

      // then
      verify(commandExecutor).execute(eq(sessionId), eq(LinuxCommands.mkdir("/etc/samba/backups")));
      verify(commandExecutor)
          .execute(
              eq(sessionId),
              eq(LinuxCommands.copy("/etc/samba/backups/" + filename, "/etc/samba/smb.conf")));
      verify(commandExecutor)
          .execute(eq(sessionId), eq(LinuxCommands.systemctl("restart", "smbd")));
    }

    @Test
    void restoreBackup_withInvalidName_throwsIllegalArgumentException() {
      // given
      String invalidName = "smb.conf.backup_latest"; // lacks correct timestamp format

      // when / then
      assertThatThrownBy(() -> sambaConfigService.restoreBackup("session-1", invalidName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Некорректное имя файла бэкапа");
    }
  }

  @Nested
  class UpdateOperations {

    @Test
    void updateSmbConf_writesChecksAndMoves() throws Exception {
      // given
      String sessionId = "sess-1";
      String newContent = "[global]\na=b";

      // when
      sambaConfigService.updateSmbConf(sessionId, newContent);

      // then
      verify(commandExecutor)
          .execute(
              eq(sessionId),
              eq(LinuxCommands.writeToFileStdin("/tmp/smb.conf.tmp")),
              eq(newContent));
      verify(commandExecutor)
          .execute(eq(sessionId), eq(LinuxCommands.testparmSilent("/tmp/smb.conf.tmp")));
      verify(commandExecutor)
          .execute(
              eq(sessionId), eq(LinuxCommands.move("/tmp/smb.conf.tmp", "/etc/samba/smb.conf")));
      verify(commandExecutor)
          .execute(eq(sessionId), eq(LinuxCommands.systemctl("restart", "smbd")));
    }

    @Test
    void getGlobalConfig_parsesAndReturnsConfigDto() throws Exception {
      // given
      String sessionId = "sess-1";
      String configContent = "[global]";
      when(commandExecutor.execute(sessionId, LinuxCommands.cat("/etc/samba/smb.conf")))
          .thenReturn(configContent);
      SambaGlobalConfigDto dto = new SambaGlobalConfigDto();
      when(smbConfParser.parseGlobalConfig(configContent)).thenReturn(dto);

      // when
      SambaGlobalConfigDto result = sambaConfigService.getGlobalConfig(sessionId);

      // then
      assertThat(result).isSameAs(dto);
    }

    @Test
    void updateGlobalConfig_updatesAndSavesConfiguration() throws Exception {
      // given
      String sessionId = "sess-1";
      SambaGlobalConfigDto updateDto = new SambaGlobalConfigDto();
      when(commandExecutor.execute(sessionId, LinuxCommands.cat("/etc/samba/smb.conf")))
          .thenReturn("[global]");
      when(smbConfParser.updateGlobalSection("[global]", updateDto))
          .thenReturn("[global]\nserver string=Hello");

      // when
      sambaConfigService.updateGlobalConfig(sessionId, updateDto);

      // then
      verify(commandExecutor)
          .execute(
              sessionId,
              LinuxCommands.writeToFileStdin("/tmp/smb.conf.tmp"),
              "[global]\nserver string=Hello");
    }
  }
}
