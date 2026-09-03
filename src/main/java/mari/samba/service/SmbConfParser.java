package mari.samba.service;

import mari.samba.dto.SambaGlobalConfigDto;
import mari.samba.dto.SambaShareCreateDto;
import mari.samba.model.SambaShare;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmbConfParser {

    /**
     * Парсинг секции [global] в DTO
     */
    public SambaGlobalConfigDto parseGlobalConfig(String content) {
        SambaGlobalConfigDto dto = new SambaGlobalConfigDto();
        if (content == null || content.isBlank()) {
            return dto;
        }

        String[] lines = content.split("\\r?\\n");
        boolean insideGlobal = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String section = trimmed.substring(1, trimmed.length() - 1).trim();
                insideGlobal = section.equalsIgnoreCase("global");
                continue;
            }

            if (insideGlobal && trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                String key = parts[0].trim().toLowerCase();
                String val = parts[1].trim();

                switch (key) {
                    case "workgroup": dto.setWorkgroup(val); break;
                    case "server string": dto.setServerString(val); break;
                    case "netbios name": dto.setNetbiosName(val); break;
                    case "security": dto.setSecurity(val.toLowerCase()); break;
                    case "map to guest": dto.setMapToGuest(val); break;
                    case "interfaces": dto.setInterfaces(val); break;
                    case "bind interfaces only": dto.setBindInterfacesOnly("yes".equalsIgnoreCase(val)); break;
                    case "load printers": dto.setLoadPrinters("yes".equalsIgnoreCase(val)); break;
                    case "disable netbios": dto.setDisableNetbios("yes".equalsIgnoreCase(val)); break;
                    case "server min protocol": dto.setServerMinProtocol(val.toUpperCase()); break;
                    case "server max protocol": dto.setServerMaxProtocol(val.toUpperCase()); break;
                    default: break;
                }
            }
        }
        return dto;
    }

    /**
     * Генерация новой секции [global]
     */
    public String buildGlobalSection(SambaGlobalConfigDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("[global]\n");
        sb.append("   workgroup = ").append(dto.getWorkgroup().trim()).append("\n");

        if (dto.getServerString() != null && !dto.getServerString().isBlank()) {
            sb.append("   server string = ").append(dto.getServerString().trim()).append("\n");
        }
        if (dto.getNetbiosName() != null && !dto.getNetbiosName().isBlank()) {
            sb.append("   netbios name = ").append(dto.getNetbiosName().trim()).append("\n");
        }

        sb.append("   security = ").append(dto.getSecurity() != null ? dto.getSecurity() : "user").append("\n");
        sb.append("   map to guest = ").append(dto.getMapToGuest() != null ? dto.getMapToGuest() : "Bad User").append("\n");

        if (dto.getInterfaces() != null && !dto.getInterfaces().isBlank()) {
            sb.append("   interfaces = ").append(dto.getInterfaces().trim()).append("\n");
            sb.append("   bind interfaces only = ").append(dto.isBindInterfacesOnly() ? "yes" : "no").append("\n");
        }

        sb.append("   load printers = ").append(dto.isLoadPrinters() ? "yes" : "no").append("\n");
        sb.append("   disable netbios = ").append(dto.isDisableNetbios() ? "yes" : "no").append("\n");

        if (dto.getServerMinProtocol() != null && !dto.getServerMinProtocol().isBlank()) {
            sb.append("   server min protocol = ").append(dto.getServerMinProtocol().trim()).append("\n");
        }
        if (dto.getServerMaxProtocol() != null && !dto.getServerMaxProtocol().isBlank()) {
            sb.append("   server max protocol = ").append(dto.getServerMaxProtocol().trim()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Замена старой секции [global] в конфиге на новую с сохранением остальных секций
     */
    public String updateGlobalSection(String content, SambaGlobalConfigDto dto) {
        String updatedWithoutGlobal;
        try {
            updatedWithoutGlobal = removeSection(content, "global");
        } catch (Exception e) {
            // Если [global] не было вовсе
            updatedWithoutGlobal = content;
        }

        String newGlobalSection = buildGlobalSection(dto);
        return newGlobalSection + "\n" + updatedWithoutGlobal.trim() + "\n";
    }

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