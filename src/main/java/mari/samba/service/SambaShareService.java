package mari.samba.service;

import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SambaShareService {

    @Autowired
    private SambaConfigService configService;

    @Autowired
    private CommandExecutor commandExecutor;

    public List<SambaShare> getAllShares(String sessionId) throws Exception {
        String content = configService.getSmbConfContent(sessionId);
        return configService.parseShares(content);
    }

    public SambaShare getShareByName(String sessionId, String name) throws Exception {
        List<SambaShare> shares = getAllShares(sessionId);
        return shares.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Шара не найдена: " + name));
    }

    public void createShare(String sessionId, SambaShareCreateDto dto) throws Exception {
        List<SambaShare> existingShares = getAllShares(sessionId);
        boolean exists = existingShares.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(dto.getName()));

        if (exists) {
            throw new RuntimeException("Шара с именем '" + dto.getName() + "' уже существует");
        }

        ensureDirectoryExists(sessionId, dto);

        String shareSection = configService.buildShareSection(dto);
        String currentContent = configService.getSmbConfContent(sessionId);
        String newContent = currentContent + "\n" + shareSection;

        configService.updateSmbConf(sessionId, newContent);
    }

    public void updateShare(String sessionId, String name, SambaShareCreateDto dto) throws Exception {
        ensureDirectoryExists(sessionId, dto);

        String content = configService.getSmbConfContent(sessionId);
        String updatedContent = configService.removeShareSection(content, name);
        String newSection = configService.buildShareSection(dto);
        updatedContent = updatedContent + "\n" + newSection;

        configService.updateSmbConf(sessionId, updatedContent);
    }

    public void deleteShare(String sessionId, String name) throws Exception {
        String content = configService.getSmbConfContent(sessionId);
        String updatedContent = configService.removeShareSection(content, name);
        configService.updateSmbConf(sessionId, updatedContent);
    }

    private void ensureDirectoryExists(String sessionId, SambaShareCreateDto dto) throws Exception {
        String path = dto.getPath().trim();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Путь к шаре должен быть абсолютным: " + path);
        }

        String safePath = path.replace("'", "'\\''");
        commandExecutor.execute(sessionId, "sudo mkdir -p '" + safePath + "'");

        String permissions = resolveDirectoryPermissions(dto);
        commandExecutor.execute(sessionId, "sudo chmod " + permissions + " '" + safePath + "'");

        String owner = resolveOwner(dto);
        if (owner != null && !owner.isBlank()) {
            commandExecutor.execute(sessionId, "sudo chown -R " + owner + " '" + safePath + "'");
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
        String user = (dto.getForceUser() != null && !dto.getForceUser().isBlank()) ? dto.getForceUser().trim() : null;
        String group = (dto.getForceGroup() != null && !dto.getForceGroup().isBlank()) ? dto.getForceGroup().trim() : null;

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