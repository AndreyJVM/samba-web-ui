package mari.samba.controller;

import com.jcraft.jsch.Session;
import mari.samba.service.SshSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты AuthController")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SshSessionManager sessionManager;

    @Mock
    private Session mockSession; // Мок для Session

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    // =============================================
    // ТЕСТЫ GET-ЗАПРОСОВ
    // =============================================

    @Test
    @DisplayName("GET / - должен возвращать страницу index")
    void testHomePage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("connectionRequest"));
    }

    @Test
    @DisplayName("GET /disconnect - должен разорвать соединение и перенаправить на главную")
    void testDisconnect() throws Exception {
        // Arrange
        String sessionId = "test-session-id";
        doNothing().when(sessionManager).disconnect(anyString());

        // Act & Assert
        mockMvc.perform(get("/disconnect").sessionAttr("id", sessionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Verify
        verify(sessionManager, times(1)).disconnect(anyString());
    }

    @Test
    @DisplayName("GET /disconnect - должен обрабатывать даже если сессия уже неактивна")
    void testDisconnectWithInvalidSession() throws Exception {
        // Arrange
        doNothing().when(sessionManager).disconnect(anyString());

        // Act & Assert
        mockMvc.perform(get("/disconnect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(sessionManager, times(1)).disconnect(anyString());
    }

    // =============================================
    // ТЕСТЫ POST-ЗАПРОСОВ: УСПЕШНЫЕ
    // =============================================

    @Test
    @DisplayName("POST /connect с валидными данными - должен подключиться и перенаправить на /shares")
    void testConnectSuccess() throws Exception {
        // Arrange
        String host = "192.168.1.100";
        String username = "admin";
        String password = "secret123";

        when(sessionManager.createSession(anyString(), eq(host), eq(username), eq(password)))
                .thenReturn(mockSession);

        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", host)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shares"));

        // Verify
        verify(sessionManager, times(1)).createSession(
                anyString(), eq(host), eq(username), eq(password)
        );
    }

    @Test
    @DisplayName("POST /connect - должен сохранять host и username в сессии")
    void testConnectStoresAttributesInSession() throws Exception {
        // Arrange
        String host = "192.168.1.100";
        String username = "admin";
        String password = "secret123";

        when(sessionManager.createSession(anyString(), eq(host), eq(username), eq(password)))
                .thenReturn(mockSession);

        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", host)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shares"));

        // Verify
        verify(sessionManager, times(1)).createSession(
                anyString(), eq(host), eq(username), eq(password)
        );
    }

    // =============================================
    // ТЕСТЫ POST-ЗАПРОСОВ: ОШИБКИ
    // =============================================

    @Test
    @DisplayName("POST /connect с пустым host - должен вернуть ошибку валидации")
    void testConnectWithEmptyHost() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", "")
                        .param("username", "admin")
                        .param("password", "secret123"))
                .andExpect(status().isBadRequest());

        // Verify
        verify(sessionManager, never()).createSession(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /connect с пустым username - должен вернуть ошибку валидации")
    void testConnectWithEmptyUsername() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", "192.168.1.100")
                        .param("username", "")
                        .param("password", "secret123"))
                .andExpect(status().isBadRequest());

        // Verify
        verify(sessionManager, never()).createSession(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /connect с пустым паролем - должен вернуть ошибку валидации")
    void testConnectWithEmptyPassword() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", "192.168.1.100")
                        .param("username", "admin")
                        .param("password", ""))
                .andExpect(status().isBadRequest());

        // Verify
        verify(sessionManager, never()).createSession(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /connect с некорректным host - должен вернуть ошибку валидации")
    void testConnectWithInvalidHost() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", "invalid host with spaces")
                        .param("username", "admin")
                        .param("password", "secret123"))
                .andExpect(status().isBadRequest());

        // Verify
        verify(sessionManager, never()).createSession(anyString(), anyString(), anyString(), anyString());
    }

    // =============================================
    // ТЕСТЫ ОБРАБОТКИ ИСКЛЮЧЕНИЙ
    // =============================================

    @Test
    @DisplayName("POST /connect - должен обрабатывать исключение при подключении")
    void testConnectWithException() throws Exception {
        // Arrange
        String host = "192.168.1.100";
        String username = "admin";
        String password = "wrong_password";
        String errorMessage = "Connection refused: Connection timed out";

        when(sessionManager.createSession(anyString(), eq(host), eq(username), eq(password)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", host)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("connectionRequest"));

        // Verify
        verify(sessionManager, times(1)).createSession(
                anyString(), eq(host), eq(username), eq(password)
        );
    }

    @Test
    @DisplayName("POST /connect - должен обрабатывать исключение с null сообщением")
    void testConnectWithExceptionNullMessage() throws Exception {
        // Arrange
        String host = "192.168.1.100";
        String username = "admin";
        String password = "wrong_password";

        when(sessionManager.createSession(anyString(), eq(host), eq(username), eq(password)))
                .thenThrow(new RuntimeException());

        // Act & Assert
        mockMvc.perform(post("/connect")
                        .param("host", host)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("connectionRequest"));

        // Verify
        verify(sessionManager, times(1)).createSession(
                anyString(), eq(host), eq(username), eq(password)
        );
    }

    // =============================================
    // ТЕСТЫ КОМБИНИРОВАННЫХ СЦЕНАРИЕВ
    // =============================================

    @Test
    @DisplayName("Полный поток: вход -> отключение")
    void testFullFlowConnectAndDisconnect() throws Exception {
        // Arrange
        String host = "192.168.1.100";
        String username = "admin";
        String password = "secret123";

        when(sessionManager.createSession(anyString(), eq(host), eq(username), eq(password)))
                .thenReturn(mockSession);
        doNothing().when(sessionManager).disconnect(anyString());

        // Act - подключаемся
        mockMvc.perform(post("/connect")
                        .param("host", host)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shares"));

        // Act - отключаемся
        mockMvc.perform(get("/disconnect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Verify
        verify(sessionManager, times(1)).createSession(
                anyString(), eq(host), eq(username), eq(password)
        );
        verify(sessionManager, times(1)).disconnect(anyString());
    }
}