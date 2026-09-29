package mari.samba.dto.fs;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateDirectoryDto(
    @NotBlank(message = "Parent path must not be blank") @JsonProperty("parentPath")
        String parentPath,
    @NotBlank(message = "Directory name must not be blank") @JsonProperty("dirName")
        String dirName) {}
