package mari.samba.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHandleSshSessionExpiredException_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/dummy/ssh-expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Connection lost internally"));
    }

    @Test
    void testHandleIllegalArgumentException_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/dummy/invalid-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Operation failed due to an error."));
    }

    @Test
    void testHandleSambaCommandException_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/dummy/samba-cmd"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Samba command failed: smbpasswd failed"));
    }

    @Test
    void testHandleMissingParams_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/dummy/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Required parameter is missing: req"));
    }

    @Test
    void testHandleMessageNotReadable_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/dummy/malformed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"badJson: }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed JSON request."));
    }

    @Test
    void testHandleAccessDenied_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/dummy/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void testHandleConstraintViolation_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/dummy/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation error: "));
    }

    @Test
    void testHandleGeneralException_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/dummy/general-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database is down"));
    }

    @RestController
    static class DummyController {
        @GetMapping("/dummy/ssh-expired")
        public void throwSshException() {
            throw new SshSessionExpiredException("Connection lost internally");
        }

        @GetMapping("/dummy/invalid-arg")
        public void throwIllegalArgumentException() {
            throw new IllegalArgumentException("Operation failed due to an error.");
        }

        @GetMapping("/dummy/samba-cmd")
        public void throwSambaCmdException() {
            throw new SambaCommandException("smbpasswd failed");
        }

        @GetMapping("/dummy/missing-param")
        public void throwMissingParam(@RequestParam("req") String req) {}

        @PostMapping("/dummy/malformed")
        public void readMalformed(@RequestBody DummyRequest req) {}

        @GetMapping("/dummy/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("You do not have permission");
        }

        @GetMapping("/dummy/constraint")
        public void throwConstraintViolation() {
            throw new ConstraintViolationException("Invalid parameter", Set.of());
        }

        @GetMapping("/dummy/general-exception")
        public void throwGeneralException() {
            throw new RuntimeException("Database is down");
        }
    }

    static class DummyRequest {
        public String data;
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
