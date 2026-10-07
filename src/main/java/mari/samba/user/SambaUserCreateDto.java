package mari.samba.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for creating a new Samba user. This record encapsulates the necessary
 * information to register a user in the Samba system.
 *
 * @param username the unique login name for the new user, must not be blank
 * @param fullName the full name or description of the new user
 * @param password the initial password for the new user, must not be blank
 */
public record SambaUserCreateDto(
        @NotBlank(message = "Validation failed") String username,
        String fullName,
        @NotBlank(message = "Validation failed") String password) {}
