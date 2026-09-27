package mari.samba.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SambaUserCreateDto(
    @NotBlank(message = "Username is mandatory")
        @Pattern(
            regexp = "^[a-z_][a-z0-9_-]*[$]?$",
            message = "Username contains invalid characters (allowed: a-z, 0-9, _, -)")
        String username,
    @NotBlank(message = "Password is mandatory")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,
    String comment) {}
