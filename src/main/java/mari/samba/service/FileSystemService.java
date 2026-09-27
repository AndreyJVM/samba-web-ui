package mari.samba.service;

import java.util.ArrayList;
import java.util.List;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DirectoryItemDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileSystemService {

  @Autowired private CommandExecutor commandExecutor;

  // List of paths that are strictly forbidden to access via the web file explorer
  private static final String[] FORBIDDEN_ROOTS = {
    "/root",
    "/etc",
    "/boot",
    "/sbin",
    "/bin",
    "/dev",
    "/proc",
    "/sys",
    "/usr",
    "/var/lib",
    "/var/log"
  };

  /** Запрашивает список вложенных папок в указанном пути */
  public DirectoryBrowseResultDto listDirectories(String sessionId, String requestedPath)
      throws Exception {
    String safePath = normalizePath(requestedPath);
    requireAllowedPath(safePath);

    // Вычисляем родительскую директорию (java.nio.file.Paths тут не подходит, так
    // как сервер может быть на Windows)
    String parentPath;
    int lastSlash = safePath.lastIndexOf('/');
    if (lastSlash <= 0) {
      parentPath = "/";
    } else {
      parentPath = safePath.substring(0, lastSlash);
    }

    // Ищем только папки на глубине 1, обрезаем скрытые
    String cmd = LinuxCommands.findDirectories(safePath);
    String output = commandExecutor.execute(sessionId, cmd);
    List<DirectoryItemDto> items = new ArrayList<>();

    if (output != null && !output.isBlank()) {
      String[] lines = output.split("\\r?\\n");
      for (String line : lines) {
        String fullPath = line.trim();
        if (fullPath.isEmpty()) continue;

        // В bash это маловероятно, но безопасным будет использовать '/' вместо File.separator
        String name = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        items.add(new DirectoryItemDto(name, fullPath));
      }
    }

    return new DirectoryBrowseResultDto(safePath, parentPath, items);
  }

  /** Создает новую папку по указанному пути */
  public void createDirectory(String sessionId, String parentPath, String dirName)
      throws Exception {
    String safeParent = normalizePath(parentPath);
    requireAllowedPath(safeParent);
    String cleanName = dirName.trim();

    if (!cleanName.matches("^[a-zA-Z0-9._-]+$")) {
      throw new IllegalArgumentException("Имя папки содержит недопустимые символы");
    }

    String fullPath =
        safeParent.endsWith("/") ? (safeParent + cleanName) : (safeParent + "/" + cleanName);

    // We already check safeParent, but checking fullPath guarantees it
    requireAllowedPath(fullPath);

    commandExecutor.execute(sessionId, LinuxCommands.mkdir(fullPath));
    commandExecutor.execute(sessionId, LinuxCommands.chmod("0775", fullPath));
  }

  /** Приводит путь к формату Linux (это нужно для запуска на Windows), удаляет двойные слеши. */
  private String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }

    // 1. Заменяем возможные виндовые слеши, если кто-то послал
    String unixPath = path.trim().replace("\\", "/");

    // 2. Убираем лишние слеши (например, /srv//samba -> /srv/samba)
    unixPath = unixPath.replaceAll("/+", "/");

    // 3. Гарантируем, что путь начинается с корня (абсолютный путь)
    return unixPath.startsWith("/") ? unixPath : "/" + unixPath;
  }

  /** Enforces a directory jail to prevent Arbitrary File Read/Write across system files. */
  private void requireAllowedPath(String path) {
    for (String forbidden : FORBIDDEN_ROOTS) {
      if (path.equals(forbidden) || path.startsWith(forbidden + "/")) {
        throw new SecurityException(
            "Security Policy: Access to system directory '"
                + forbidden
                + "' is strictly forbidden.");
      }
    }
  }

  /** Запрашивает информацию о пространстве жестких дисков по пути */
  public DiskUsageDto getDiskUsage(String sessionId, String path) throws Exception {
    String safePath = normalizePath(path);
    requireAllowedPath(safePath);

    String cmd = LinuxCommands.df(safePath);
    String output = commandExecutor.execute(sessionId, cmd);

    if (output == null || output.isBlank()) {
      throw new RuntimeException("Пустой ответ от df для пути " + path);
    }

    // Парсим вторую строчку
    String[] lines = output.trim().split("\\r?\\n");
    if (lines.length < 2) {
      throw new RuntimeException("Не удалось распарсить df: " + output);
    }

    // Берем последнюю строчку (в случаях длинных маунтов Filesystem, 1024-blocks и т.д.)
    String dataLine = lines[lines.length - 1];
    String[] parts = dataLine.trim().split("\\s+");

    if (parts.length >= 6) {
      try {
        long totalKb = Long.parseLong(parts[1]);
        long usedKb = Long.parseLong(parts[2]);
        long availKb = Long.parseLong(parts[3]);
        String capacityStr = parts[4].replace("%", "");
        int usePercent = Integer.parseInt(capacityStr);
        String mountPoint = parts[5];

        return new DiskUsageDto(
            safePath,
            formatSize(totalKb * 1024L),
            formatSize(usedKb * 1024L),
            formatSize(availKb * 1024L),
            usePercent,
            mountPoint);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Ошибка парсинга чисел в выводе df: " + dataLine);
      }
    }

    throw new RuntimeException("Неожиданный формат ответа парсинга: " + dataLine);
  }

  /** Преобразует байты в удобочитаемые KB, MB, GB, TB */
  private String formatSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "B";
    return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre).replace(",", ".");
  }
}
