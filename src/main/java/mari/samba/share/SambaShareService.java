package mari.samba.share;

import java.util.List;
import java.util.Optional;
import mari.samba.filesystem.FileSystemService;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import mari.samba.smbconfig.SambaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class SambaShareService {

    private static final Logger log = LoggerFactory.getLogger(SambaShareService.class);

    private final SambaConfigService configService;
    private final CommandExecutor commandExecutor;
    private final FileSystemService fileSystemService;

    SambaShareService(
            SambaConfigService configService, CommandExecutor commandExecutor, FileSystemService fileSystemService) {
        this.configService = configService;
        this.commandExecutor = commandExecutor;
        this.fileSystemService = fileSystemService;
    }

    List<SambaShare> getAllShares(String sessionId) {
        String content = configService.getSmbConfContent(sessionId);
        return configService.parseShares(content);
    }

    SambaShare getShareByName(String sessionId, String name) {
        return findShareByName(sessionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + name));
    }

    Optional<SambaShare> findShareByName(String sessionId, String name) {
        List<SambaShare> shares = getAllShares(sessionId);
        return shares.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    void createShare(String sessionId, SambaShareCreateDto dto) {
        List<SambaShare> existingShares = getAllShares(sessionId);
        boolean exists = existingShares.stream().anyMatch(s -> s.getName().equalsIgnoreCase(dto.getName()));

        if (exists) {
            throw new IllegalArgumentException("Share already exists: " + dto.getName());
        }

        ensureDirectoryExists(sessionId, dto);

        String shareSection = configService.buildShareSection(dto);
        String currentContent = configService.getSmbConfContent(sessionId);
        String newContent = currentContent + "\n" + shareSection;

        configService.updateSmbConf(sessionId, newContent);
        log.info("Created Samba share: {}", dto.getName());
    }

    void updateShare(String sessionId, String name, SambaShareCreateDto dto) {
        SambaShareCreateDto effectiveDto =
                dto.getName() == null || !dto.getName().equalsIgnoreCase(name) ? dto.withName(name) : dto;

        ensureDirectoryExists(sessionId, effectiveDto);

        String content = configService.getSmbConfContent(sessionId);
        String updatedContent = configService.removeShareSection(content, name);
        String newSection = configService.buildShareSection(effectiveDto);
        updatedContent = updatedContent + "\n" + newSection;

        configService.updateSmbConf(sessionId, updatedContent);
        log.info("Updated Samba share: {}", name);
    }

    String getShareSize(String sessionId, String name) {
        SambaShare share = getShareByName(sessionId, name);
        String path = share.getPath();
        if (path == null || path.isBlank()) {
            return "N/A";
        }
        String output = commandExecutor.execute(sessionId, LinuxCommands.du(path));
        if (output != null && !output.isBlank()) {
            String[] parts = output.trim().split("\\s+");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "0";
    }

    void deleteShare(String sessionId, String name) {
        String content = configService.getSmbConfContent(sessionId);
        String updatedContent = configService.removeShareSection(content, name);
        configService.updateSmbConf(sessionId, updatedContent);
        log.info("Deleted Samba share: {}", name);
    }

    private void ensureDirectoryExists(String sessionId, SambaShareCreateDto dto) {
        String path = dto.getPath().trim();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }

        fileSystemService.requireAllowedPath(path);

        commandExecutor.execute(sessionId, LinuxCommands.mkdir(path));

        String permissions = resolveDirectoryPermissions(dto);
        commandExecutor.execute(sessionId, LinuxCommands.chmod(permissions, path));

        String owner = resolveOwner(dto);
        if (owner != null && !owner.isBlank()) {
            commandExecutor.execute(sessionId, LinuxCommands.chownRecursive(owner, path));
        }
    }

    private String resolveDirectoryPermissions(SambaShareCreateDto dto) {
        if (dto.getDirectoryMask() != null && !dto.getDirectoryMask().isBlank()) {
            return dto.getDirectoryMask().trim();
        }
        if (dto.isGuestOk() && !dto.isReadOnly()) {
            return "0777";
        }
        return dto.isReadOnly() ? "0755" : "0775";
    }

    private String resolveOwner(SambaShareCreateDto dto) {
        String user = (dto.getForceUser() != null && !dto.getForceUser().isBlank())
                ? dto.getForceUser().trim()
                : null;
        String group = (dto.getForceGroup() != null && !dto.getForceGroup().isBlank())
                ? dto.getForceGroup().trim()
                : null;

        if (user == null && dto.getValidUsers() != null && !dto.getValidUsers().isBlank()) {
            String firstUser = dto.getValidUsers().split(",")[0].trim();
            if (!firstUser.startsWith("@")) {
                user = firstUser;
            } else if (group == null) {
                group = firstUser.substring(1);
            }
        }

        if (user != null && group != null) return user + ":" + group;
        if (user != null) return user;
        if (group != null) return ":" + group;
        return null;
    }
}
