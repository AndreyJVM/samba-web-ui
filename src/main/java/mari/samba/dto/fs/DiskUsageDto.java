package mari.samba.dto.fs;

public record DiskUsageDto(
        String path,
        String total,
        String used,
        String available,
        int usePercent,
        String mountPoint
) {}
