package mari.samba.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SambaShare {

    private String name;
    private String path;
    private String comment;
    private boolean readOnly;
    private boolean guestOk;
    private boolean browseable;
    private String validUsers;
    private String writeList;
    private String createMask;
    private String directoryMask;
    private String forceUser;
    private String forceGroup;
    private String maxConnections;
    private String hostsAllow;
    private String hostsDeny;

    public SambaShare() {}

    public SambaShare(String name, String path) {
        this.name = name;
        this.path = path;
        this.readOnly = true;
        this.guestOk = false;
        this.browseable = true;
    }

}