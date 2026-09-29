package mari.samba.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "samba")
public record SambaProperties(
    @DefaultValue Ssh ssh,
    @DefaultValue Paths paths,
    @DefaultValue Security security,
    @DefaultValue Group group) {

  public record Ssh(
      @DefaultValue("7000ms") Duration connectTimeout,
      @DefaultValue("30s") Duration commandTimeout,
      @DefaultValue("30s") Duration serverAliveInterval,
      @DefaultValue("3") int serverAliveCountMax) {}

  public record Paths(
      @DefaultValue("/etc/samba/smb.conf") String config,
      @DefaultValue("/etc/samba/backups") String backups,
      @DefaultValue("/var/log/samba/log.smbd") String log) {}

  public record Security(
      @DefaultValue({"/mnt", "/media", "/srv", "/data", "/home"}) List<String> allowedRoots,
      @DefaultValue("5") int maxLoginAttempts,
      @DefaultValue("3m") Duration loginBlockDuration) {}

  public record Group(@DefaultValue("smb_") String prefix) {}
}
