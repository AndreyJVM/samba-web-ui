package mari.samba.dto.config;

public record SambaBackupDto(
        String filename,
        String createdAt,
        String size
) {}
