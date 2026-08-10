package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SambaShareCreateDto {

    // === Геттеры и сеттеры ===
    @NotBlank(message = "Имя шары обязательно")
    private String name;

    @NotBlank(message = "Путь к папке обязателен")
    private String path;

    private String comment;
    private boolean readOnly = true;
    private boolean guestOk = false;
    private String validUsers;

}