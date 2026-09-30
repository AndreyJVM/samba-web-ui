package mari.samba.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebMvcConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void testRootRoutesForwardToIndex() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/ui/index.html"));
    mockMvc
        .perform(get("/ui"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/ui/index.html"));
    mockMvc
        .perform(get("/ui/"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/ui/index.html"));
  }
}
