package mari.samba.dto.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SambaShareCreateDto(
    @NotBlank(message = "Share name is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Share name contains invalid characters")
        String shareName,
    @NotBlank(message = "Share path is mandatory") String path,
    String comment,
    boolean readOnly,
    boolean guestOk,
    boolean browseable) {}
