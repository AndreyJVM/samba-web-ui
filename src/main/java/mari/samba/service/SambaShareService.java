package mari.samba.service;

import com.jcraft.jsch.Session;
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
    private SshSessionManager sessionManager;

    public List<SambaShare> getAllShares(Session session) throws Exception {
        String content = configService.getSmbConfContent(session);
        return configService.parseShares(content);
    }

    public SambaShare getShareByName(Session session, String name) throws Exception {
        List<SambaShare> shares = getAllShares(session);
        return shares.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Шара не найдена: " + name));
    }

    public void createShare(Session session, SambaShareCreateDto dto) throws Exception {
        List<SambaShare> existingShares = getAllShares(session);
        boolean exists = existingShares.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(dto.getName()));

        if (exists) {
            throw new RuntimeException("Шара с именем '" + dto.getName() + "' уже существует");
        }

        // 1. Создаем директорию и выставляем права на сервере
        ensureDirectoryExists(session, dto);

        // 2. Генерируем конфигурацию и обновляем smb.conf
        String shareSection = configService.buildShareSection(dto);
        String currentContent = configService.getSmbConfContent(session);
        String newContent = currentContent + "\n" + shareSection;

        configService.updateSmbConf(session, newContent);
    }

    public void updateShare(Session session, String name, SambaShareCreateDto dto) throws Exception {
        // 1. Проверяем / создаем директорию при редактировании (если путь изменился)
        ensureDirectoryExists(session, dto);

        // 2. Обновляем конфигурацию в smb.conf
        String content = configService.getSmbConfContent(session);
        String updatedContent = configService.removeShareSection(content, name);
        String newSection = configService.buildShareSection(dto);
        updatedContent = updatedContent + "\n" + newSection;

        configService.updateSmbConf(session, updatedContent);
    }

    public void deleteShare(Session session, String name) throws Exception {
        String content = configService.getSmbConfContent(session);
        String updatedContent = configService.removeShareSection(content, name);
        configService.updateSmbConf(session, updatedContent);
    }

    /**
     * Создает директорию на удаленном сервере и задает права доступа (chmod/chown)
     */
    private void ensureDirectoryExists(Session session, SambaShareCreateDto dto) throws Exception {
        String path = dto.getPath().trim();

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Путь к шаре должен быть абсолютным (начинаться с /): " + path);
        }

        // Экранируем одинарные кавычки для безопасного выполнения в bash
        String safePath = path.replace("'", "'\\''");

        // 1. Создаем папку, если ее нет
        sessionManager.executeCommand(session, "sudo mkdir -p '" + safePath + "'");

        // 2. Определяем и выставляем права chmod
        String permissions = resolveDirectoryPermissions(dto);
        sessionManager.executeCommand(session, "sudo chmod " + permissions + " '" + safePath + "'");

        // 3. Выставляем владельца chown (если указаны force user / force group или valid users)
        String owner = resolveOwner(dto);
        if (owner != null && !owner.isBlank()) {
            sessionManager.executeCommand(session, "sudo chown -R " + owner + " '" + safePath + "'");
        }
    }

    /**
     * Определяет права доступа (chmod) на основе параметров шары
     */
    private String resolveDirectoryPermissions(SambaShareCreateDto dto) {
        if (dto.getDirectoryMask() != null && !dto.getDirectoryMask().isBlank()) {
            return dto.getDirectoryMask().trim();
        }
        if (dto.isGuestOk() && !dto.isReadOnly()) {
            return "0777"; // Гостевая шара с записью
        }
        if (dto.isReadOnly()) {
            return "0755";
        }
        return "0775"; // Полноправный доступ для группы
    }

    /**
     * Определяет владельца и группу для chown
     */
    private String resolveOwner(SambaShareCreateDto dto) {
        String user = (dto.getForceUser() != null && !dto.getForceUser().isBlank())
                ? dto.getForceUser().trim()
                : null;

        String group = (dto.getForceGroup() != null && !dto.getForceGroup().isBlank())
                ? dto.getForceGroup().trim()
                : null;

        // Если force user/group не указаны, но есть valid users — берем первого пользователя
        if (user == null && dto.getValidUsers() != null && !dto.getValidUsers().isBlank()) {
            String firstUser = dto.getValidUsers().split(",")[0].trim();
            if (!firstUser.startsWith("@")) {
                user = firstUser;
            } else if (group == null) {
                group = firstUser.substring(1);
            }
        }

        if (user != null && group != null) {
            return user + ":" + group;
        } else if (user != null) {
            return user;
        } else if (group != null) {
            return ":" + group;
        }

        return null;
    }
}