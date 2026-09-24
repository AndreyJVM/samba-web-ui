package mari.samba.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import mari.samba.service.SambaMonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonitoringApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class MonitoringApiControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SambaMonitoringService monitoringService;

  @Test
  void testGetDashboard_WhenRunning_ShouldReturnFullStats() throws Exception {
    when(monitoringService.isServiceRunning(anyString())).thenReturn(true);
    when(monitoringService.getDiskUsage(anyString())).thenReturn(List.of("/ 50% 100G"));
    when(monitoringService.getActiveConnections(anyString()))
        .thenReturn(List.of(Map.of("pid", "1234", "user", "root")));
    when(monitoringService.getOpenFiles(anyString()))
        .thenReturn(List.of(Map.of("file", "/path/to/file")));

    mockMvc
        .perform(get("/api/monitoring/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.isRunning").value(true))
        .andExpect(jsonPath("$.data.diskUsage[0]").value("/ 50% 100G"))
        .andExpect(jsonPath("$.data.connections[0].pid").value("1234"))
        .andExpect(jsonPath("$.data.openFiles[0].file").value("/path/to/file"));
  }

  @Test
  void testGetDashboard_WhenNotRunning_ShouldReturnEmptyData() throws Exception {
    when(monitoringService.isServiceRunning(anyString())).thenReturn(false);
    when(monitoringService.getDiskUsage(anyString())).thenReturn(List.of("/ 50% 100G"));

    mockMvc
        .perform(get("/api/monitoring/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.isRunning").value(false))
        .andExpect(jsonPath("$.data.connections").isEmpty())
        .andExpect(jsonPath("$.data.openFiles").isEmpty());
  }

  @Test
  void testControlService_ShouldReturnSuccess() throws Exception {
    mockMvc
        .perform(post("/api/monitoring/control").param("action", "restart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Команда 'restart' успешно выполнена"));

    verify(monitoringService).controlService(anyString(), eq("restart"));
  }

  @Test
  void testKillSession_ShouldReturnSuccess() throws Exception {
    mockMvc
        .perform(delete("/api/monitoring/sessions/1234"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Сессия (PID: 1234) успешно завершена"));

    verify(monitoringService).killSession(anyString(), eq("1234"));
  }
}
