package mari.samba.controller.api;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.common.ApiResponse;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.service.FileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {

    @Autowired
    private FileSystemService fileSystemService;

    @GetMapping("/browse")
    public ResponseEntity<ApiResponse<DirectoryBrowseResultDto>> browseDirectories(
            @RequestParam(defaultValue = "/") String path,
            HttpSession httpSession) {
        try {
            DirectoryBrowseResultDto result = fileSystemService.listDirectories(httpSession.getId(), path);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/mkdir")
    public ResponseEntity<ApiResponse<Void>> makeDirectory(
            @RequestParam String parentPath,
            @RequestParam String name,
            HttpSession httpSession) {
        try {
            fileSystemService.createDirectory(httpSession.getId(), parentPath, name);
            return ResponseEntity.ok(ApiResponse.ok("Каталог успешно создан", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}