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
  // List of root directories allowed for Samba shares
  private static final String[] ALLOWED_ROOTS = {"/mnt", "/media", "/srv", "/data", "/home"};

  /** Lists subdirectories within a requested path, returning normalized structure. */
  public DirectoryBrowseResultDto listDirectories(String sessionId, String requestedPath) {
    String safePath = normalizePath(requestedPath);
    requireAllowedPath(safePath);

    if (safePath.equals("/")) {
      List<DirectoryItemDto> items = new ArrayList<>();
      for (String root : ALLOWED_ROOTS) {
        items.add(new DirectoryItemDto(root.substring(1), root));
      }
      return new DirectoryBrowseResultDto("/", "/", items);
    }

    String parentPath;
    int lastSlash = safePath.lastIndexOf('/');
    if (lastSlash <= 0) {
      parentPath = "/";
    } else {
      parentPath = safePath.substring(0, lastSlash);
    }

    String cmd = LinuxCommands.findDirectories(safePath);
    String output = commandExecutor.execute(sessionId, cmd);
    List<DirectoryItemDto> items = new ArrayList<>();

    if (output != null && !output.isBlank()) {
      String[] lines = output.split("\\r?\\n");
      for (String line : lines) {
        String fullPath = line.trim();
        if (fullPath.isEmpty()) continue;
        if (fullPath.endsWith("/")) {
          fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.equals(safePath) || fullPath.isEmpty()) continue;

        int slash = fullPath.lastIndexOf('/');
        String name = slash >= 0 ? fullPath.substring(slash + 1) : fullPath;
        if (!name.isEmpty()) {
          items.add(new DirectoryItemDto(name, fullPath));
        }
      }
    }

    return new DirectoryBrowseResultDto(safePath, parentPath, items);
  }

  /** Creates a safe sub-directory in the requested parent path. */
  public void createDirectory(String sessionId, String parentPath, String dirName) {
    String safeParent = normalizePath(parentPath);
    requireAllowedPath(safeParent);
    String cleanName = dirName.trim();

    if (!cleanName.matches("^[a-zA-Z0-9._-]+$")) {
      throw new IllegalArgumentException("Directory name contains invalid characters");
    }

    String fullPath =
        safeParent.endsWith("/") ? (safeParent + cleanName) : (safeParent + "/" + cleanName);

    requireAllowedPath(fullPath);

    commandExecutor.execute(sessionId, LinuxCommands.mkdir(fullPath));
    commandExecutor.execute(sessionId, LinuxCommands.chmod("0775", fullPath));
  }

  /** Normalizes path to linux-style canonical absolute path avoiding arbitrary traversal. */
  private String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }

    String unixPath = path.trim().replace("\\", "/");
    unixPath = unixPath.replaceAll("/+", "/");
    if (unixPath.length() > 1 && unixPath.endsWith("/")) {
      unixPath = unixPath.substring(0, unixPath.length() - 1);
    }

    return unixPath.startsWith("/") ? unixPath : "/" + unixPath;
  }

  /** Enforces a directory jail to prevent Arbitrary File Read/Write across system files. */
  public void requireAllowedPath(String path) {
    if (path == null) return;
    if (path.equals("/")) {
      return;
    }
    boolean allowed = false;
    for (String root : ALLOWED_ROOTS) {
      if (path.equals(root) || path.startsWith(root + "/")) {
        allowed = true;
        break;
      }
    }
    if (!allowed) {
      throw new SecurityException(
          "Security Policy: Access to directory '"
              + path
              + "' is denied. Restricted to: "
              + String.join(", ", ALLOWED_ROOTS));
    }
  }

  /** Returns disk usage details for the requested path by calling 'df'. */
  public DiskUsageDto getDiskUsage(String sessionId, String path) {
    String safePath = normalizePath(path);
    requireAllowedPath(safePath);

    String cmd = LinuxCommands.df(safePath);
    String output = commandExecutor.execute(sessionId, cmd);

    if (output == null || output.isBlank()) {
      throw new RuntimeException("Empty output from df for path " + path);
    }

    String[] lines = output.trim().split("\\r?\\n");
    if (lines.length < 2) {
      throw new RuntimeException("Could not parse output from df: " + output);
    }

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
        throw new RuntimeException("Failed to parse numeric string in df output: " + dataLine);
      }
    }

    throw new RuntimeException("Invalid format string block in df output: " + dataLine);
  }

  /** Formats byte size to human readable equivalent */
  private String formatSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "B";
    return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre).replace(",", ".");
  }
}
