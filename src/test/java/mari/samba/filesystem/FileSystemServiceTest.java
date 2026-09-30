package mari.samba.filesystem;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import mari.samba.config.SambaProperties;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileSystemServiceTest {

  @Mock private CommandExecutor commandExecutor;
  @Mock private SambaProperties properties;
  @Mock private SambaProperties.Security mockSecurity;

  private FileSystemService fileSystemService;

  private static final String SESSION_ID = "test-session";

  @BeforeEach
  void setUp() {
    when(properties.security()).thenReturn(mockSecurity);
    when(mockSecurity.allowedRoots()).thenReturn(List.of("/srv", "/data", "/mnt"));

    fileSystemService = new FileSystemService(commandExecutor, properties);
  }

  // ==========================================
  // Tests for listDirectories
  // ==========================================

  @Test
  void testListDirectories_ShouldParseGivenOutput() throws Exception {
    String path = "/srv/samba";
    String mockOutput = "/srv/samba/public\n/srv/samba/private";

    when(commandExecutor.execute(eq(SESSION_ID), anyString())).thenReturn(mockOutput);

    DirectoryBrowseResultDto result = fileSystemService.listDirectories(SESSION_ID, path);

    assertNotNull(result);
    assertEquals("/srv/samba", result.currentPath());
    assertEquals("/srv", result.parentPath());
    assertEquals(2, result.directories().size());
    assertEquals("public", result.directories().get(0).name());
    assertEquals("/srv/samba/public", result.directories().get(0).fullPath());
  }

  @Test
  void testListDirectories_RootPath_ShouldReturnAllowedRoots() {
    DirectoryBrowseResultDto result = fileSystemService.listDirectories(SESSION_ID, "/");

    assertNotNull(result);
    assertEquals("/", result.currentPath());
    assertEquals(3, result.directories().size());
    assertTrue(result.directories().stream().anyMatch(i -> i.fullPath().equals("/srv")));
    assertTrue(result.directories().stream().anyMatch(i -> i.fullPath().equals("/data")));
  }

  @Test
  void testListDirectories_UnauthorizedPath_ShouldThrowSecurityException() {
    assertThrows(
        SecurityException.class,
        () -> fileSystemService.listDirectories(SESSION_ID, "/etc/shadow"));
  }

  // ==========================================
  // Tests for createDirectory
  // ==========================================

  @Test
  void testCreateDirectory_ValidName_ShouldExecuteMkdirAndChmod() throws Exception {
    fileSystemService.createDirectory(SESSION_ID, "/srv/samba", "new_folder");

    verify(commandExecutor)
        .execute(eq(SESSION_ID), eq(LinuxCommands.mkdir("/srv/samba/new_folder")));
    verify(commandExecutor)
        .execute(eq(SESSION_ID), eq(LinuxCommands.chmod("0775", "/srv/samba/new_folder")));
  }

  @Test
  void testCreateDirectory_InvalidName_ShouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> fileSystemService.createDirectory(SESSION_ID, "/srv/samba", "bad;folder*name"));
  }

  @Test
  void testCreateDirectory_UnauthorizedParent_ShouldThrowSecurityException() {
    assertThrows(
        SecurityException.class,
        () -> fileSystemService.createDirectory(SESSION_ID, "/root", "my_folder"));
  }

  // ==========================================
  // Tests for getDiskUsage
  // ==========================================

  @Test
  void testGetDiskUsage_ValidDfOutput_ShouldParseCorrectly() throws Exception {
    String path = "/srv/samba";
    String mockDfOutput =
        "Filesystem     1024-blocks      Used Available Capacity Mounted on\n"
            + "/dev/sda1        104857600  41943040  62914560      40% /srv";

    when(commandExecutor.execute(eq(SESSION_ID), anyString())).thenReturn(mockDfOutput);

    DiskUsageDto result = fileSystemService.getDiskUsage(SESSION_ID, path);

    assertNotNull(result);
    assertEquals(path, result.path());
    assertEquals(40, result.usePercent());
    assertEquals("/srv", result.mountPoint());
    assertTrue(result.total().contains("GB"));
  }

  @Test
  void testGetDiskUsage_UnauthorizedPath_ShouldThrowSecurityException() {
    assertThrows(SecurityException.class, () -> fileSystemService.getDiskUsage(SESSION_ID, "/etc"));
  }
}
