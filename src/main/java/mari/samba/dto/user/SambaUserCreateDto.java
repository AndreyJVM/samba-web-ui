package mari.samba.dto.user;

import jakarta.validation.constraints.NotBlank;

public record SambaUserCreateDto(
    @NotBlank(message = "Имя пользователя обязательно") String username,
    String fullName,
    @NotBlank(message = "Пароль обязателен") String password) {}
