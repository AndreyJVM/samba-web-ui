package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SambaGlobalConfigDto {

    @NotBlank(message = "Рабочая группа не может быть пустой")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Недопустимые символы в имени рабочей группы")
    private String workgroup = "WORKGROUP";

    private String serverString = "Samba Server";
    private String netbiosName;

    // Режим аутентификации: user, ads
    private String security = "user";

    // Поведение для неизвестных пользователей: Bad User (для гостевого доступа), Never
    private String mapToGuest = "Bad User";

    // Сетевые привязки
    private String interfaces;
    private boolean bindInterfacesOnly = false;

    // Оптимизация (отключение принтеров для чистого файлового сервера)
    private boolean loadPrinters = false;
    private boolean disableNetbios = false;

    // Версии протокола SMB (по умолчанию min=SMB2, max=SMB3)
    private String serverMinProtocol = "SMB2";
    private String serverMaxProtocol = "SMB3";
}