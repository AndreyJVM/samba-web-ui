package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import mari.samba.dto.user.SambaUserCreateDto;
import mari.samba.model.SambaUser;
import mari.samba.service.SambaUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private SambaUserService sambaUserService;

  @Test
  void testGetAllUsers_ShouldReturnUsersList() throws Exception {
    SambaUser testUser = new SambaUser("johndoe", "John Doe", true, "/home/johndoe", "/bin/bash");
    when(sambaUserService.getAllUsers(anyString())).thenReturn(List.of(testUser));

    mockMvc
        .perform(get("/api/users").sessionAttr("dummy", "dummy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].username").value("johndoe"))
        .andExpect(jsonPath("$.data[0].fullName").value("John Doe"));
  }

  @Test
  void testCreateUser_ShouldReturnSuccessMsg() throws Exception {
    SambaUserCreateDto dto = new SambaUserCreateDto("johndoe", "John Doe", "Secret@123");

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Пользователь 'johndoe' успешно создан"));

    verify(sambaUserService)
        .createUser(anyString(), eq("johndoe"), eq("Secret@123"), eq("John Doe"));
  }

  @Test
  void testDeleteUser_ShouldReturnSuccessMsg() throws Exception {
    mockMvc
        .perform(delete("/api/users/johndoe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Пользователь 'johndoe' успешно удален"));

    verify(sambaUserService).deleteUser(anyString(), eq("johndoe"));
  }
}
