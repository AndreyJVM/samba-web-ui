package mari.samba.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SambaGroupCreateDto(
        @NotBlank(message = "Validation failed")
                @Pattern(regexp = "^[a-z_][a-z0-9_-]*[$]?$", message = "Validation failed")
                String groupName,
        String description) {}
