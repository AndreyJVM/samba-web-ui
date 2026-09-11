package mari.samba.service;

import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DirectoryItemDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        String cmd = LinuxCommands.findDirectories(safePath);
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
        commandExecutor.execute(sessionId, LinuxCommands.mkdir(fullPath));
        commandExecutor.execute(sessionId, LinuxCommands.chmod("0775", fullPath));
    }

    /**
     * Нормализация путей строго для Linux (замена слешей и удаление дублей),
     * независимо от того, на какой ОС запущен сам Spring Boot.
     */
    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        // 1. Меняем виндовые слеши на линуксовые, если они вдруг есть
        String unixPath = path.trim().replace("\\", "/");

        // 2. Убираем двойные слеши (например, /srv//samba -> /srv/samba)
        unixPath = unixPath.replaceAll("/+", "/");

        // 3. Гарантируем, что путь начинается с корня (абсолютный путь)
        return unixPath.startsWith("/") ? unixPath : "/" + unixPath;
    }

    /**
     * Получение информации о свободном месте на диске по указанному пути
     */
    /**
     * Получение информации о свободном месте на диске по указанному пути
     */
    public DiskUsageDto getDiskUsage(String sessionId, String path) throws Exception {
        String safePath = normalizePath(path);

        String cmd = LinuxCommands.df(safePath);
        String output = commandExecutor.execute(sessionId, cmd);

        if (output == null || output.isBlank()) {
            throw new RuntimeException("Пустой ответ от df для пути " + path);
        }

        // Разбиваем вывод на строки
        String[] lines = output.trim().split("\\r?\\n");
        if (lines.length < 2) {
            throw new RuntimeException("Неожиданный вывод df: " + output);
        }

        // Берем последнюю строку (в первой строке идут заголовки Filesystem, 1024-blocks и т.д.)
        String dataLine = lines[lines.length - 1];
        String[] parts = dataLine.trim().split("\\s+");

        if (parts.length >= 6) {
            try {
                long totalKb = Long.parseLong(parts[1]);
                long usedKb = Long.parseLong(parts[2]);
                long availKb = Long.parseLong(parts[3]);
                String capacityStr = parts[4].replace("%", "");
                int usePercent = Integer.parseInt(capacityStr);
                String mountPoint = parts[5];

                return new DiskUsageDto(
                        safePath,
                        formatSize(totalKb * 1024L),
                        formatSize(usedKb * 1024L),
                        formatSize(availKb * 1024L),
                        usePercent,
                        mountPoint
                );
            } catch (NumberFormatException e) {
                throw new RuntimeException("Ошибка парсинга чисел из вывода df: " + dataLine);
            }
        }

        throw new RuntimeException("Некорректный формат строки данных: " + dataLine);
    }

    /**
     * Преобразование байтов в читаемый вид (KB, MB, GB, TB)
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre).replace(",", ".");
    }

}