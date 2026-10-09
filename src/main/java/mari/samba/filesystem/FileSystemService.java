package mari.samba.filesystem;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import mari.samba.config.SambaProperties;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class FileSystemService {
    public void copyItem(@NonNull String sessionId, @NonNull String source, @NonNull String destination) {
        requireAllowedPath(normalizePath(source));
        requireAllowedPath(normalizePath(destination));
        String command = String.format(
                "sudo cp -r %s %s",
                mari.samba.infra.LinuxCommands.escape(source), mari.samba.infra.LinuxCommands.escape(destination));
        commandExecutor.execute(sessionId, command);
    }

    public void moveItem(@NonNull String sessionId, @NonNull String source, @NonNull String destination) {
        requireAllowedPath(normalizePath(source));
        requireAllowedPath(normalizePath(destination));
        String command = String.format(
                "sudo mv %s %s",
                mari.samba.infra.LinuxCommands.escape(source), mari.samba.infra.LinuxCommands.escape(destination));
        commandExecutor.execute(sessionId, command);
    }

    public void deleteItem(@NonNull String sessionId, @NonNull String target) {
        requireAllowedPath(normalizePath(target));
        String command = String.format("sudo rm -rf %s", mari.samba.infra.LinuxCommands.escape(target));
        commandExecutor.execute(sessionId, command);
    }

    public void uploadFile(
            @NonNull String sessionId,
            @NonNull String directory,
            @NonNull String filename,
            @NonNull String base64Content) {
        String safeParent = normalizePath(directory);
        requireAllowedPath(safeParent);
        String targetPath = safeParent.endsWith("/") ? safeParent + filename : safeParent + "/" + filename;
        requireAllowedPath(targetPath);

        String command = String.format(
                "echo '%s' | base64 -d | sudo tee %s > /dev/null",
                base64Content, mari.samba.infra.LinuxCommands.escape(targetPath));
        commandExecutor.execute(sessionId, command);
    }

    private static final Logger log = LoggerFactory.getLogger(FileSystemService.class);
    private static final Pattern SAFE_DIR_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final CommandExecutor commandExecutor;
    private final List<String> allowedRoots;

    public FileSystemService(CommandExecutor commandExecutor, SambaProperties properties) {
        this.commandExecutor = commandExecutor;
        this.allowedRoots = properties.security().allowedRoots();
    }

    /** Lists subdirectories within a requested path, returning normalized structure. */
    public DirectoryBrowseResultDto listDirectories(@NonNull String sessionId, String requestedPath) {
        String safePath = normalizePath(requestedPath);
        requireAllowedPath(safePath);

        if ("/".equals(safePath)) {
            List<DirectoryItemDto> rootDirs = allowedRoots.stream()
                    .map(root -> new DirectoryItemDto(root.replaceFirst("^/", ""), root, "dir", 0))
                    .toList();
            return new DirectoryBrowseResultDto("/", "/", rootDirs);
        }

        String parentPath = getParentPath(safePath);
        String rawOutput = commandExecutor.execute(sessionId, LinuxCommands.findDirectories(safePath));

        List<DirectoryItemDto> subDirs = Arrays.stream(rawOutput.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length < 3) return null;
                    String typeCode = parts[0];
                    long size = 0;
                    try {
                        size = Long.parseLong(parts[1]);
                    } catch (Exception ignored) {
                    }
                    String fullPath = parts[2];
                    String type = "d".equals(typeCode) ? "dir" : ("f".equals(typeCode) ? "file" : "link");
                    String dirName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                    return new DirectoryItemDto(dirName, fullPath, type, size);
                })
                .filter(item -> item != null)
                .toList();

        return new DirectoryBrowseResultDto(safePath, parentPath, subDirs);
    }

    /** Creates a directory under the given parent directory with secure permissions. */
    public void createDirectory(@NonNull String sessionId, @NonNull String parentPath, @NonNull String name) {
        if (!SAFE_DIR_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid directory name. Only alphanumeric characters, dashes, underscores, and dots are"
                            + " allowed.");
        }

        String safeParent = normalizePath(parentPath);
        requireAllowedPath(safeParent);

        String newDirPath = safeParent.endsWith("/") ? safeParent + name : safeParent + "/" + name;
        requireAllowedPath(newDirPath);

        commandExecutor.execute(sessionId, LinuxCommands.mkdir(newDirPath));
        commandExecutor.execute(sessionId, LinuxCommands.chmod("0775", newDirPath));
        log.info("Created directory {} with permissions 0775", newDirPath);
    }

    /** Retrieves disk usage information for a specified path. */
    public DiskUsageDto getDiskUsage(@NonNull String sessionId, String requestedPath) {
        String safePath = normalizePath(requestedPath);
        requireAllowedPath(safePath);

        String rawOutput = commandExecutor.execute(sessionId, LinuxCommands.df(safePath));
        if (rawOutput == null || rawOutput.isBlank()) {
            throw new RuntimeException("Empty output from df command for path: " + safePath);
        }

        String[] lines = rawOutput.trim().split("\\r?\\n");
        if (lines.length < 2) {
            throw new RuntimeException("Invalid df output format: " + rawOutput);
        }

        String[] parts = lines[1].trim().split("\\s+");
        if (parts.length < 6) {
            throw new RuntimeException("Unexpected df line structure: " + lines[1]);
        }

        try {
            long totalKb = Long.parseLong(parts[1]);
            long usedKb = Long.parseLong(parts[2]);
            long availKb = Long.parseLong(parts[3]);
            int usePercent = Integer.parseInt(parts[4].replace("%", ""));
            String mountPoint = parts[5];

            return new DiskUsageDto(
                    safePath,
                    formatBytes(totalKb * 1024),
                    formatBytes(usedKb * 1024),
                    formatBytes(availKb * 1024),
                    usePercent,
                    mountPoint);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse disk usage numbers from: " + lines[1], e);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public void requireAllowedPath(String path) {
        if (path == null || "/".equals(path)) {
            return;
        }
        boolean allowed = allowedRoots.stream().anyMatch(root -> path.equals(root) || path.startsWith(root + "/"));
        if (!allowed) {
            throw new SecurityException("Access to path '" + path + "' is not permitted.");
        }
    }

    private String getParentPath(String path) {
        if ("/".equals(path)) {
            return "/";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
