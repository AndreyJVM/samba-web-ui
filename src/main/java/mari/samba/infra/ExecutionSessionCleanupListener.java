package mari.samba.infra;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

/** Closes execution sessions on servlet session invalidation or expiration. */
@Component
public class ExecutionSessionCleanupListener implements HttpSessionListener {

    private final ExecutionSessionManager sessions;

    public ExecutionSessionCleanupListener(ExecutionSessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessions.disconnect(event.getSession().getId());
    }
}
