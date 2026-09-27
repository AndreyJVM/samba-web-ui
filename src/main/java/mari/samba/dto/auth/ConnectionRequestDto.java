package mari.samba.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectionRequestDto(
    @NotBlank(message = "Host IP or domain is mandatory")
        @Pattern(
            regexp =
                "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^localhost$",
            message = "Invalid IP address or domain format")
        String host,
    @Min(value = 1, message = "Port must be accurately between 1 and 65535")
        @Max(value = 65535, message = "Port must be accurately between 1 and 65535")
        Integer port,
    @NotBlank(message = "Username is mandatory") String username,
    String authType,
    String password,
    String privateKey,
    String passphrase) {

  public ConnectionRequestDto {
    if (port == null || port <= 0) {
      port = 22;
    }
    if (authType == null || authType.isBlank()) {
      authType = "password";
    }
  }

  public int getResolvedPort() {
    return (port != null && port > 0) ? port : 22;
  }

  public boolean isKeyAuth() {
    return "key".equalsIgnoreCase(authType) || (privateKey != null && !privateKey.isBlank());
  }

  // Convenience getters for compatibility
  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getAuthType() {
    return authType;
  }

  public String getPassword() {
    return password;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPassphrase() {
    return passphrase;
  }
}
