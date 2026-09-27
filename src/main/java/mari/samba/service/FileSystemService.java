package mari.samba.service;

import java.util.ArrayList;
import java.util.List;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DirectoryItemDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileSystemService {

  @Autowired private CommandExecutor commandExecutor;

  // List of paths that are strictly forbidden to access via the web file explorer
  private static final String[] FORBIDDEN_ROOTS = {
    "/root",
    "/etc",
    "/boot",
    "/sbin",
    "/bin",
    "/dev",
    "/proc",
    "/sys",
    "/usr",
    "/var/lib",
    "/var/log"
  };

  /**
   * Р—Р°РїСЂР°С€РёРІР°РµС‚ СЃРїРёСЃРѕРє РІР»РѕР¶РµРЅРЅС‹С… РїР°РїРѕРє РІ СѓРєР°Р·Р°РЅРЅРѕРј
   * РїСѓС‚Рё
   */
  public DirectoryBrowseResultDto listDirectories(String sessionId, String requestedPath) {
    String safePath = normalizePath(requestedPath);
    requireAllowedPath(safePath);

    // Р’С‹С‡РёСЃР»СЏРµРј СЂРѕРґРёС‚РµР»СЊСЃРєСѓСЋ РґРёСЂРµРєС‚РѕСЂРёСЋ (java.nio.file.Paths С‚СѓС‚
    // РЅРµ РїРѕРґС…РѕРґРёС‚, С‚Р°Рє
    // РєР°Рє СЃРµСЂРІРµСЂ РјРѕР¶РµС‚ Р±С‹С‚СЊ РЅР° Windows)
    String parentPath;
    int lastSlash = safePath.lastIndexOf('/');
    if (lastSlash <= 0) {
      parentPath = "/";
    } else {
      parentPath = safePath.substring(0, lastSlash);
    }

    // РС‰РµРј С‚РѕР»СЊРєРѕ РїР°РїРєРё РЅР° РіР»СѓР±РёРЅРµ 1, РѕР±СЂРµР·Р°РµРј СЃРєСЂС‹С‚С‹Рµ
    String cmd = LinuxCommands.findDirectories(safePath);
    String output = commandExecutor.execute(sessionId, cmd);
    List<DirectoryItemDto> items = new ArrayList<>();

    if (output != null && !output.isBlank()) {
      String[] lines = output.split("\\r?\\n");
      for (String line : lines) {
        String fullPath = line.trim();
        if (fullPath.isEmpty()) continue;

        // Р’ bash СЌС‚Рѕ РјР°Р»РѕРІРµСЂРѕСЏС‚РЅРѕ, РЅРѕ Р±РµР·РѕРїР°СЃРЅС‹Рј Р±СѓРґРµС‚
        // РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ '/' РІРјРµСЃС‚Рѕ File.separator
        String name = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        items.add(new DirectoryItemDto(name, fullPath));
      }
    }

    return new DirectoryBrowseResultDto(safePath, parentPath, items);
  }

  /** РЎРѕР·РґР°РµС‚ РЅРѕРІСѓСЋ РїР°РїРєСѓ РїРѕ СѓРєР°Р·Р°РЅРЅРѕРјСѓ РїСѓС‚Рё */
  public void createDirectory(String sessionId, String parentPath, String dirName) {
    String safeParent = normalizePath(parentPath);
    requireAllowedPath(safeParent);
    String cleanName = dirName.trim();

    if (!cleanName.matches("^[a-zA-Z0-9._-]+$")) {
      throw new IllegalArgumentException(
          "РРјСЏ РїР°РїРєРё СЃРѕРґРµСЂР¶РёС‚ РЅРµРґРѕРїСѓСЃС‚РёРјС‹Рµ СЃРёРјРІРѕР»С‹");
    }

    String fullPath =
        safeParent.endsWith("/") ? (safeParent + cleanName) : (safeParent + "/" + cleanName);

    // We already check safeParent, but checking fullPath guarantees it
    requireAllowedPath(fullPath);

    commandExecutor.execute(sessionId, LinuxCommands.mkdir(fullPath));
    commandExecutor.execute(sessionId, LinuxCommands.chmod("0775", fullPath));
  }

  /**
   * РџСЂРёРІРѕРґРёС‚ РїСѓС‚СЊ Рє С„РѕСЂРјР°С‚Сѓ Linux (СЌС‚Рѕ РЅСѓР¶РЅРѕ РґР»СЏ Р·Р°РїСѓСЃРєР° РЅР°
   * Windows), СѓРґР°Р»СЏРµС‚ РґРІРѕР№РЅС‹Рµ СЃР»РµС€Рё.
   */
  private String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }

    // 1. Р—Р°РјРµРЅСЏРµРј РІРѕР·РјРѕР¶РЅС‹Рµ РІРёРЅРґРѕРІС‹Рµ СЃР»РµС€Рё, РµСЃР»Рё РєС‚Рѕ-С‚Рѕ
    // РїРѕСЃР»Р°Р»
    String unixPath = path.trim().replace("\\", "/");

    // 2. РЈР±РёСЂР°РµРј Р»РёС€РЅРёРµ СЃР»РµС€Рё (РЅР°РїСЂРёРјРµСЂ, /srv//samba -> /srv/samba)
    unixPath = unixPath.replaceAll("/+", "/");

    // 3. Р“Р°СЂР°РЅС‚РёСЂСѓРµРј, С‡С‚Рѕ РїСѓС‚СЊ РЅР°С‡РёРЅР°РµС‚СЃСЏ СЃ РєРѕСЂРЅСЏ
    // (Р°Р±СЃРѕР»СЋС‚РЅС‹Р№ РїСѓС‚СЊ)
    return unixPath.startsWith("/") ? unixPath : "/" + unixPath;
  }

  /** Enforces a directory jail to prevent Arbitrary File Read/Write across system files. */
  private void requireAllowedPath(String path) {
    for (String forbidden : FORBIDDEN_ROOTS) {
      if (path.equals(forbidden) || path.startsWith(forbidden + "/")) {
        throw new SecurityException(
            "Security Policy: Access to system directory '"
                + forbidden
                + "' is strictly forbidden.");
      }
    }
  }

  /**
   * Р—Р°РїСЂР°С€РёРІР°РµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ Рѕ РїСЂРѕСЃС‚СЂР°РЅСЃС‚РІРµ Р¶РµСЃС‚РєРёС…
   * РґРёСЃРєРѕРІ РїРѕ РїСѓС‚Рё
   */
  public DiskUsageDto getDiskUsage(String sessionId, String path) {
    String safePath = normalizePath(path);
    requireAllowedPath(safePath);

    String cmd = LinuxCommands.df(safePath);
    String output = commandExecutor.execute(sessionId, cmd);

    if (output == null || output.isBlank()) {
      throw new RuntimeException("РџСѓСЃС‚РѕР№ РѕС‚РІРµС‚ РѕС‚ df РґР»СЏ РїСѓС‚Рё " + path);
    }

    // РџР°СЂСЃРёРј РІС‚РѕСЂСѓСЋ СЃС‚СЂРѕС‡РєСѓ
    String[] lines = output.trim().split("\\r?\\n");
    if (lines.length < 2) {
      throw new RuntimeException("РќРµ СѓРґР°Р»РѕСЃСЊ СЂР°СЃРїР°СЂСЃРёС‚СЊ df: " + output);
    }

    // Р‘РµСЂРµРј РїРѕСЃР»РµРґРЅСЋСЋ СЃС‚СЂРѕС‡РєСѓ (РІ СЃР»СѓС‡Р°СЏС… РґР»РёРЅРЅС‹С… РјР°СѓРЅС‚РѕРІ
    // Filesystem, 1024-blocks Рё С‚.Рґ.)
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
            mountPoint);
      } catch (NumberFormatException e) {
        throw new RuntimeException(
            "РћС€РёР±РєР° РїР°СЂСЃРёРЅРіР° С‡РёСЃРµР» РІ РІС‹РІРѕРґРµ df: " + dataLine);
      }
    }

    throw new RuntimeException(
        "РќРµРѕР¶РёРґР°РЅРЅС‹Р№ С„РѕСЂРјР°С‚ РѕС‚РІРµС‚Р° РїР°СЂСЃРёРЅРіР°: " + dataLine);
  }

  /** РџСЂРµРѕР±СЂР°Р·СѓРµС‚ Р±Р°Р№С‚С‹ РІ СѓРґРѕР±РѕС‡РёС‚Р°РµРјС‹Рµ KB, MB, GB, TB */
  private String formatSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "B";
    return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre).replace(",", ".");
  }
}
