import re

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'r', encoding='utf-8') as f:
    content = f.read()

upload_method = '''
    public void uploadFile(@NonNull String sessionId, @NonNull String directory, @NonNull String filename, @NonNull String base64Content) {
        String safeParent = normalizePath(directory);
        requireAllowedPath(safeParent);
        String targetPath = safeParent.endsWith("/") ? safeParent + filename : safeParent + "/" + filename;
        requireAllowedPath(targetPath);
        
        String command = String.format("echo '%s' | base64 -d | sudo tee %s > /dev/null", base64Content, mari.samba.infra.LinuxCommands.escape(targetPath));
        commandExecutor.execute(sessionId, command);
    }
'''

content = content.replace('public class FileSystemService {\n', 'public class FileSystemService {\n' + upload_method)

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'w', encoding='utf-8') as f:
    f.write(content)
