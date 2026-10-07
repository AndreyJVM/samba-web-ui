package mari.samba.smbconfig;

/**
 * Data Transfer Object (DTO) representing a backup of a Samba configuration file. This record
 * encapsulates metadata about a specific backup file stored on the server.
 *
 * @param filename the name of the backup file
 * @param createdAt the timestamp or formatted date indicating when the backup was created
 * @param size the size of the backup file, typically formatted as a human-readable string (e.g.,
 *     "1.2 KB")
 */
public record SambaBackupDto(String filename, String createdAt, String size) {}
