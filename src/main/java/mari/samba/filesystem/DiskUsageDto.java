package mari.samba.filesystem;

public record DiskUsageDto(
        String path, String total, String used, String available, int usePercent, String mountPoint) {}
