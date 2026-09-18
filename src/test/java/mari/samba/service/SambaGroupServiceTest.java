package mari.samba.service;

import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SambaGroupServiceTest {

    @Mock
    private CommandExecutor commandExecutor;

    @InjectMocks
    private SambaGroupService sambaGroupService;

    private static final String SESSION_ID = "test-session";

    @Test
    void getAllGroups_ShouldFilterAndParseCorrectly() throws Exception {
        String mockGetentOutput = 
                "root:x:0:\n" +
                "daemon:x:1:\n" +
                "smb_managers:x:1001:alice,bob\n" +
                "smb_emptygroup:x:1002:\n" + 
                "docker:x:999:admin\n";

        when(commandExecutor.execute(SESSION_ID, "getent group")).thenReturn(mockGetentOutput);

        List<SambaGroup> groups = sambaGroupService.getAllGroups(SESSION_ID);

        assertNotNull(groups);
        assertEquals(2, groups.size(), "Должны вернуться только группы с префиксом smb_");

        SambaGroup managers = groups.get(0);
        assertEquals("managers", managers.getName());
        assertTrue(managers.getMembers().containsAll(List.of("alice", "bob")));

        SambaGroup emptyGroup = groups.get(1);
        assertEquals("emptygroup", emptyGroup.getName());
        assertTrue(emptyGroup.getMembers().isEmpty(), "В группе не должно быть пользователей");
    }

    @Test
    void createGroup_ShouldAddPrefixAndEscape() throws Exception {
        SambaGroupCreateDto dto = new SambaGroupCreateDto("developers", "description");

        sambaGroupService.createGroup(SESSION_ID, dto);

        verify(commandExecutor).execute(eq(SESSION_ID), eq("sudo groupadd 'smb_developers'"));
    }

    @Test
    void deleteGroup_ShouldAddPrefixAndEscape() throws Exception {
        sambaGroupService.deleteGroup(SESSION_ID, "developers");

        verify(commandExecutor).execute(eq(SESSION_ID), eq("sudo groupdel 'smb_developers'"));
    }

    @Test
    void addUserToGroup_ShouldFormatCommandCorrectly() throws Exception {
        sambaGroupService.addUserToGroup(SESSION_ID, "john.doe", "developers");

        verify(commandExecutor).execute(eq(SESSION_ID), eq("sudo gpasswd -a 'john.doe' 'smb_developers'"));
    }
}
