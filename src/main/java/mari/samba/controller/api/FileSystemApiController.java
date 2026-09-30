package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.fs.CreateDirectoryDto;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.FileSystemService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for performing low-level remote file system operations.
 *
 * <p>Provides endpoints to browse standard server directories, calculate disk usage per directory,
 * and provision new directories specifically for Samba shares.
 */
@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {

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
  public ApiResponse<DiskUsageDto> getDiskUsage(
      @NonNull HttpSession session, @RequestParam("path") String path) {
    String sessionId = session.getId();
    DiskUsageDto usage = fileSystemService.getDiskUsage(sessionId, path);
    return ApiResponse.ok(usage);
  }
}
