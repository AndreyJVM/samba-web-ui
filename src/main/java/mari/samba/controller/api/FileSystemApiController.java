package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.fs.CreateDirectoryDto;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.FileSystemService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {

  private final FileSystemService fileSystemService;

  public FileSystemApiController(FileSystemService fileSystemService) {
    this.fileSystemService = fileSystemService;
  }

  @GetMapping("/browse")
  public ApiResponse<DirectoryBrowseResultDto> browse(
      HttpSession session, @RequestParam(value = "path", defaultValue = "/") String path) {
    String sessionId = session.getId();
    DirectoryBrowseResultDto result = fileSystemService.listDirectories(sessionId, path);
    return ApiResponse.ok(result);
  }

  @PostMapping("/mkdir")
  public ApiResponse<Void> createDirectory(
      HttpSession session,
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

  @GetMapping({"/disk-usage", "/usage"})
  public ApiResponse<DiskUsageDto> getDiskUsage(
      HttpSession session, @RequestParam("path") String path) {
    String sessionId = session.getId();
    DiskUsageDto usage = fileSystemService.getDiskUsage(sessionId, path);
    return ApiResponse.ok(usage);
  }
}
