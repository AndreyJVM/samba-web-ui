package mari.samba.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SambaShareDto {

    private String name;
    private String path;
    private String comment;
    private boolean readOnly;
    private boolean guestOk;
    private String validUsers;

}