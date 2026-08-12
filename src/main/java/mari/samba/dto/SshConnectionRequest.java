package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SshConnectionRequest {

    @NotBlank(message = "IP адрес обязателен")
    @Pattern(
            regexp = "^(([0-9]{1,3}\\.){3}[0-9]{1,3}|[a-zA-Z0-9.-]+)$",
            message = "Введите корректный IP адрес или домен"
    )
    private String host;

    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    private String privateKey; // опционально (для SSH ключей)

}