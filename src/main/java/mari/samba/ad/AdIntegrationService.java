package mari.samba.ad;

import java.util.Locale;
import mari.samba.infra.CommandExecutor;
import mari.samba.infra.LinuxCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdIntegrationService {

  private static final Logger log = LoggerFactory.getLogger(AdIntegrationService.class);

  private static final String JOIN_OK_MARKER = "Join is OK";
  private static final String MSG_JOINED = "Successfully joined to domain.";
  private static final String MSG_NOT_JOINED = "Not joined to any domain.";
  private static final String DOMAIN_UNKNOWN = "UNKNOWN";

  // Пока не проверяются по-настоящему; заполним реальными данными на одном из следующих шагов
  private static final boolean DNS_REACHABLE_PLACEHOLDER = true;
  private static final boolean KERBEROS_WORKING_PLACEHOLDER = true;

  private final CommandExecutor commandExecutor;

  public AdIntegrationService(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  public AdStatusDto getStatus(String sessionId) {
    String testJoinOutput = executeIgnoringErrors(sessionId, LinuxCommands.netAdsTestJoin());
    boolean joined = testJoinOutput.contains(JOIN_OK_MARKER);
    return new AdStatusDto(
            joined,
            joined ? MSG_JOINED : MSG_NOT_JOINED,
            DOMAIN_UNKNOWN,
            DNS_REACHABLE_PLACEHOLDER,
            KERBEROS_WORKING_PLACEHOLDER,
            joined);
  }

  public AdStatusDto joinDomain(String sessionId, AdJoinRequestDto request) {
    log.info(
            "Joining Active Directory domain '{}' as user '{}'", request.domain(), request.username());

    commandExecutor.execute(sessionId, LinuxCommands.writeKrb5Conf(), buildKrb5Conf(request.domain()));

    String joinOutput =
            commandExecutor.execute(
                    sessionId, LinuxCommands.netAdsJoin(request.username(), request.password()));
    if (looksLikeFailure(joinOutput)) {
      log.error("Domain join failed: {}", joinOutput);
      throw new AdJoinException("Domain join failed: " + joinOutput);
    }

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

  private static boolean looksLikeFailure(String output) {
    String normalized = output.toLowerCase(Locale.ROOT);
    return normalized.contains("failed") || normalized.contains("error");
  }

  private static String buildKrb5Conf(String domain) {
    return """
        [libdefaults]
            default_realm = %s
            dns_lookup_realm = false
            dns_lookup_kdc = true
        """
            .formatted(domain.toUpperCase(Locale.ROOT));
  }

  private String executeIgnoringErrors(String sessionId, String command) {
    try {
      return commandExecutor.execute(sessionId, command);
    } catch (Exception e) {
      log.debug("Command failed, treating output as the error message", e);
      return e.getMessage() != null ? e.getMessage() : "";
    }
  }
}