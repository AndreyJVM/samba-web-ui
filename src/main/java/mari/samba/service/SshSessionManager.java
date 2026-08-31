package mari.samba.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SshSessionManager {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session createSession(String sessionId, String host, String username, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(5000);

        sessions.put(sessionId, session);
        return session;
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
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

    /**
     * Выполнение shell-команды на удаленном сервере с опциональной передачей данных через stdin
     */
    public String executeCommand(Session session, String command, String inputData) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("SSH-сессия не активна");
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
            throw new RuntimeException("Команда завершилась с ошибкой (code " + exitStatus + "): " + (error.isBlank() ? output : error));
        }

        return output;
    }

    public String executeCommand(Session session, String command) throws Exception {
        return executeCommand(session, command, null);
    }
}