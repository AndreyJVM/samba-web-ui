package mari.samba.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mari.samba.config.ExecutionProperties;
import mari.samba.config.ExecutionProperties.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivilegedHelperClientIntegrationTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void exchangesTypedRequestWithUnixSocketHelper() throws Exception {
        Path socket = temporaryDirectory.resolve("helper.sock");
        Process helper = new ProcessBuilder(
                        "python3",
                        "packaging/helper/shareguard_helper.py",
                        "--socket",
                        socket.toString(),
                        "--user",
                        System.getProperty("user.name"),
                        "--workers",
                        "1")
                .redirectErrorStream(true)
                .start();
        try {
            waitForSocket(socket, helper);
            ExecutionProperties properties =
                    new ExecutionProperties(Mode.LOCAL, "admin", "secret", Duration.ofSeconds(5), socket.toString());
            PrivilegedHelperClient client = new PrivilegedHelperClient(new ObjectMapper(), properties);

            CommandResult result =
                    client.execute(new PrivilegedCommand(PrivilegedOperation.FILESYSTEM_OVERVIEW, List.of(), null));

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout()).contains("Filesystem");
        } finally {
            helper.destroy();
            if (!helper.waitFor(2, TimeUnit.SECONDS)) {
                helper.destroyForcibly();
            }
        }
    }

    private static void waitForSocket(Path socket, Process helper) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Files.exists(socket)) {
                return;
            }
            if (!helper.isAlive()) {
                throw new IllegalStateException("Helper exited before creating its Unix socket.");
            }
            Thread.sleep(20);
        }
        throw new IllegalStateException("Helper did not create its Unix socket.");
    }
}
