package mari.samba.filesystem;

import java.util.List;

public record DirectoryBrowseResultDto(String currentPath, String parentPath, List<DirectoryItemDto> directories) {}
