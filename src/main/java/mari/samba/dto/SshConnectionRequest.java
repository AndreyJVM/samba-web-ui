package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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

    // === Геттеры и сеттеры ===
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}