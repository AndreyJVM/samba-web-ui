package mari.samba.dto.user;

import lombok.Data;

@Data
public class SambaUserCreateDto {
    private String username;
    private String fullName;
    private String password;
}
