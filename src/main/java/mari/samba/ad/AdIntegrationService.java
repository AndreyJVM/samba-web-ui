package mari.samba.ad;

import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdIntegrationService {

  private static final Logger log = LoggerFactory.getLogger(AdIntegrationService.class);

  private final CommandExecutor commandExecutor;

  public AdIntegrationService(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public AdStatusDto getStatus(String sessionId) {
    String testJoinOut = executeSafe(sessionId, LinuxCommands.netAdsTestJoin());
    boolean isJoined = testJoinOut.contains("Join is OK");
    return new AdStatusDto(
        isJoined,
        isJoined ? "Successfully joined to domain." : "Not joined to any domain.",
        "UNKNOWN",
        true,
        true,
        isJoined);
  }

  public AdStatusDto joinDomain(String sessionId, AdJoinRequestDto request) {
    log.info(
        "Joining Active Directory domain '{}' as user '{}'", request.domain(), request.username());
    // Basic kerberos conf generation
    String krbConf = generateKrb5Conf(request.domain());
    commandExecutor.execute(sessionId, LinuxCommands.writeKrb5Conf(), krbConf);

    // net ads join
    String out =
        commandExecutor.execute(
            sessionId, LinuxCommands.netAdsJoin(request.username(), request.password()));
    if (out.toLowerCase().contains("failed") || out.contains("error")) {
      log.error("Domain join failed: {}", out);
      throw new RuntimeException("Domain join failed: " + out);
    }

    // Restart winbind
    commandExecutor.execute(sessionId, LinuxCommands.systemctl("restart", "winbind"));
    log.info("Successfully joined Active Directory domain '{}'", request.domain());

    return getStatus(sessionId);
  }

  public AdStatusDto leaveDomain(String sessionId, AdJoinRequestDto request) {
    log.info("Leaving Active Directory domain as user '{}'", request.username());
    commandExecutor.execute(
        sessionId, LinuxCommands.netAdsLeave(request.username(), request.password()));
    return getStatus(sessionId);
  }

  private String generateKrb5Conf(String domain) {
    String upperDomain = domain.toUpperCase();
    return "[libdefaults]\n"
        + "    default_realm = "
        + upperDomain
        + "\n"
        + "    dns_lookup_realm = false\n"
        + "    dns_lookup_kdc = true\n";
  }

  private String executeSafe(String sessionId, String command) {
    try {
      return commandExecutor.execute(sessionId, command);
    } catch (Exception e) {
      return e.getMessage() != null ? e.getMessage() : "";
    }
  }
}
