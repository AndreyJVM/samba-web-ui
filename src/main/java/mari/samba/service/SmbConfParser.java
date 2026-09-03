package mari.samba.service;

import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmbConfParser {

    /**
     * Парсинг сырого текста smb.conf в список моделей SambaShare
     */
    public List<SambaShare> parseShares(String content) {
        List<SambaShare> shares = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return shares;
        }

        String[] lines = content.split("\\r?\\n");
        SambaShare currentShare = null;
        boolean insideShare = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String sectionName = trimmed.substring(1, trimmed.length() - 1).trim();

                // Пропускаем служебные секции Samba
                if (!sectionName.equalsIgnoreCase("global") &&
                        !sectionName.equalsIgnoreCase("homes") &&
                        !sectionName.equalsIgnoreCase("printers")) {

                    if (currentShare != null && currentShare.getName() != null) {
                        shares.add(currentShare);
                    }
                    currentShare = new SambaShare();
                    currentShare.setName(sectionName);
                    insideShare = true;
                } else {
                    if (currentShare != null && currentShare.getName() != null) {
                        shares.add(currentShare);
                    }
                    insideShare = false;
                    currentShare = null;
                }
                continue;
            }

            if (insideShare && currentShare != null && trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                mapProperty(currentShare, key, value);
            }
        }

        if (currentShare != null && currentShare.getName() != null) {
            shares.add(currentShare);
        }

        return shares;
    }

    /**
     * Генерация INI-секции для конкретной шары
     */
    public String buildShareSection(SambaShareCreateDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(dto.getName()).append("]\n");
        sb.append("   path = ").append(dto.getPath()).append("\n");

        appendIfPresent(sb, "comment", dto.getComment());
        sb.append("   read only = ").append(dto.isReadOnly() ? "yes" : "no").append("\n");
        sb.append("   guest ok = ").append(dto.isGuestOk() ? "yes" : "no").append("\n");
        sb.append("   browseable = ").append(dto.isBrowseable() ? "yes" : "no").append("\n");

        appendIfPresent(sb, "valid users", dto.getValidUsers());
        appendIfPresent(sb, "write list", dto.getWriteList());
        appendIfPresent(sb, "create mask", dto.getCreateMask());
        appendIfPresent(sb, "directory mask", dto.getDirectoryMask());
        appendIfPresent(sb, "force user", dto.getForceUser());
        appendIfPresent(sb, "force group", dto.getForceGroup());
        appendIfPresent(sb, "max connections", dto.getMaxConnections());
        appendIfPresent(sb, "hosts allow", dto.getHostsAllow());
        appendIfPresent(sb, "hosts deny", dto.getHostsDeny());

        return sb.toString();
    }

    /**
     * Удаление указанной секции из исходного содержимого
     */
    public String removeSection(String content, String sectionNameToRemove) {
        String[] lines = content.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        boolean insideTargetSection = false;
        boolean sectionFound = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                if (currentSection.equalsIgnoreCase(sectionNameToRemove)) {
                    insideTargetSection = true;
                    sectionFound = true;
                    continue;
                } else {
                    insideTargetSection = false;
                }
            }

            if (!insideTargetSection) {
                result.append(line).append("\n");
            }
        }

        if (!sectionFound) {
            throw new RuntimeException("Секция '" + sectionNameToRemove + "' не найдена в файле конфигурации");
        }

        return result.toString();
    }

    private void mapProperty(SambaShare share, String key, String value) {
        switch (key) {
            case "path": share.setPath(value); break;
            case "comment": share.setComment(value); break;
            case "read only": share.setReadOnly("yes".equalsIgnoreCase(value)); break;
            case "guest ok": share.setGuestOk("yes".equalsIgnoreCase(value)); break;
            case "browseable": share.setBrowseable("yes".equalsIgnoreCase(value)); break;
            case "valid users": share.setValidUsers(value); break;
            case "write list": share.setWriteList(value); break;
            case "create mask": share.setCreateMask(value); break;
            case "directory mask": share.setDirectoryMask(value); break;
            case "force user": share.setForceUser(value); break;
            case "force group": share.setForceGroup(value); break;
            case "max connections": share.setMaxConnections(value); break;
            case "hosts allow": share.setHostsAllow(value); break;
            case "hosts deny": share.setHostsDeny(value); break;
            default: break;
        }
    }

    private void appendIfPresent(StringBuilder sb, String propertyName, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("   ").append(propertyName).append(" = ").append(value.trim()).append("\n");
        }
    }
}