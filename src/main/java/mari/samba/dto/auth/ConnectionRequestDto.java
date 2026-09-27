package mari.samba.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectionRequestDto(
    @NotBlank(message = "IP Р°РґСЂРµСЃ РёР»Рё РёРјСЏ С…РѕСЃС‚Р° РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ")
        @Pattern(
            regexp =
                "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^localhost$",
            message = "Р’РІРµРґРёС‚Рµ РєРѕСЂСЂРµРєС‚РЅС‹Р№ IP Р°РґСЂРµСЃ РёР»Рё РґРѕРјРµРЅ")
        String host,
    @Min(value = 1, message = "РџРѕСЂС‚ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ РѕС‚ 1 РґРѕ 65535")
        @Max(value = 65535, message = "РџРѕСЂС‚ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ РѕС‚ 1 РґРѕ 65535")
        Integer port,
    @NotBlank(message = "РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ") String username,
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
