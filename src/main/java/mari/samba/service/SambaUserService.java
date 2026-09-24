package mari.samba.service;

import java.util.ArrayList;
import java.util.List;
import mari.samba.model.SambaUser;
import mari.samba.service.infra.CommandExecutor;
import mari.samba.service.infra.LinuxCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing Samba and underlying OS users via SSH commands. Acts as a bridge
 * between the web UI and the Linux server, translating Java instructions into bash execution
 * sequences. Ensures both system and Samba user accounts are kept in sync.
 */
@Service
public class SambaUserService {

  @Autowired private CommandExecutor commandExecutor;

  /**
   * Retrieves a list of all currently registered Samba users from the remote server. Parses the
   * output of the raw Samba users configuration file or command.
   *
   * @param sessionId the active SSH session identifier of the calling client
   * @return a list of {@link SambaUser} objects representing the remote accounts
   * @throws Exception if the SSH execution fails or the output cannot be parsed
   */
  public List<SambaUser> getAllUsers(String sessionId) throws Exception {
    String output = commandExecutor.execute(sessionId, LinuxCommands.listSambaUsers());

    List<SambaUser> users = new ArrayList<>();
    String[] lines = output.split("\\r?\\n");

    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) continue;

      String[] parts = line.split(":");
      if (parts.length >= 2) {
        String username = parts[0].trim();
        String fullName = (parts.length > 2 && !parts[2].trim().isEmpty()) ? parts[2].trim() : "-";
        users.add(new SambaUser(username, fullName, true, null, null));
      }
    }
    return users;
  }

  /**
   * Creates a new user tightly coupled to both the Linux OS and Samba configurations. Executes a
   * multi-step routine: creates the OS user, updates their OS password, adds them to the Samba
   * database, and forcefully enables the new Samba account.
   *
   * @param sessionId the active SSH session identifier
   * @param username the unix-compliant username to be registered
   * @param password the plaintext password to assign for both OS and Samba login
   * @param fullName an optional full descriptive name (mapped to GECOS field)
   * @throws Exception if any step of the user creation sequence fails on the remote server
   */
  public void createUser(String sessionId, String username, String password, String fullName)
      throws Exception {
    String cleanUsername = username.trim();
    String cleanPassword = password.trim();
    String comment = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanUsername;

    commandExecutor.execute(sessionId, LinuxCommands.addSystemUserWithHome(cleanUsername, comment));
    commandExecutor.execute(
        sessionId, LinuxCommands.chpasswd(), cleanUsername + ":" + cleanPassword + "\n");
    commandExecutor.execute(
        sessionId,
        LinuxCommands.addSambaUser(cleanUsername),
        cleanPassword + "\n" + cleanPassword + "\n");
    commandExecutor.execute(sessionId, LinuxCommands.enableSambaUser(cleanUsername));
  }

  /**
   * Deletes an existing user from both the Samba database and the OS. Safely ignores Samba deletion
   * failures (in case the user was an OS-only user) but guarantees the system-wide deletion
   * attempt.
   *
   * @param sessionId the active SSH session identifier
   * @param username the exact username to completely remove from the remote machine
   * @throws Exception if the OS user deletion command fails
   */
  public void deleteUser(String sessionId, String username) throws Exception {
    try {
      commandExecutor.execute(sessionId, LinuxCommands.deleteSambaUser(username));
    } catch (Exception ignored) {
    }
    commandExecutor.execute(sessionId, LinuxCommands.deleteSystemUser(username));
  }

  /**
   * Synchronizes and updates the password for both the underlying OS and the Samba service to
   * guarantee matching authentication states for the specified user.
   *
   * @param sessionId the active SSH session identifier
   * @param username the target username whose password is to be rotated
   * @param newPassword the new plaintext password to enforce
   * @throws Exception if either the OS or Samba password change command fails
   */
  public void changePassword(String sessionId, String username, String newPassword)
      throws Exception {
    commandExecutor.execute(
        sessionId, LinuxCommands.chpasswd(), username + ":" + newPassword + "\n");
    commandExecutor.execute(
        sessionId,
        LinuxCommands.changeSambaPassword(username),
        newPassword + "\n" + newPassword + "\n");
  }

  /**
   * Evaluates whether a user exists on the remote system based on standard OS directories or
   * databases.
   *
   * @param sessionId the active SSH session identifier
   * @param username the target username to verify
   * @return {@code true} if the user account is found on the server, otherwise {@code false}
   */
  public boolean userExists(String sessionId, String username) {
    try {
      commandExecutor.execute(sessionId, LinuxCommands.checkUserExists(username));
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
