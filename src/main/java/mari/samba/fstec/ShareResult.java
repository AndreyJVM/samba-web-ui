package mari.samba.fstec;

/**
 * Compliance check result for a specific individual Samba network share.
 *
 * @param shareName the name of the Samba share
 * @param status the compliance status of this share
 * @param currentValue the actual evaluated value for this share
 */
public record ShareResult(String shareName, FstecStatus status, String currentValue) {}
