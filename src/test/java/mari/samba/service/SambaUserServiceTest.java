package mari.samba.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import mari.samba.model.SambaUser;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SambaUserServiceTest {

  @Mock private CommandExecutor commandExecutor;

  @InjectMocks private SambaUserService sambaUserService;

  private static final String SESSION_ID = "test-session";

  @Test
  void getAllUsers_ShouldParsePdbeditOutputCorrectly() throws Exception {
    // Имитируем вывод команды `sudo pdbedit -L`
    String mockOutput =
        "admin:1000:Server Administrator\n" + "bob:1001:\n" + "alice:1002:Alice Wonderland\n";

    when(commandExecutor.execute(SESSION_ID, LinuxCommands.listSambaUsers()))
        .thenReturn(mockOutput);

    List<SambaUser> users = sambaUserService.getAllUsers(SESSION_ID);

    assertNotNull(users);
    assertEquals(3, users.size());

    assertEquals("admin", users.get(0).getUsername());
    assertEquals("Server Administrator", users.get(0).getFullName());

    assertEquals("bob", users.get(1).getUsername());
    assertEquals("-", users.get(1).getFullName()); // Пустое поле заменяется на "-"

    assertEquals("alice", users.get(2).getUsername());
  }

  @Test
  void createUser_ShouldExecuteCorrectSequenceOfCommands() throws Exception {
    String username = "newuser";
    String password = "securepass123";
    String fullName = "New User";

    sambaUserService.createUser(SESSION_ID, username, password, fullName);

    // Проверяем 4 этапа создания
    verify(commandExecutor)
        .execute(SESSION_ID, LinuxCommands.addSystemUserWithHome(username, fullName));
    verify(commandExecutor)
        .execute(
            eq(SESSION_ID), eq(LinuxCommands.chpasswd()), eq(username + ":" + password + "\n"));
    verify(commandExecutor)
        .execute(
            eq(SESSION_ID),
            eq(LinuxCommands.addSambaUser(username)),
            eq(password + "\n" + password + "\n"));
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.enableSambaUser(username));
  }

  @Test
  void deleteUser_ShouldHandleExceptionsGracefullyForSambaAndKillSystemUser() throws Exception {
    String username = "olduser";

    // Если удаление из Samba падает (пользователь есть в Линуксе, но нет в smbpasswd)
    when(commandExecutor.execute(SESSION_ID, LinuxCommands.deleteSambaUser(username)))
        .thenThrow(new RuntimeException("Failed to delete from Samba"));

    // Метод не должен выбросить исключение наружу, он должен проигнорировать первичное и удалить
    // системного юзера
    assertDoesNotThrow(() -> sambaUserService.deleteUser(SESSION_ID, username));

    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.deleteSambaUser(username));
    verify(commandExecutor).execute(SESSION_ID, LinuxCommands.deleteSystemUser(username));
  }

  @Test
  void changePassword_ShouldUpdateBothSystemAndSamba() throws Exception {
    String username = "bob";
    String newPassword = "newpassword!";

    sambaUserService.changePassword(SESSION_ID, username, newPassword);

    verify(commandExecutor)
        .execute(eq(SESSION_ID), eq(LinuxCommands.chpasswd()), eq("bob:newpassword!\n"));
    verify(commandExecutor)
        .execute(
            eq(SESSION_ID),
            eq(LinuxCommands.changeSambaPassword(username)),
            eq("newpassword!\nnewpassword!\n"));
  }
}
