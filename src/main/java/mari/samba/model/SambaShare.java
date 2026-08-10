package mari.samba.model;

public class SambaShare {

    private String name;
    private String path;
    private String comment;
    private boolean readOnly;
    private boolean guestOk;
    private String validUsers;
    private String writeList;
    private String createMask;
    private String directoryMask;

    // === Конструктор ===
    public SambaShare() {}

    public SambaShare(String name, String path) {
        this.name = name;
        this.path = path;
        this.readOnly = true;
        this.guestOk = false;
    }

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

    public String getWriteList() {
        return writeList;
    }

    public void setWriteList(String writeList) {
        this.writeList = writeList;
    }

    public String getCreateMask() {
        return createMask;
    }

    public void setCreateMask(String createMask) {
        this.createMask = createMask;
    }

    public String getDirectoryMask() {
        return directoryMask;
    }

    public void setDirectoryMask(String directoryMask) {
        this.directoryMask = directoryMask;
    }
}