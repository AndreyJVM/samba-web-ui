package mari.samba.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SambaGroupCreateDto(
    @NotBlank(message = "Group name is mandatory")
        @Pattern(
            regexp = "^[a-z_][a-z0-9_-]*[$]?$",
            message = "Group name contains invalid characters (allowed: a-z, 0-9, _, -)")
        String groupName,
    String description) {}
