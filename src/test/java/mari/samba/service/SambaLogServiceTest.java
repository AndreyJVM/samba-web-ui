package mari.samba.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaLogServiceTest {

  @Mock private CommandExecutor commandExecutor;

  @InjectMocks private SambaLogService sambaLogService;

  @Nested
  class GetRecentLogs {

    @Test
    void getRecentLogs_withAllowedLinesCount_executesTailWithRequestedLines() throws Exception {
      // given
      String sessionId = "test-session";
      int requestedLines = 200;
      String expectedLog = "line 1\nline 2";
      String expectedCmd = LinuxCommands.tail("/var/log/samba/log.smbd", 200);
      when(commandExecutor.execute(eq(sessionId), eq(expectedCmd))).thenReturn(expectedLog);

      // when
      String actualLog = sambaLogService.getRecentLogs(sessionId, requestedLines);

      // then
      assertThat(actualLog).isEqualTo(expectedLog);
    }

    @Test
    void getRecentLogs_withUnallowedLinesCount_executesTailWithDefault100Lines() throws Exception {
      // given
      String sessionId = "test-session";
      int requestedLines = 333; // not in ALLOWED_LINE_COUNTS
      String expectedLog = "some default logs";
      String expectedCmd = LinuxCommands.tail("/var/log/samba/log.smbd", 100);
      when(commandExecutor.execute(eq(sessionId), eq(expectedCmd))).thenReturn(expectedLog);

      // when
      String actualLog = sambaLogService.getRecentLogs(sessionId, requestedLines);

      // then
      assertThat(actualLog).isEqualTo(expectedLog);
    }

    @Test
    void getRecentLogs_whenCommandExecutorThrowsException_returnsErrorMessage() throws Exception {
      // given
      String sessionId = "test-session";
      int requestedLines = 50;
      when(commandExecutor.execute(anyString(), anyString()))
          .thenThrow(new RuntimeException("Access denied"));

      // when
      String actualLog = sambaLogService.getRecentLogs(sessionId, requestedLines);

      // then
      assertThat(actualLog)
          .startsWith("Не удалось прочитать лог-файл /var/log/samba/log.smbd: Access denied");
    }
  }
}
