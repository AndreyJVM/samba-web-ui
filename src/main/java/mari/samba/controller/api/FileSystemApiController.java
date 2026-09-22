package mari.samba.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.FileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fs")
@Tag(
    name = "File System",
    description = "Operations for browsing and managing Linux directories via SSH")
public class FileSystemApiController {

  @Autowired private FileSystemService fileSystemService;

  @GetMapping("/browse")
  @Operation(
      summary = "Browse directories",
      description = "Lists subdirectories for a given path on the remote server.")
  public ResponseEntity<ApiResponse<DirectoryBrowseResultDto>> browseDirectories(
      @Parameter(description = "Absolute path to browse", example = "/mnt/data")
          @RequestParam(defaultValue = "/")
          String path,
      HttpSession httpSession) {
    try {
      DirectoryBrowseResultDto result =
          fileSystemService.listDirectories(httpSession.getId(), path);
      return ResponseEntity.ok(ApiResponse.ok(result));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @PostMapping("/mkdir")
  @Operation(
      summary = "Create directory",
      description = "Creates a new subdirectory inside the specified parent path.")
  public ResponseEntity<ApiResponse<Void>> makeDirectory(
      @Parameter(description = "Parent directory absolute path", example = "/mnt/data")
          @RequestParam
          String parentPath,
      @Parameter(description = "Name of the new directory", example = "new_folder") @RequestParam
          String name,
      HttpSession httpSession) {
    try {
      fileSystemService.createDirectory(httpSession.getId(), parentPath, name);
      return ResponseEntity.ok(ApiResponse.ok("Каталог успешно создан", null));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @GetMapping("/disk-usage")
  @Operation(
      summary = "Get disk usage",
      description =
          "Retrieves disk space usage statistics for the partition hosting the specified path.")
  public ResponseEntity<ApiResponse<DiskUsageDto>> getDiskUsage(
      @Parameter(description = "Path to check disk usage for", example = "/") @RequestParam
          String path,
      HttpSession httpSession) {
    try {
      DiskUsageDto result = fileSystemService.getDiskUsage(httpSession.getId(), path);
      return ResponseEntity.ok(ApiResponse.ok(result));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }
}
