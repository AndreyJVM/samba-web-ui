package mari.samba.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SambaGroupCreateDto(
    @NotBlank(message = "РќР°Р·РІР°РЅРёРµ РіСЂСѓРїРїС‹ РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ")
        @Pattern(
            regexp = "^[a-z_][a-z0-9_-]*[$]?$",
            message =
                "РќРµРґРѕРїСѓСЃС‚РёРјС‹Рµ СЃРёРјРІРѕР»С‹ РІ РёРјРµРЅРё РіСЂСѓРїРїС‹ (РёСЃРїРѕР»СЊР·СѓР№С‚Рµ a-z, 0-9, _, -)")
        String groupName,
    String description) {}
