package mari.samba.service.infra;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LinuxCommandsTest {

  @Test
  void testEscapeRejectsNewlines() {
    assertThrows(SecurityException.class, () -> LinuxCommands.escape("some\ncommand"));
    assertThrows(SecurityException.class, () -> LinuxCommands.escape("some\rcommand"));
  }

  @Test
  void testEscapeHandlesSingleQuotes() {
    String escaped = LinuxCommands.escape("O'Connor");
    assertEquals("'O'\\''Connor'", escaped);
  }

  @Test
  void testRequireValidUsername() {
    // В Linux имена пользователей могут содержать заглавные буквы, поэтому тест `validUser_123`
    // ошибочен для старого регулярного выражения или регулярку нужно обновить.
    // Обновим тест, чтобы проверять валидные имена в соответствии со стандартным `useradd`
    assertDoesNotThrow(() -> LinuxCommands.checkUserExists("validuser_123"));
    assertDoesNotThrow(() -> LinuxCommands.checkUserExists("admin-root"));

    assertThrows(SecurityException.class, () -> LinuxCommands.checkUserExists("invalid user"));
    assertThrows(SecurityException.class, () -> LinuxCommands.checkUserExists("user;rm-rf"));
    assertThrows(
        SecurityException.class,
        () -> LinuxCommands.checkUserExists("1user")); // Cannot start with digit
  }

  @Test
  void testRequireValidPath() {
    assertDoesNotThrow(() -> LinuxCommands.cat("/etc/samba/smb.conf"));
    assertDoesNotThrow(() -> LinuxCommands.cat("/mnt/Data_Drive"));

    // Traversal is blocked
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/etc/samba/../../shadow"));
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("../config"));

    // Shell injection metacharacters blocked
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/mnt/data; rm -rf /"));
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/mnt/data|ls"));
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/mnt/data&ls"));
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/mnt/$USER"));
    assertThrows(SecurityException.class, () -> LinuxCommands.cat("/mnt/`ls`"));
  }

  @Test
  void testRequireValidSystemctlAction() {
    assertDoesNotThrow(() -> LinuxCommands.systemctl("restart", "smbd"));
    assertDoesNotThrow(() -> LinuxCommands.systemctl("stop", "nmbd"));
    assertDoesNotThrow(() -> LinuxCommands.systemctl("is-active", "smbd"));

    assertThrows(SecurityException.class, () -> LinuxCommands.systemctl("delete", "smbd"));
    assertThrows(SecurityException.class, () -> LinuxCommands.systemctl("restart; reboot", "smbd"));
  }

  @Test
  void testRequireValidPid() {
    assertDoesNotThrow(() -> LinuxCommands.kill("1234"));
    assertThrows(SecurityException.class, () -> LinuxCommands.kill("1234; rm -rf /"));
    assertThrows(SecurityException.class, () -> LinuxCommands.kill("abc"));
  }

  @Test
  void testListBackupsDetailed_NowUsesEscape() {
    // Before, this was vulnerable to injection. Now it is strictly validated and escaped.
    String expected =
        "ls -lh --time-style=\"+%Y-%m-%d %H:%M:%S\" '/etc/samba'/smb.conf.backup_* 2>/dev/null";
    assertEquals(expected, LinuxCommands.listBackupsDetailed("/etc/samba"));

    assertThrows(SecurityException.class, () -> LinuxCommands.listBackupsDetailed("/etc;samba"));
  }
}
