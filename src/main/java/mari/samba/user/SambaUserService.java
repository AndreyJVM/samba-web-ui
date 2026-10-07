package mari.samba.user;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing Samba and underlying OS users via SSH commands. Acts as a bridge
 * between the web UI and the Linux server, translating Java instructions into bash execution
 * sequences. Ensures both system and Samba user accounts are kept in sync.
 */
@Service
public class SambaUserService {

    /** Logger for this service. */
    private static final Logger log = LoggerFactory.getLogger(SambaUserService.class);

    /**
     * Blacklist of critical system users to prevent accidental or malicious destruction via API.
     * Operations on these users will be blocked to ensure system stability and security.
     */
    private static final Set<String> SYSTEM_USERS_BLACKLIST = Set.of(
            "root",
            "daemon",
            "bin",
            "sys",
            "sync",
            "games",
            "man",
            "lp",
            "mail",
            "news",
            "uucp",
            "proxy",
            "www-data",
            "backup",
            "list",
            "irc",
            "gnats",
            "nobody",
            "systemd-network",
            "systemd-resolve",
            "syslog",
            "messagebus",
            "_apt",
            "lxd",
            "uuidd",
            "dnsmasq",
            "landscape",
            "pollinate",
            "sshd",
            "postgres",
            "mysql");

    /** Executor used for dispatching bash commands to the remote server via SSH. */
    private final CommandExecutor commandExecutor;

    /**
     * Constructs a new {@code SambaUserService} with the required command executor.
     *
     * @param commandExecutor the command executor for handling remote Linux command execution
     */
    public SambaUserService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * Checks whether the provided username belongs to the restricted system users blacklist.
     *
     * @param username the username to validate against the blacklist
     * @throws SecurityException if the requested username is found in the blacklist
     */
    private void requireNonSystemUser(String username) {
        if (SYSTEM_USERS_BLACKLIST.contains(username.trim().toLowerCase())) {
            throw new SecurityException("Security Policy: Cannot modify core system users via Samba API.");
        }
    }

    /**
     * Retrieves a list of all currently registered Samba users from the remote server. Parses the
     * output of the raw Samba users configuration file or command.
     *
     * @param sessionId the active SSH session identifier of the calling client
     * @return a list of {@link SambaUser} objects representing the remote accounts
     */
    public List<SambaUser> getAllUsers(String sessionId) {
        String output = commandExecutor.execute(sessionId, LinuxCommands.listSambaUsers());

        if (output == null || output.isBlank()) {
            return List.of();
        }

        return Arrays.stream(output.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String username = parts[0].trim();
                        String fullName = (parts.length > 2 && !parts[2].trim().isEmpty()) ? parts[2].trim() : "-";
                        return new SambaUser(username, fullName, true, null, null);
                    }
                    return null;
                })
                .filter(u -> u != null)
                .toList();
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
     */
    public void createUser(String sessionId, String username, String password, String fullName) {
        requireNonSystemUser(username);
        String cleanUsername = username.trim();
        String cleanPassword = password.trim();
        String comment = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanUsername;

        log.info("Creating system and Samba user '{}'", cleanUsername);
        commandExecutor.execute(sessionId, LinuxCommands.addSystemUserWithHome(cleanUsername, comment));
        commandExecutor.execute(sessionId, LinuxCommands.chpasswd(), cleanUsername + ":" + cleanPassword + "\n");
        commandExecutor.execute(
                sessionId, LinuxCommands.addSambaUser(cleanUsername), cleanPassword + "\n" + cleanPassword + "\n");
        commandExecutor.execute(sessionId, LinuxCommands.enableSambaUser(cleanUsername));
        log.info("Successfully created and enabled Samba user '{}'", cleanUsername);
    }

    /**
     * Deletes an existing user from both the Samba database and the OS. Safely ignores Samba deletion
     * failures (in case the user was an OS-only user) but guarantees the system-wide deletion
     * attempt.
     *
     * @param sessionId the active SSH session identifier
     * @param username the exact username to completely remove from the remote machine
     */
    public void deleteUser(String sessionId, String username) {
        requireNonSystemUser(username);
        log.info("Deleting Samba and system user '{}'", username);
        try {
            commandExecutor.execute(sessionId, LinuxCommands.deleteSambaUser(username));
        } catch (Exception e) {
            log.debug("User '{}' was not present in Samba passdb or deletion failed: {}", username, e.getMessage());
        }
        commandExecutor.execute(sessionId, LinuxCommands.deleteSystemUser(username));
        log.info("Successfully deleted user '{}'", username);
    }

    /**
     * Synchronizes and updates the password for both the underlying OS and the Samba service to
     * guarantee matching authentication states for the specified user.
     *
     * @param sessionId the active SSH session identifier
     * @param username the target username whose password is to be rotated
     * @param newPassword the new plaintext password to enforce
     */
    public void changePassword(String sessionId, String username, String newPassword) {
        requireNonSystemUser(username);
        log.info("Rotating password for user '{}'", username);
        commandExecutor.execute(sessionId, LinuxCommands.chpasswd(), username + ":" + newPassword + "\n");
        commandExecutor.execute(
                sessionId, LinuxCommands.changeSambaPassword(username), newPassword + "\n" + newPassword + "\n");
        log.info("Successfully rotated password for user '{}'", username);
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
