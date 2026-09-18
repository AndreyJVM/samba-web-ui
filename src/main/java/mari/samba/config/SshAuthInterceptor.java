package mari.samba.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mari.samba.service.infra.SshSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SshAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SshSessionManager sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session != null && sessionManager.isConnected(session.getId())) {
            // Передаем SessionId дальше в контроллеры как RequestAttribute 
            // Это избавляет нас от необходимости извлекать его руками в каждом контроллере
            request.setAttribute("sessionId", session.getId());
            return true;
        }

        response.sendRedirect("/?disconnected=true");
        return false;
    }
}