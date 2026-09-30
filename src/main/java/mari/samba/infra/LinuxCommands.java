package mari.samba.infra;

import java.util.regex.Pattern;

/**
 * Utility for generating safe Linux commands. Enforces strict input validation to prevent Shell
 * Injection and Path Traversal. Includes defense-in-depth measures like "--" to prevent
 * Argument/Option Injection.
 */
public final class LinuxCommands {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_-]{0,31}$");
  private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile("[|&;\\$<>!\\n\\r`]");

  private LinuxCommands() {}

  /**
   * Escapes a string for safe usage in bash within single quotes. Rejects newlines to prevent
   * multi-line execution vulnerabilities.
   */
  public static String escape(String arg) {
    if (arg == null) return "''";
    if (arg.contains("\n") || arg.contains("\r")) {
      throw new SecurityException("Newlines are strictly forbidden in shell arguments.");
    }
    return "'" + arg.replace("'", "'\\''") + "'";
  }

  // --- Security Guard Methods ---

  private static String requireValidUsername(String username) {
    if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
      throw new SecurityException("Invalid username format: possible injection detected.");
    }
    return username;
  }

  private static String requireValidPath(String path) {
    if (path == null || path.isBlank()) {
      throw new SecurityException("Path cannot be empty.");
    }
    if (path.contains("..")) {
      throw new SecurityException("Path traversal attempt detected (..).");
    }
    if (DANGEROUS_CHARS_PATTERN.matcher(path).find()) {
      throw new SecurityException("Path contains forbidden shell characters.");
    }
    if (!path.startsWith("/")) {
      throw new SecurityException("Path must be absolute (start with /).");
    }
    return path;
  }

  private static String requireValidAction(String action) {
    if (action == null || !action.matches("^(start|stop|restart|reload|status|is-active)$")) {
      throw new SecurityException("Invalid systemd action requested.");
    }
    return action;
  }

  private static String requireValidPid(String pid) {
    if (pid == null || !pid.matches("^\\d+$")) {
      throw new SecurityException("PID must be strictly numeric.");
    }
    return pid;
  }

  // ==========================================
  // File and Directory Operations (Safe)
  // ==========================================

  public static String cat(String path) {
    return String.format("cat -- %s", escape(requireValidPath(path)));
  }

  public static String sudoCat(String path) {
    return String.format("sudo cat -- %s", escape(requireValidPath(path)));
  }

  public static String writeToFileStdin(String targetPath) {
    return String.format("cat > %s", escape(requireValidPath(targetPath)));
  }

  public static String du(String path) {
    return String.format("sudo du -sh -- %s 2>/dev/null", escape(requireValidPath(path)));
  }

  public static String df(String path) {
    return String.format("sudo df -kP -- %s", escape(requireValidPath(path)));
  }

  public static String findDirectories(String path) {
    return String.format(
        "sudo find -L %s -mindepth 1 -maxdepth 1 -type d ! -name '.*' 2>/dev/null | sort",
        escape(requireValidPath(path)));
  }

  public static String mkdir(String path) {
    return String.format("sudo mkdir -p -- %s", escape(requireValidPath(path)));
  }

  public static String chmod(String permissions, String path) {
    if (!permissions.matches("^[0-7]{3,4}$")) {
      throw new SecurityException("Invalid chmod permissions format.");
    }
    return String.format("sudo chmod %s -- %s", permissions, escape(requireValidPath(path)));
  }

  public static String chownRecursive(String owner, String path) {
    if (!owner.matches("^[a-z_][a-z0-9_-]*(:[a-z_][a-z0-9_-]*)?$")) {
      throw new SecurityException("Invalid chown owner profile.");
    }
    return String.format("sudo chown -R %s -- %s", escape(owner), escape(requireValidPath(path)));
  }

  public static String copy(String source, String destination) {
    return String.format(
        "sudo cp -- %s %s",
        escape(requireValidPath(source)), escape(requireValidPath(destination)));
  }

  public static String move(String source, String destination) {
    return String.format(
        "sudo mv -- %s %s",
        escape(requireValidPath(source)), escape(requireValidPath(destination)));
  }

  public static String listBackupsDetailed(String backupDir) {
    return String.format(
        "ls -lh --time-style=\"+%%Y-%%m-%%d %%H:%%M:%%S\" -- %s/smb.conf.backup_* 2>/dev/null",
        escape(requireValidPath(backupDir)));
  }

  public static String cleanupOldBackups(String backupDir, int keepCount) {
    if (keepCount < 1 || keepCount > 100) throw new SecurityException("Invalid backup keep count.");
    return String.format(
        "ls -t %s/smb.conf.backup_* 2>/dev/null | tail -n +%d | xargs -r sudo rm --",
        escape(requireValidPath(backupDir)), keepCount + 1);
  }

  // ==========================================
  // Logs and Config Verification
  // ==========================================

  public static String tail(String filePath, int lines) {
    if (lines < 1 || lines > 5000) throw new SecurityException("Invalid lines requested.");
    return String.format("sudo tail -n %d -- %s", lines, escape(requireValidPath(filePath)));
  }

  public static String testparmSilent(String filePath) {
    return String.format("testparm -s -- %s > /dev/null", escape(requireValidPath(filePath)));
  }

  // ==========================================
  // Systemd Services
  // ==========================================

  public static String systemctl(String action, String service) {
    if (!service.matches("^[a-zA-Z0-9_-]+$")) throw new SecurityException("Invalid service name.");
    return String.format(
        "sudo systemctl %s -- %s", escape(requireValidAction(action)), escape(service));
  }

  // ==========================================
  // Samba Users (pdbedit / smbpasswd)
  // ==========================================

  public static String listSambaUsers() {
    return "sudo pdbedit -L";
  }

  public static String checkUserExists(String username) {
    return String.format("id -- %s", escape(requireValidUsername(username)));
  }

  public static String addSystemUserWithHome(String username, String comment) {
    return String.format(
        "sudo useradd -m -s /bin/bash -c %s -- %s",
        escape(comment), escape(requireValidUsername(username)));
  }

  public static String deleteSystemUser(String username) {
    return String.format("sudo userdel -r -- %s", escape(requireValidUsername(username)));
  }

  public static String chpasswd() {
    return "sudo chpasswd";
  }

  public static String addSambaUser(String username) {
    return String.format("sudo smbpasswd -s -a %s", escape(requireValidUsername(username)));
  }

  public static String enableSambaUser(String username) {
    return String.format("sudo smbpasswd -e %s", escape(requireValidUsername(username)));
  }

  public static String changeSambaPassword(String username) {
    return String.format("sudo smbpasswd -s %s", escape(requireValidUsername(username)));
  }

  public static String deleteSambaUser(String username) {
    return String.format("sudo smbpasswd -x %s", escape(requireValidUsername(username)));
  }

  // ==========================================
  // Samba Monitoring (SMBSTATUS)
  // ==========================================

  public static String smbstatus(String flag) {
    if (!flag.matches("^-[a-zA-Z]$")) throw new SecurityException("Invalid smbstatus flag.");
    return "sudo smbstatus " + flag;
  }

  public static String kill(String pid) {
    return "sudo kill -9 " + requireValidPid(pid);
  }

  // ==========================================
  // Active Directory Integration
  // ==========================================

  public static String netAdsJoin(String username, String password) {
    return String.format("sudo net ads join -U %s", escape(username + "%" + password));
  }

  public static String netAdsLeave(String username, String password) {
    return String.format("sudo net ads leave -U %s", escape(username + "%" + password));
  }

  public static String netAdsTestJoin() {
    return "sudo net ads testjoin";
  }

  public static String wbinfoUsers() {
    return "wbinfo -u";
  }

  public static String wbinfoGroups() {
    return "wbinfo -g";
  }

  public static String writeKrb5Conf() {
    return "sudo tee /etc/krb5.conf > /dev/null";
  }
}
