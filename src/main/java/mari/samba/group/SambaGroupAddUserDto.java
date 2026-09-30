package mari.samba.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SambaGroupAddUserDto(
    @NotBlank(message = "Username must not be blank") @JsonProperty("username") String username) {}
