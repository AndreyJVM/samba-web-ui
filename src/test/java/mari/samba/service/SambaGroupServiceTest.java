package mari.samba.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import mari.samba.config.SambaProperties;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaGroupServiceTest {

  @Mock private CommandExecutor commandExecutor;
  @Mock private SambaProperties properties;

  @InjectMocks private SambaGroupService sambaGroupService;

  private static final String SESSION_ID = "test-session";

  @Test
  void getAllGroups_ShouldFilterAndParseCorrectly() throws Exception {
    String mockGetentOutput =
        "root:x:0:\n"
            + "daemon:x:1:\n"
            + "smb_managers:x:1001:alice,bob\n"
            + "smb_emptygroup:x:1002:\n"
            + "docker:x:999:admin\n";

    when(commandExecutor.execute(SESSION_ID, "getent group")).thenReturn(mockGetentOutput);

    List<SambaGroup> groups = sambaGroupService.getAllGroups(SESSION_ID);

    assertNotNull(groups);
    assertEquals(2, groups.size());

    SambaGroup group1 = groups.get(0);
    assertEquals("managers", group1.name());
    assertEquals(List.of("alice", "bob"), group1.members());

    SambaGroup group2 = groups.get(1);
    assertEquals("emptygroup", group2.name());
    assertTrue(group2.members().isEmpty());
  }

  @Test
  void createGroup_ShouldExecuteCorrectCommand() throws Exception {
    SambaGroupCreateDto dto = new SambaGroupCreateDto("developers", "Developers group");

    sambaGroupService.createGroup(SESSION_ID, dto);

    verify(commandExecutor).execute(eq(SESSION_ID), eq("sudo groupadd 'smb_developers'"));
  }

  @Test
  void deleteGroup_ShouldExecuteCorrectCommand() throws Exception {
    sambaGroupService.deleteGroup(SESSION_ID, "developers");

    verify(commandExecutor).execute(eq(SESSION_ID), eq("sudo groupdel 'smb_developers'"));
  }

  @Test
  void addUserToGroup_ShouldExecuteCorrectCommand() throws Exception {
    sambaGroupService.addUserToGroup(SESSION_ID, "john.doe", "developers");

    verify(commandExecutor)
        .execute(eq(SESSION_ID), eq("sudo gpasswd -a 'john.doe' 'smb_developers'"));
  }
}
