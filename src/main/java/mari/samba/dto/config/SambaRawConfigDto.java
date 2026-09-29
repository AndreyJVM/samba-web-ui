package mari.samba.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SambaRawConfigDto(
    @NotBlank(message = "Configuration content must not be blank") @JsonProperty("content")
        String content) {}
