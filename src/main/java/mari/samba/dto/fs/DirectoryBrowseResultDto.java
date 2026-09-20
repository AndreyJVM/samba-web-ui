package mari.samba.dto.fs;

import java.util.List;

public record DirectoryBrowseResultDto(
        String currentPath,
        String parentPath,
        List<DirectoryItemDto> directories
) {}
