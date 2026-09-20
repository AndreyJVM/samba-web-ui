package mari.samba.service.infra;

public final class LinuxCommands {

  private LinuxCommands() {}

  public static String escape(String arg) {
    if (arg == null) return "''";
    return "'" + arg.replace("'", "'\\''") + "'";
  }

  // ==========================================
  // ФАЙЛОВАЯ СИСТЕМА И ФАЙЛЫ
  // ==========================================

  public static String cat(String path) {
    return String.format("cat %s", escape(path));
  }

  public static String sudoCat(String path) {
    return String.format("sudo cat %s", escape(path));
  }

  public static String writeToFileStdin(String targetPath) {
    return String.format("cat > %s", escape(targetPath));
  }

  public static String df(String path) {
    return String.format("sudo df -kP %s", escape(path));
  }

  public static String findDirectories(String path) {
    return String.format(
        "sudo find %s -mindepth 1 -maxdepth 1 -type d ! -name '.*' 2>/dev/null | sort",
        escape(path));
  }

  public static String mkdir(String path) {
    return String.format("sudo mkdir -p %s", escape(path));
  }

  public static String chmod(String permissions, String path) {
    return String.format("sudo chmod %s %s", permissions, escape(path));
  }

  public static String chownRecursive(String owner, String path) {
    return String.format("sudo chown -R %s %s", escape(owner), escape(path));
  }

  public static String copy(String source, String destination) {
    return String.format("sudo cp %s %s", escape(source), escape(destination));
  }

  public static String move(String source, String destination) {
    return String.format("sudo mv %s %s", escape(source), escape(destination));
  }

  public static String listBackupsDetailed(String backupDir) {
    return String.format(
        "ls -lh --time-style=\"+%%Y-%%m-%%d %%H:%%M:%%S\" %s/smb.conf.backup_* 2>/dev/null",
        backupDir);
  }

  public static String cleanupOldBackups(String backupDir, int keepCount) {
    return String.format(
        "ls -t %s/smb.conf.backup_* 2>/dev/null | tail -n +%d | xargs -r sudo rm --",
        backupDir, keepCount + 1);
  }

  // ==========================================
  // ЛОГИ И ТЕСТЫ КОНФИГУРАЦИИ
  // ==========================================

  public static String tail(String filePath, int lines) {
    return String.format("sudo tail -n %d %s", lines, escape(filePath));
  }

  public static String testparmSilent(String filePath) {
    return String.format("testparm -s %s > /dev/null", escape(filePath));
  }

  // ==========================================
  // СЛУЖБЫ (SYSTEMD)
  // ==========================================

  public static String systemctl(String action, String service) {
    return String.format("sudo systemctl %s %s", action, service);
  }

  // ==========================================
  // ПОЛЬЗОВАТЕЛИ
  // ==========================================

  public static String listSambaUsers() {
    return "sudo pdbedit -L";
  }

  public static String checkUserExists(String username) {
    return String.format("id %s", escape(username));
  }

  public static String addSystemUserWithHome(String username, String comment) {
    return String.format(
        "sudo useradd -m -s /bin/bash -c %s %s", escape(comment), escape(username));
  }

  public static String deleteSystemUser(String username) {
    return String.format("sudo userdel -r %s", escape(username));
  }

  public static String chpasswd() {
    return "sudo chpasswd";
  }

  public static String addSambaUser(String username) {
    return String.format("sudo smbpasswd -s -a %s", escape(username));
  }

  public static String enableSambaUser(String username) {
    return String.format("sudo smbpasswd -e %s", escape(username));
  }

  public static String changeSambaPassword(String username) {
    return String.format("sudo smbpasswd -s %s", escape(username));
  }

  public static String deleteSambaUser(String username) {
    return String.format("sudo smbpasswd -x %s", escape(username));
  }

  // ==========================================
  // МОНИТОРИНГ (SMBSTATUS)
  // ==========================================

  public static String smbstatus(String flag) {
    return "sudo smbstatus " + flag;
  }

  /** Команда для принудительного завершения процесса (SIGKILL) */
  public static String kill(String pid) {
    // Завершаем процесс с сигналом 9 (SIGKILL) для гарантированного обрыва сессии
    return "sudo kill -9 " + pid;
  }
}
