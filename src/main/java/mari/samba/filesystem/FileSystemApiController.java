package mari.samba.filesystem;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Base64;
import mari.samba.core.ApiResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for performing low-level remote file system operations.
 *
 * <p>Provides endpoints to browse standard server directories, calculate disk usage per directory,
 * and provision new directories specifically for Samba shares.
 */
@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {
    @PostMapping("/copy")
    public ApiResponse<Void> copyItem(@NonNull HttpSession session, @RequestBody java.util.Map<String, String> body) {
        String sessionId = session.getId();
        String source = body.get("source");
        String destination = body.get("destination");
        fileSystemService.copyItem(sessionId, source, destination);
        return ApiResponse.ok("Copied successfully", null);
    }

    @PostMapping("/move")
    public ApiResponse<Void> moveItem(@NonNull HttpSession session, @RequestBody java.util.Map<String, String> body) {
        String sessionId = session.getId();
        String source = body.get("source");
        String destination = body.get("destination");
        fileSystemService.moveItem(sessionId, source, destination);
        return ApiResponse.ok("Moved successfully", null);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteItem(@NonNull HttpSession session, @RequestBody java.util.Map<String, String> body) {
        String sessionId = session.getId();
        String target = body.get("target");
        fileSystemService.deleteItem(sessionId, target);
        return ApiResponse.ok("Deleted successfully", null);
    }

    @PostMapping("/upload")
    public ApiResponse<Void> uploadFile(
            @NonNull HttpSession session, @RequestParam("path") String path, @RequestParam("file") MultipartFile file)
            throws Exception {
        String sessionId = session.getId();
        byte[] content = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(content);
        fileSystemService.uploadFile(sessionId, path, file.getOriginalFilename(), base64Content);
        return ApiResponse.ok("File uploaded", null);
    }

    private final FileSystemService fileSystemService;

    /**
     * Constructs a new {@link FileSystemApiController}.
     *
     * @param fileSystemService the service handling OS directory structures
     */
    public FileSystemApiController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    /**
     * Explores the remote server file system starting from a specific path. Only folders are
     * retrieved (files are typically omitted for Samba config purposes).
     *
     * @param session the user's current HTTP session referencing the remote server
     * @param path the absolute query path on the remote host (defaults to root "/")
     * @return an {@link ApiResponse} wrapping the target directory hierarchy info
     */
    @GetMapping("/browse")
    public ApiResponse<DirectoryBrowseResultDto> browse(
            @NonNull HttpSession session, @RequestParam(value = "path", defaultValue = "/") String path) {
        String sessionId = session.getId();
        DirectoryBrowseResultDto result = fileSystemService.listDirectories(sessionId, path);
        return ApiResponse.ok(result);
    }

    /**
     * Provisions a brand-new directory on the remote file system. Accepts configuration either via a
     * RequestParam structure or a full JSON RequestBody.
     *
     * @param session the user's current HTTP session
     * @param parentPathParam the parent folder where the new folder will reside (RequestParam
     *     fallback)
     * @param nameParam the exact localized name of the new folder (RequestParam fallback)
     * @param bodyDto the structured request body payload containing creation info
     * @return an {@link ApiResponse} representing operation success
     * @throws IllegalArgumentException if both payload mechanisms failed to define path and name
     */
    @PostMapping("/mkdir")
    public ApiResponse<Void> createDirectory(
            @NonNull HttpSession session,
            @RequestParam(value = "parentPath", required = false) String parentPathParam,
            @RequestParam(value = "name", required = false) String nameParam,
            @Valid @RequestBody(required = false) CreateDirectoryDto bodyDto) {
        String sessionId = session.getId();
        String parent = bodyDto != null ? bodyDto.parentPath() : parentPathParam;
        String name = bodyDto != null ? bodyDto.dirName() : nameParam;

        if (parent == null || name == null) {
            throw new IllegalArgumentException("Parent path and directory name are required.");
        }

        fileSystemService.createDirectory(sessionId, parent, name);
        return ApiResponse.ok("Directory created successfully", null);
    }

    /**
     * Interrogates the operating system to retrieve current disk usage of a specific folder line.
     *
     * @param session the user's current HTTP session
     * @param path the absolute location on disk to scan
     * @return an {@link ApiResponse} wrapping standard statistics like size and percent utilized
     */
    @GetMapping({"/disk-usage", "/usage"})
    public ApiResponse<DiskUsageDto> getDiskUsage(@NonNull HttpSession session, @RequestParam("path") String path) {
        String sessionId = session.getId();
        DiskUsageDto usage = fileSystemService.getDiskUsage(sessionId, path);
        return ApiResponse.ok(usage);
    }
}
