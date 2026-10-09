import re

with open('src/main/java/mari/samba/filesystem/FileSystemApiController.java', 'r', encoding='utf-8') as f:
    content = f.read()

copy_move = '''
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
'''

content = content.replace('public class FileSystemApiController {', 'public class FileSystemApiController {' + copy_move)

with open('src/main/java/mari/samba/filesystem/FileSystemApiController.java', 'w', encoding='utf-8') as f:
    f.write(content)

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'r', encoding='utf-8') as f:
    content = f.read()

copy_move_svc = '''
    public void copyItem(@NonNull String sessionId, @NonNull String source, @NonNull String destination) {
        requireAllowedPath(normalizePath(source));
        requireAllowedPath(normalizePath(destination));
        String command = String.format("sudo cp -r %s %s", mari.samba.infra.LinuxCommands.escape(source), mari.samba.infra.LinuxCommands.escape(destination));
        commandExecutor.execute(sessionId, command);
    }

    public void moveItem(@NonNull String sessionId, @NonNull String source, @NonNull String destination) {
        requireAllowedPath(normalizePath(source));
        requireAllowedPath(normalizePath(destination));
        String command = String.format("sudo mv %s %s", mari.samba.infra.LinuxCommands.escape(source), mari.samba.infra.LinuxCommands.escape(destination));
        commandExecutor.execute(sessionId, command);
    }

    public void deleteItem(@NonNull String sessionId, @NonNull String target) {
        requireAllowedPath(normalizePath(target));
        String command = String.format("sudo rm -rf %s", mari.samba.infra.LinuxCommands.escape(target));
        commandExecutor.execute(sessionId, command);
    }
'''

content = content.replace('public class FileSystemService {', 'public class FileSystemService {' + copy_move_svc)

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'w', encoding='utf-8') as f:
    f.write(content)

