package mari.samba.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SambaGroupCreateDto(
    @NotBlank(message = "Название группы обязательно")
        @Pattern(
            regexp = "^[a-z_][a-z0-9_-]*[$]?$",
            message = "Недопустимые символы в имени группы (используйте a-z, 0-9, _, -)")
        String groupName,
    String description) {}
