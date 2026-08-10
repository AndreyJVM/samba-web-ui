package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SambaUserCreateDto {

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 20, message = "Имя должно быть от 3 до 20 символов")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    private String password;

    private String fullName;

}