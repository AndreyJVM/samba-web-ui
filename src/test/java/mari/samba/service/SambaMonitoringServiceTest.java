package mari.samba.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaMonitoringServiceTest {

  @Mock private CommandExecutor commandExecutor;

  @InjectMocks private SambaMonitoringService monitoringService;

  private static final String SESSION_ID = "test-session";

  // ==========================================
  // isServiceRunning Tests
  // ==========================================

  @Test
  void testIsServiceRunning_WhenActive_ShouldReturnTrue() throws Exception {
    when(commandExecutor.execute(SESSION_ID, LinuxCommands.systemctl("is-active", "smbd")))
        .thenReturn("active\n");

    boolean active = monitoringService.isServiceRunning(SESSION_ID);
    assertTrue(active);
  }

  @Test
  void testIsServiceRunning_WhenInactive_ShouldReturnFalse() throws Exception {
    when(commandExecutor.execute(SESSION_ID, LinuxCommands.systemctl("is-active", "smbd")))
        .thenReturn("inactive\n");

    boolean active = monitoringService.isServiceRunning(SESSION_ID);
    assertFalse(active);
  }

  @Test
  void testIsServiceRunning_WhenException_ShouldReturnFalse() throws Exception {
    when(commandExecutor.execute(SESSION_ID, LinuxCommands.systemctl("is-active", "smbd")))
        .thenThrow(new RuntimeException("Command failed"));

    boolean active = monitoringService.isServiceRunning(SESSION_ID);
    assertFalse(active);
  }

  // ==========================================
  // getActiveConnections Tests
  // ==========================================

  @Test
  void testGetActiveConnections_ShouldParseOutputCorrectly() throws Exception {
    String mockOutput =
        "Samba version 4.15.13-Ubuntu\n"
            + "PID     Username     Group        Machine                                   Protocol Version  Encryption           Signing\n"
            + "----------------------------------------------------------------------------------------------------------------------------------------\n"
            + "76174   nobody       nogroup      192.168.1.100 (ipv4:192.168.1.100:43336) SMB3_11           -                    -\n"
            + "12345   alice        users        desktop-pc (ipv4:10.0.0.5:54321)         SMB3_11           -                    -\n";

    when(commandExecutor.execute(SESSION_ID, LinuxCommands.smbstatus("-b"))).thenReturn(mockOutput);

    List<Map<String, String>> connections = monitoringService.getActiveConnections(SESSION_ID);

    assertEquals(2, connections.size());

    assertEquals("76174", connections.get(0).get("pid"));
    assertEquals("nobody", connections.get(0).get("user"));
    assertEquals("192.168.1.100", connections.get(0).get("machine"));

    assertEquals("12345", connections.get(1).get("pid"));
    assertEquals("alice", connections.get(1).get("user"));
    assertEquals("desktop-pc", connections.get(1).get("machine"));
  }

  @Test
  void testGetActiveConnections_WhenEmptyOutput_ShouldReturnEmptyList() throws Exception {
    when(commandExecutor.execute(SESSION_ID, LinuxCommands.smbstatus("-b"))).thenReturn("");

    List<Map<String, String>> connections = monitoringService.getActiveConnections(SESSION_ID);
    assertTrue(connections.isEmpty());
  }

  // ==========================================
  // getOpenFiles Tests
  // ==========================================

  @Test
  void testGetOpenFiles_ShouldParseOutputCorrectly() throws Exception {
    String mockOutput =
        "\nLocked files:\n"
            + "Pid          Uid        DenyMode   Access      R/W        Oplock           SharePath   Name   Time\n"
            + "--------------------------------------------------------------------------------------------------\n"
            + "76174        65534      DENY_NONE  0x100081    RDONLY     NONE             /srv/samba   public/test file.txt   Mon May 15 12:30:00 2023\n"
            + "12345        1000       DENY_WRITE 0x120089    RDWR       EXCLUSIVE+BATCH  /srv/samba   private/doc.docx   Tue May 16 09:00:00 2023\n";

    when(commandExecutor.execute(SESSION_ID, LinuxCommands.smbstatus("-L"))).thenReturn(mockOutput);

    List<Map<String, String>> files = monitoringService.getOpenFiles(SESSION_ID);

    assertEquals(2, files.size());

    // File 1 (with space in name)
    assertEquals("76174", files.get(0).get("pid"));
    assertEquals("RDONLY", files.get(0).get("rw"));
    assertEquals("public/test file.txt", files.get(0).get("file")); // Space is preserved correctly

    // File 2
    assertEquals("12345", files.get(1).get("pid"));
    assertEquals("RDWR", files.get(1).get("rw"));
    assertEquals("private/doc.docx", files.get(1).get("file"));
  }

  // ==========================================
  // controlService Tests
  // ==========================================

  @Test
  void testControlService_ValidAction_ShouldExecute() throws Exception {
    monitoringService.controlService(SESSION_ID, "restart");
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.systemctl("restart", "smbd"));
  }

  @Test
  void testControlService_InvalidAction_ShouldThrowException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> monitoringService.controlService(SESSION_ID, "delete"));

    assertTrue(ex.getMessage().contains("Недопустимое действие для службы: delete"));
    verifyNoInteractions(commandExecutor);
  }

  // ==========================================
  // killSession Tests
  // ==========================================

  @Test
  void testKillSession_ValidPid_ShouldExecuteKill() throws Exception {
    monitoringService.killSession(SESSION_ID, "12345");
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.kill("12345"));
  }

  @Test
  void testKillSession_InvalidPid_ShouldThrowException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> monitoringService.killSession(SESSION_ID, "1234a"));

    assertTrue(ex.getMessage().contains("Некорректный PID"));
    verifyNoInteractions(commandExecutor);
  }

  // ==========================================
  // getDiskUsage Tests
  // ==========================================

  @Test
  void testGetDiskUsage_ShouldParseDfOutput() throws Exception {
    String mockOutput =
        "Filesystem      Size  Used Avail Use% Mounted on\n"
            + "tmpfs           1.6G  2.0M  1.6G   1% /run\n"
            + "/dev/sda2       100G   50G   50G  50% /\n";

    when(commandExecutor.execute(SESSION_ID, "df -h")).thenReturn(mockOutput);

    List<String> stats = monitoringService.getDiskUsage(SESSION_ID);

    assertEquals(2, stats.size());
    // Format returned is essentially "mountPoint capacity totalSize" (e.g., "/run 1% 1.6G")
    assertEquals("/run 1% 1.6G", stats.get(0));
    assertEquals("/ 50% 100G", stats.get(1));
  }
}
