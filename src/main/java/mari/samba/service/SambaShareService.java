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

        String shareSection = configService.buildShareSection(dto);
        String currentContent = configService.getSmbConfContent(session);
        String newContent = currentContent + "\n" + shareSection;

        configService.updateSmbConf(session, newContent);
    }

    public void updateShare(Session session, String name, SambaShareCreateDto dto) throws Exception {
        String content = configService.getSmbConfContent(session);

        // Удаляем старую секцию и добавляем обновленную
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
}