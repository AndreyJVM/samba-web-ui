import re

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_parser = '''        List<DirectoryItemDto> subDirs = Arrays.stream(rawOutput.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.equals(safePath))
                .filter(line -> !line.contains("/."))
                .map(fullPath -> {
                    String dirName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                    return new DirectoryItemDto(dirName, fullPath);
                })
                .toList();'''

new_parser = '''        List<DirectoryItemDto> subDirs = Arrays.stream(rawOutput.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length < 3) return null;
                    String typeCode = parts[0];
                    long size = 0;
                    try { size = Long.parseLong(parts[1]); } catch(Exception ignored) {}
                    String fullPath = parts[2];
                    String type = "d".equals(typeCode) ? "dir" : ("f".equals(typeCode) ? "file" : "link");
                    String dirName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                    return new DirectoryItemDto(dirName, fullPath, type, size);
                })
                .filter(item -> item != null)
                .toList();'''

# also the rootDirs logic for "/"
old_root = '''        if ("/".equals(safePath)) {
            List<DirectoryItemDto> rootDirs = allowedRoots.stream()
                    .map(root -> new DirectoryItemDto(root.replaceFirst("^/", ""), root))
                    .toList();
            return new DirectoryBrowseResultDto("/", "/", rootDirs);
        }'''

new_root = '''        if ("/".equals(safePath)) {
            List<DirectoryItemDto> rootDirs = allowedRoots.stream()
                    .map(root -> new DirectoryItemDto(root.replaceFirst("^/", ""), root, "dir", 0))
                    .toList();
            return new DirectoryBrowseResultDto("/", "/", rootDirs);
        }'''

content = content.replace(old_parser, new_parser)
content = content.replace(old_root, new_root)

with open('src/main/java/mari/samba/filesystem/FileSystemService.java', 'w', encoding='utf-8') as f:
    f.write(content)
