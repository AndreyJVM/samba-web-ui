package mari.samba.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SshSessionManager {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Создать новую SSH-сессию
     */
    public Session createSession(String sessionId, String host, String username, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword(password);
        // Для разработки отключаем проверку ключа хоста (в проде использовать known_hosts)
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(5000); // таймаут 5 секунд

        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Получить сессию по ID
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Проверить, активна ли сессия
     */
    public boolean isConnected(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null && session.isConnected();
    }

    /**
     * Разорвать соединение и удалить сессию
     */
    public void disconnect(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * Очистить все неактивные сессии (можно вызывать по расписанию)
     */
    public void cleanupInactiveSessions() {
        sessions.entrySet().removeIf(entry -> !entry.getValue().isConnected());
    }
}