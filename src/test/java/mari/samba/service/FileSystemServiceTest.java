package mari.samba.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileSystemServiceTest {

  @Mock private CommandExecutor commandExecutor;

  @InjectMocks private FileSystemService fileSystemService;

  private static final String SESSION_ID = "test-session";

  // ==========================================
  // Tests for listDirectories
  // ==========================================

  @Test
  void testListDirectories_ShouldParseGivenOutput() throws Exception {
    String path = "/srv/samba";
    String mockOutput = "/srv/samba/public\n/srv/samba/private";

    when(commandExecutor.execute(eq(SESSION_ID), eq(LinuxCommands.findDirectories(path))))
        .thenReturn(mockOutput);

    DirectoryBrowseResultDto result = fileSystemService.listDirectories(SESSION_ID, path);

    assertNotNull(result);
    assertEquals("/srv/samba", result.currentPath());
    assertEquals("/srv", result.parentPath());
    assertEquals(2, result.directories().size());
    assertEquals("public", result.directories().get(0).name());
    assertEquals("/srv/samba/public", result.directories().get(0).fullPath());
    assertEquals("private", result.directories().get(1).name());
  }

  @Test
  void testListDirectories_WithEmptyPath_ShouldNormalizeToRoot() throws Exception {
    String mockOutput = "/etc\n/var";

    // normalizePath("") -> "/"
    when(commandExecutor.execute(eq(SESSION_ID), eq(LinuxCommands.findDirectories("/"))))
        .thenReturn(mockOutput);

    DirectoryBrowseResultDto result = fileSystemService.listDirectories(SESSION_ID, "");

    assertEquals("/", result.currentPath());
    assertEquals("/", result.parentPath()); // Parent of root is handled gracefully as "/"
    assertEquals(2, result.directories().size());
  }

  // ==========================================
  // Tests for createDirectory
  // ==========================================

  @Test
  void testCreateDirectory_ValidName_ShouldExecuteCommands() throws Exception {
    fileSystemService.createDirectory(SESSION_ID, "/srv/samba", "new_folder");

    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.mkdir("/srv/samba/new_folder"));
    verify(commandExecutor)
        .execute(SESSION_ID, LinuxCommands.chmod("0775", "/srv/samba/new_folder"));
  }

  @Test
  void testCreateDirectory_InvalidName_ShouldThrowException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> fileSystemService.createDirectory(SESSION_ID, "/srv", "bad name!"));

    assertEquals("Имя папки содержит недопустимые символы", ex.getMessage());
    verifyNoInteractions(commandExecutor);
  }

  // ==========================================
  // Tests for getDiskUsage
  // ==========================================

  @Test
  void testGetDiskUsage_ShouldParseValidDfOutput() throws Exception {
    String path = "/mnt/data";
    // Standard df -kP output
    String mockOutput =
        "Filesystem     1024-blocks      Used Available Capacity Mounted on\n"
            + "/dev/sda1        104857600  52428800  52428800      50% /mnt/data";

    when(commandExecutor.execute(eq(SESSION_ID), eq(LinuxCommands.df(path))))
        .thenReturn(mockOutput);

    DiskUsageDto result = fileSystemService.getDiskUsage(SESSION_ID, path);

    assertNotNull(result);
    assertEquals("/mnt/data", result.path());
    // 104857600 KB = 100 GB
    assertEquals("100.0 GB", result.total());
    // 52428800 KB = 50 GB
    assertEquals("50.0 GB", result.used());
    assertEquals("50.0 GB", result.available());
    assertEquals(50, result.usePercent());
    assertEquals("/mnt/data", result.mountPoint());
  }

  @Test
  void testGetDiskUsage_EmptyOutput_ShouldThrowException() throws Exception {
    when(commandExecutor.execute(anyString(), anyString())).thenReturn("");

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> fileSystemService.getDiskUsage(SESSION_ID, "/mnt"));
    assertTrue(ex.getMessage().contains("Пустой ответ от df"));
  }

  @Test
  void testGetDiskUsage_HeaderOnlyOutput_ShouldThrowException() throws Exception {
    String mockOutput = "Filesystem     1024-blocks      Used Available Capacity Mounted on";
    when(commandExecutor.execute(anyString(), anyString())).thenReturn(mockOutput);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> fileSystemService.getDiskUsage(SESSION_ID, "/mnt"));
    assertTrue(ex.getMessage().contains("Неожиданный вывод df"));
  }

  @Test
  void testGetDiskUsage_InvalidNumberFormat_ShouldThrowException() throws Exception {
    String mockOutput =
        "Filesystem     1024-blocks      Used Available Capacity Mounted on\n"
            + "/dev/sda1        BAD_NUM    BAD_NUM   BAD_NUM  50% /mnt/data";

    when(commandExecutor.execute(anyString(), anyString())).thenReturn(mockOutput);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> fileSystemService.getDiskUsage(SESSION_ID, "/mnt"));
    assertTrue(ex.getMessage().contains("Ошибка парсинга чисел из вывода df"));
  }
}
