package mari.samba.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionRequestDto {

    @NotBlank(message = "IP адрес или имя хоста обязательно")
    @Pattern(
            regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^localhost$",
            message = "Введите корректный IP адрес или домен"
    )
    private String host;

    @Min(value = 1, message = "Порт должен быть от 1 до 65535")
    @Max(value = 65535, message = "Порт должен быть от 1 до 65535")
    private Integer port = 22;

    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    // Тип авторизации: "password" или "key"
    private String authType = "password";

    // Для авторизации по паролю
    private String password;

    // Для авторизации по SSH-ключу
    private String privateKey;

    // Кодовая фраза, если закрытый ключ зашифрован (опционально)
    private String passphrase;

    public int getResolvedPort() {
        return (port != null && port > 0) ? port : 22;
    }

    public boolean isKeyAuth() {
        return "key".equalsIgnoreCase(authType) || (privateKey != null && !privateKey.isBlank());
    }
}