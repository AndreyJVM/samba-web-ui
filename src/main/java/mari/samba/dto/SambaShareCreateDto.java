package mari.samba.dto;

import jakarta.validation.constraints.NotBlank;

public class SambaShareCreateDto {

    @NotBlank(message = "Имя шары обязательно")
    private String name;

    @NotBlank(message = "Путь к папке обязателен")
    private String path;

    private String comment;
    private boolean readOnly = true;
    private boolean guestOk = false;
    private String validUsers;

    // === Геттеры и сеттеры ===
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isGuestOk() {
        return guestOk;
    }

    public void setGuestOk(boolean guestOk) {
        this.guestOk = guestOk;
    }

    public String getValidUsers() {
        return validUsers;
    }

    public void setValidUsers(String validUsers) {
        this.validUsers = validUsers;
    }
}