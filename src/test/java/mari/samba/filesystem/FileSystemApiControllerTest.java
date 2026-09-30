package mari.samba.filesystem;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileSystemApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileSystemApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private FileSystemService fileSystemService;

  @Test
  void testBrowseDirectories_ShouldReturnResult() throws Exception {
    DirectoryBrowseResultDto result = new DirectoryBrowseResultDto("/srv/samba", "/srv", List.of());
    when(fileSystemService.listDirectories(anyString(), anyString())).thenReturn(result);

    mockMvc
        .perform(get("/api/fs/browse").param("path", "/srv/samba"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currentPath").value("/srv/samba"));
  }

  @Test
  void testMakeDirectoryParams_ShouldReturnSuccess() throws Exception {
    mockMvc
        .perform(post("/api/fs/mkdir").param("parentPath", "/srv/samba").param("name", "newfolder"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(fileSystemService).createDirectory(anyString(), eq("/srv/samba"), eq("newfolder"));
  }

  @Test
  void testMakeDirectoryBody_ShouldReturnSuccess() throws Exception {
    CreateDirectoryDto dto = new CreateDirectoryDto("/srv/samba", "newfolder");

    mockMvc
        .perform(
            post("/api/fs/mkdir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(fileSystemService).createDirectory(anyString(), eq("/srv/samba"), eq("newfolder"));
  }

  @Test
  void testGetDiskUsage_ShouldReturnUsage() throws Exception {
    DiskUsageDto dto = new DiskUsageDto("/", "100G", "50G", "50G", 50, "/");
    when(fileSystemService.getDiskUsage(anyString(), anyString())).thenReturn(dto);

    mockMvc
        .perform(get("/api/fs/disk-usage").param("path", "/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value("100G"))
        .andExpect(jsonPath("$.data.usePercent").value(50));
  }
}
