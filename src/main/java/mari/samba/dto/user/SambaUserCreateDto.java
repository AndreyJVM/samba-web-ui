package mari.samba.dto.user;

import jakarta.validation.constraints.NotBlank;

public record SambaUserCreateDto(
    @NotBlank(message = "РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ") String username,
    String fullName,
    @NotBlank(message = "РџР°СЂРѕР»СЊ РѕР±СЏР·Р°С‚РµР»РµРЅ") String password) {}
