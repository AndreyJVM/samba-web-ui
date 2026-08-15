package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SambaShareCreateDto {

    @NotBlank(message = "Имя шары обязательно")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Имя может содержать только буквы, цифры, подчеркивания и дефисы")
    private String name;

    @NotBlank(message = "Путь к папке обязателен")
    private String path;

    private String comment;
    private boolean readOnly = true;
    private boolean guestOk = false;
    private boolean browseable = true;
    private String validUsers;
    private String writeList;
    private String createMask;
    private String directoryMask;
    private String forceUser;
    private String forceGroup;
    private String maxConnections;
    private String hostsAllow;
    private String hostsDeny;

}