package mari.samba.controller;

import jakarta.servlet.http.HttpSession;
import mari.samba.dto.DirectoryBrowseResultDto;
import mari.samba.service.FileSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/fs")
public class FileSystemApiController {

    @Autowired
    private FileSystemService fileSystemService;

    @GetMapping("/browse")
    public ResponseEntity<?> browseDirectories(@RequestParam(defaultValue = "/") String path,
                                               HttpSession httpSession) {
        try {
            DirectoryBrowseResultDto result = fileSystemService.listDirectories(httpSession.getId(), path);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/mkdir")
    public ResponseEntity<?> makeDirectory(@RequestParam String parentPath,
                                           @RequestParam String name,
                                           HttpSession httpSession) {
        try {
            fileSystemService.createDirectory(httpSession.getId(), parentPath, name);
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}