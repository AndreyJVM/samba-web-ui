package mari.samba.dto.fs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryBrowseResultDto {
    private String currentPath;
    private String parentPath;
    private List<DirectoryItemDto> directories;
}