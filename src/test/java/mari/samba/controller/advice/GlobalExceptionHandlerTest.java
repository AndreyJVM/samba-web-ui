package mari.samba.controller.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mari.samba.exception.SshSessionExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void testHandleSshSessionExpiredException_ShouldReturn401() throws Exception {
    mockMvc
        .perform(get("/dummy/ssh-expired"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("SSH-сессия истекла. Требуется повторный вход."));
  }

  @Test
  void testHandleIllegalArgumentException_ShouldReturn400() throws Exception {
    mockMvc
        .perform(get("/dummy/invalid-arg"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Неверные параметры"));
  }

  @Test
  void testHandleGeneralException_ShouldReturn500() throws Exception {
    mockMvc
        .perform(get("/dummy/general-exception"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Database is down"));
  }

  /** Dummy controller to trigger exceptions for the GlobalExceptionHandler to intercept. */
  @RestController
  static class DummyController {
    @GetMapping("/dummy/ssh-expired")
    public void throwSshException() {
      throw new SshSessionExpiredException("Connection lost internally");
    }

    @GetMapping("/dummy/invalid-arg")
    public void throwIllegalArgumentException() {
      throw new IllegalArgumentException("Неверные параметры");
    }

    @GetMapping("/dummy/general-exception")
    public void throwGeneralException() {
      throw new RuntimeException("Database is down");
    }
  }

  @Configuration
  static class DummyContextConfig {
    @org.springframework.context.annotation.Bean
    public DummyController dummyController() {
      return new DummyController();
    }

    @org.springframework.context.annotation.Bean
    public GlobalExceptionHandler globalExceptionHandler() {
      return new GlobalExceptionHandler();
    }
  }
}
