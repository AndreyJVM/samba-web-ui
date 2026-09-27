package mari.samba.dto.ad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdJoinRequestDto(
    @NotBlank(message = "Domain is mandatory")
        @Pattern(
            regexp = "^([a-zA-Z0-9\\-]+[\\.]?)+[a-zA-Z0-9\\-]+$",
            message = "Domain name contains invalid characters")
        String domain,
    @NotBlank(message = "Admin username is mandatory") String username,
    @NotBlank(message = "Admin password is mandatory") String password,
    String organizationalUnit) {}
