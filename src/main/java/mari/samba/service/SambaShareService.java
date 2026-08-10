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

    /**
     * Получить список всех шар
     */
    public List<SambaShare> getAllShares(Session session) throws Exception {
        String content = configService.getSmbConfContent(session);
        return configService.parseShares(content);
    }

    /**
     * Найти шару по имени
     */
    public SambaShare getShareByName(Session session, String name) throws Exception {
        List<SambaShare> shares = getAllShares(session);
        return shares.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Share not found: " + name));
    }

    /**
     * Создать новую шару
     */
    public void createShare(Session session, SambaShareCreateDto dto) throws Exception {
        // 1. Проверяем, что шара с таким именем не существует
        List<SambaShare> existingShares = getAllShares(session);
        boolean exists = existingShares.stream()
                .anyMatch(s -> s.getName().equals(dto.getName()));
        if (exists) {
            throw new RuntimeException("Share with name '" + dto.getName() + "' already exists");
        }

        // 2. Формируем секцию для smb.conf
        String shareSection = buildShareSection(dto);

        // 3. Добавляем в конец файла
        String currentContent = configService.getSmbConfContent(session);
        String newContent = currentContent + "\n" + shareSection;

        // 4. Обновляем конфиг
        configService.updateSmbConf(session, newContent);
    }

    /**
     * Обновить существующую шару
     */
    public void updateShare(Session session, String name, SambaShareCreateDto dto) throws Exception {
        // 1. Получаем текущий контент
        String content = configService.getSmbConfContent(session);

        // 2. Удаляем старую секцию
        String updatedContent = removeShareSection(content, name);

        // 3. Добавляем новую секцию (с новыми параметрами)
        String newSection = buildShareSection(dto);
        updatedContent = updatedContent + "\n" + newSection;

        // 4. Сохраняем
        configService.updateSmbConf(session, updatedContent);
    }

    /**
     * Удалить шару
     */
    public void deleteShare(Session session, String name) throws Exception {
        String content = configService.getSmbConfContent(session);
        String updatedContent = removeShareSection(content, name);
        configService.updateSmbConf(session, updatedContent);
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    /**
     * Построить секцию для smb.conf из DTO
     */
    private String buildShareSection(SambaShareCreateDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(dto.getName()).append("]\n");
        sb.append("   path = ").append(dto.getPath()).append("\n");

        if (dto.getComment() != null && !dto.getComment().isEmpty()) {
            sb.append("   comment = ").append(dto.getComment()).append("\n");
        }

        sb.append("   read only = ").append(dto.isReadOnly() ? "yes" : "no").append("\n");
        sb.append("   guest ok = ").append(dto.isGuestOk() ? "yes" : "no").append("\n");
        sb.append("   browseable = yes\n");

        if (dto.getValidUsers() != null && !dto.getValidUsers().isEmpty()) {
            sb.append("   valid users = ").append(dto.getValidUsers()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Удалить секцию из конфига по имени
     */
    private String removeShareSection(String content, String shareName) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean insideShare = false;
        boolean shareFound = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Проверяем начало секции
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String sectionName = trimmed.substring(1, trimmed.length() - 1);
                if (sectionName.equals(shareName)) {
                    insideShare = true;
                    shareFound = true;
                    continue;
                } else {
                    insideShare = false;
                }
            }

            // Пропускаем строки внутри удаляемой секции
            if (insideShare) {
                continue;
            }

            // Добавляем строки, которые не в удаляемой секции
            result.append(line).append("\n");
        }

        if (!shareFound) {
            throw new RuntimeException("Share not found: " + shareName);
        }

        return result.toString();
    }
}