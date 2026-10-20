package mari.samba.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "shareguard.execution")
public record ExecutionProperties(
        @DefaultValue("AUTO") @NotNull Mode mode,
        @DefaultValue("admin") @NotBlank String localUsername,
        @DefaultValue("") String localPassword,
        @DefaultValue("30s") @NotNull Duration commandTimeout,
        @DefaultValue("/run/shareguard/helper.sock") @NotBlank String helperSocket) {

    public enum Mode {
        AUTO,
        LOCAL,
        REMOTE
    }

    public Mode effectiveMode() {
        if (mode != Mode.AUTO) {
            return mode;
        }
        return localPassword != null && !localPassword.isBlank() ? Mode.LOCAL : Mode.REMOTE;
    }
}
