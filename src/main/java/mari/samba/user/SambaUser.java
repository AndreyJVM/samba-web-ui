package mari.samba.user;

/**
 * Represents a user in the Samba system. This record encapsulates the details of a Samba user,
 * including their account status and credentials.
 *
 * @param username the unique login name of the user
 * @param fullName the full name or description of the user
 * @param accountEnabled {@code true} if the user account is active and enabled, {@code false}
 *     otherwise
 * @param lastChange the timestamp or date of the last password or account change
 * @param passwordHash the hash of the user's password
 */
public record SambaUser(
        String username, String fullName, boolean accountEnabled, String lastChange, String passwordHash) {

    /**
     * Constructs a new {@code SambaUser} with only a username. By default, the full name is set to
     * "-", the account is enabled, and both the last change timestamp and password hash are {@code
     * null}.
     *
     * @param username the unique login name of the user
     */
    public SambaUser(String username) {
        this(username, "-", true, null, null);
    }

    /**
     * Returns the username of this Samba user.
     *
     * @return the user's login name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the full name of this Samba user.
     *
     * @return the user's full name or description
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Checks whether the user's account is currently enabled.
     *
     * @return {@code true} if the account is enabled, {@code false} otherwise
     */
    public boolean isAccountEnabled() {
        return accountEnabled;
    }

    /**
     * Returns the timestamp or date of the last change made to the user's account or password.
     *
     * @return the last change timestamp, or {@code null} if not available
     */
    public String getLastChange() {
        return lastChange;
    }

    /**
     * Returns the hash of the user's password.
     *
     * @return the user's password hash, or {@code null} if not available
     */
    public String getPasswordHash() {
        return passwordHash;
    }
}
