package mari.samba.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SambaUser {

    private String username;
    private String fullName;
    private boolean accountEnabled;
    private String lastChange;
    private String passwordHash;

    public SambaUser() {}

    public SambaUser(String username) {
        this.username = username;
        this.accountEnabled = true;
    }

}