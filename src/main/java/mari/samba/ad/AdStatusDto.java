package mari.samba.ad;

public record AdStatusDto(
        boolean isJoined,
        String statusMessage,
        String domainName,
        boolean dnsReachable,
        boolean kerberosWorking,
        boolean winbindWorking) {}
