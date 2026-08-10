package mari.samba.dto;

public class SambaShareDto {

    private String name;
    private String path;
    private String comment;
    private boolean readOnly;
    private boolean guestOk;
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