package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.FileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {

  @Autowired private FileSystemService fileSystemService;

  @GetMapping("/browse")
  public ApiResponse<DirectoryBrowseResultDto> browseDirectories(
      @RequestParam(defaultValue = "/") String path, HttpSession httpSession) throws Exception {
    DirectoryBrowseResultDto result = fileSystemService.listDirectories(httpSession.getId(), path);
    return ApiResponse.ok(result);
  }

  @PostMapping("/mkdir")
  public ApiResponse<Void> makeDirectory(
      @RequestParam String parentPath, @RequestParam String name, HttpSession httpSession)
      throws Exception {
    fileSystemService.createDirectory(httpSession.getId(), parentPath, name);
    return ApiResponse.ok("Каталог успешно создан", null);
  }

  @GetMapping("/disk-usage")
  public ApiResponse<DiskUsageDto> getDiskUsage(@RequestParam String path, HttpSession httpSession)
      throws Exception {
    DiskUsageDto result = fileSystemService.getDiskUsage(httpSession.getId(), path);
    return ApiResponse.ok(result);
  }
}
