package mari.samba.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for changing a Samba user's password. This record encapsulates the
 * data required to update a user's password.
 *
 * @param newPassword the new password for the user, which must not be blank
 */
public record SambaUserChangePasswordDto(
        @NotBlank(message = "New password must not be blank") @JsonProperty("newPassword") String newPassword) {}
