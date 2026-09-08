package mari.samba.service;

import mari.samba.dto.DirectoryBrowseResultDto;
import mari.samba.dto.DirectoryItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FileSystemService {

    @Autowired
    private CommandExecutor commandExecutor;

    /**
     * Получение списка поддиректорий по указанному пути
     */
    public DirectoryBrowseResultDto listDirectories(String sessionId, String requestedPath) throws Exception {
        String safePath = normalizePath(requestedPath);

        // Определяем родительский каталог
        Path pathObj = Paths.get(safePath);
        Path parentObj = pathObj.getParent();
        String parentPath = parentObj != null ? parentObj.toString() : "/";

        // Читаем только папки с глубиной 1, исключая скрытые
        String escapedPath = safePath.replace("'", "'\\''");
        String cmd = String.format("sudo find '%s' -mindepth 1 -maxdepth 1 -type d ! -name '.*' 2>/dev/null | sort", escapedPath);

        String output = commandExecutor.execute(sessionId, cmd);
        List<DirectoryItemDto> items = new ArrayList<>();

        if (output != null && !output.isBlank()) {
            String[] lines = output.split("\\r?\\n");
            for (String line : lines) {
                String fullPath = line.trim();
                if (fullPath.isEmpty()) continue;

                String name = fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
                items.add(new DirectoryItemDto(name, fullPath));
            }
        }

        return new DirectoryBrowseResultDto(safePath, parentPath, items);
    }

    /**
     * Создание новой директории внутри выбранного пути
     */
    public void createDirectory(String sessionId, String parentPath, String dirName) throws Exception {
        String safeParent = normalizePath(parentPath);
        String cleanName = dirName.trim();

        if (!cleanName.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Имя папки содержит недопустимые символы");
        }

        String fullPath = safeParent.endsWith("/") ? (safeParent + cleanName) : (safeParent + "/" + cleanName);
        String escaped = fullPath.replace("'", "'\\''");

        commandExecutor.execute(sessionId, "sudo mkdir -p '" + escaped + "'");
        commandExecutor.execute(sessionId, "sudo chmod 0775 '" + escaped + "'");
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        Path normalized = Paths.get(path.trim()).normalize();
        String res = normalized.toString();
        return res.startsWith("/") ? res : "/" + res;
    }
}