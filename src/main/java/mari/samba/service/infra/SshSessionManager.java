package mari.samba.service.infra;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import mari.samba.dto.auth.ConnectionRequestDto;
import mari.samba.exception.SshSessionExpiredException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SshSessionManager implements CommandExecutor {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void createSession(String sessionId, ConnectionRequestDto request) throws Exception {
        JSch jsch = new JSch();

        // 1. Настройка аутентификации по ключу, если передан ключ
        if (request.isKeyAuth()) {
            if (request.getPrivateKey() == null || request.getPrivateKey().isBlank()) {
                throw new IllegalArgumentException("Приватный SSH-ключ не может быть пустым");
            }

            byte[] prvkey = request.getPrivateKey().trim().getBytes(StandardCharsets.UTF_8);
            byte[] passphraseBytes = (request.getPassphrase() != null && !request.getPassphrase().isBlank())
                    ? request.getPassphrase().getBytes(StandardCharsets.UTF_8)
                    : null;

            // Регистрируем ключ в JSch в памяти (без временных файлов на диске)
            jsch.addIdentity("custom-key-" + sessionId, prvkey, null, passphraseBytes);
        }

        int port = request.getResolvedPort();
        Session session = jsch.getSession(request.getUsername(), request.getHost(), port);

        // 2. Если используется пароль
        if (!request.isKeyAuth() && request.getPassword() != null && !request.getPassword().isBlank()) {
            session.setPassword(request.getPassword());
        }

        session.setConfig("StrictHostKeyChecking", "no");

        // Keep-Alive пинг каждые 30 секунд
        session.setServerAliveInterval(30_000);
        session.setServerAliveCountMax(3);

        // Таймаут подключения 7 секунд
        session.connect(7000);

        sessions.put(sessionId, session);
    }

    public boolean isConnected(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null && session.isConnected();
    }

    public void disconnect(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void cleanupInactiveSessions() {
        sessions.entrySet().removeIf(entry -> !entry.getValue().isConnected());
    }

    @Override
    public String execute(String sessionId, String command) throws Exception {
        return execute(sessionId, command, null);
    }

    @Override
    public String execute(String sessionId, String command, String inputData) throws Exception {
        Session session = sessions.get(sessionId);
        if (session == null || !session.isConnected()) {
            // Удаляем зависший ключ из мапы, если он там остался
            if (sessionId != null) {
                sessions.remove(sessionId);
            }
            throw new SshSessionExpiredException("SSH-сессия истекла или была разорвана сервером");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        channel.setOutputStream(outStream);
        channel.setErrStream(errStream);

        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();

        channel.connect();

        if (inputData != null) {
            out.write(inputData.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                outStream.write(tmp, 0, i);
            }
            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                break;
            }
            Thread.sleep(50);
        }

        int exitStatus = channel.getExitStatus();
        channel.disconnect();

        String output = outStream.toString(StandardCharsets.UTF_8);
        String error = errStream.toString(StandardCharsets.UTF_8);

        if (exitStatus != 0) {
            throw new RuntimeException("Команда завершилась с кодом " + exitStatus + ": " + (error.isBlank() ? output : error));
        }

        return output;
    }
}