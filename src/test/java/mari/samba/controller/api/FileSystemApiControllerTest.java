package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import mari.samba.dto.fs.DirectoryBrowseResultDto;
import mari.samba.dto.fs.DiskUsageDto;
import mari.samba.service.FileSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileSystemApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileSystemApiControllerTest {

  @Autowired private MockMvc mockMvc;

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
  void testMakeDirectory_ShouldReturnSuccess() throws Exception {
    mockMvc
        .perform(post("/api/fs/mkdir").param("parentPath", "/srv/samba").param("name", "newfolder"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Каталог успешно создан"));

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
