package mari.samba.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import mari.samba.model.SambaShare;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class SambaConfigService {

    private static final String SMB_CONF_PATH = "/etc/samba/smb.conf";

    /**
     * Получить содержимое smb.conf через SSH
     */
    public String getSmbConfContent(Session session) throws Exception {
        String command = "cat " + SMB_CONF_PATH;
        return executeCommand(session, command);
    }

    /**
     * Обновить smb.conf через SSH
     */
    public void updateSmbConf(Session session, String content) throws Exception {
        // Создаем временный файл, затем заменяем
        String tempFile = "/tmp/smb.conf.tmp";
        String writeCommand = "cat > " + tempFile + " << 'EOF'\n" + content + "\nEOF\n";
        executeCommand(session, writeCommand);
        executeCommand(session, "sudo mv " + tempFile + " " + SMB_CONF_PATH);
        executeCommand(session, "sudo systemctl restart smbd");
    }

    /**
     * Парсинг smb.conf и извлечение списка шар
     */
    public List<SambaShare> parseShares(String content) {
        List<SambaShare> shares = new ArrayList<>();
        String[] lines = content.split("\n");
        SambaShare currentShare = null;
        boolean insideShare = false;

        for (String line : lines) {
            line = line.trim();

            // Пропускаем комментарии и глобальную секцию [global]
            if (line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            // Начало новой секции (шары)
            if (line.startsWith("[") && line.endsWith("]")) {
                String sectionName = line.substring(1, line.length() - 1);
                if (!sectionName.equals("global") && !sectionName.equals("homes") && !sectionName.equals("printers")) {
                    // Сохраняем предыдущую шару, если была
                    if (currentShare != null && currentShare.getName() != null) {
                        shares.add(currentShare);
                    }
                    currentShare = new SambaShare();
                    currentShare.setName(sectionName);
                    insideShare = true;
                } else {
                    insideShare = false;
                    currentShare = null;
                }
                continue;
            }

            // Парсим параметры внутри шары
            if (insideShare && currentShare != null && line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "path":
                        currentShare.setPath(value);
                        break;
                    case "comment":
                        currentShare.setComment(value);
                        break;
                    case "read only":
                        currentShare.setReadOnly("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value));
                        break;
                    case "guest ok":
                        currentShare.setGuestOk("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value));
                        break;
                    case "valid users":
                        currentShare.setValidUsers(value);
                        break;
                    case "write list":
                        currentShare.setWriteList(value);
                        break;
                    case "create mask":
                        currentShare.setCreateMask(value);
                        break;
                    case "directory mask":
                        currentShare.setDirectoryMask(value);
                        break;
                }
            }
        }

        // Добавляем последнюю шару
        if (currentShare != null && currentShare.getName() != null) {
            shares.add(currentShare);
        }

        return shares;
    }

    /**
     * Выполнить SSH-команду и вернуть результат
     */
    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        channel.setOutputStream(outStream);
        channel.setErrStream(errStream);

        channel.connect();
        while (channel.isConnected()) {
            Thread.sleep(100);
        }

        String output = outStream.toString("UTF-8");
        String error = errStream.toString("UTF-8");

        if (!error.isEmpty()) {
            throw new RuntimeException("SSH Error: " + error);
        }

        return output;
    }
}