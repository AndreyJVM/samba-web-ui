import re

with open('src/main/java/mari/samba/filesystem/FileSystemApiController.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import org.springframework.web.bind.annotation.*;', 'import org.springframework.web.bind.annotation.*;\nimport org.springframework.web.multipart.MultipartFile;\nimport java.util.Base64;')

upload_endpoint = '''
    @PostMapping("/upload")
    public ApiResponse<Void> uploadFile(
            @NonNull HttpSession session, 
            @RequestParam("path") String path, 
            @RequestParam("file") MultipartFile file) throws Exception {
        String sessionId = session.getId();
        byte[] content = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(content);
        fileSystemService.uploadFile(sessionId, path, file.getOriginalFilename(), base64Content);
        return ApiResponse.ok("File uploaded", null);
    }
'''

content = content.replace('public class FileSystemApiController {\n', 'public class FileSystemApiController {\n' + upload_endpoint)

with open('src/main/java/mari/samba/filesystem/FileSystemApiController.java', 'w', encoding='utf-8') as f:
    f.write(content)
