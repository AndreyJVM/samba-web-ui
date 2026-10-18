package mari.samba.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import mari.samba.config.ExecutionProperties;
import mari.samba.core.SambaCommandException;
import org.springframework.stereotype.Component;

@Component
public class PrivilegedHelperClient {

    private static final int MAX_FRAME_SIZE = 128 * 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final ExecutionProperties properties;

    public PrivilegedHelperClient(ObjectMapper objectMapper, ExecutionProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public CommandResult execute(PrivilegedCommand command) {
        AtomicReference<SocketChannel> activeChannel = new AtomicReference<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<CommandResult> response = executor.submit(() -> exchange(command, activeChannel));
        Duration timeout = properties.commandTimeout().plusSeconds(2);
        try {
            return response.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            closeQuietly(activeChannel.get());
            response.cancel(true);
            throw new SambaCommandException("Privileged helper timed out after " + timeout.toMillis() + " ms.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeQuietly(activeChannel.get());
            response.cancel(true);
            throw new SambaCommandException("Privileged helper request was interrupted.", e);
        } catch (Exception e) {
            throw new SambaCommandException("Unable to execute privileged local operation.", rootCause(e));
        } finally {
            executor.shutdownNow();
        }
    }

    private CommandResult exchange(PrivilegedCommand command, AtomicReference<SocketChannel> activeChannel)
            throws Exception {
        Path socketPath = Path.of(properties.helperSocket());
        if (!socketPath.isAbsolute()) {
            throw new IllegalStateException("Privileged helper socket path must be absolute.");
        }

        byte[] request = objectMapper.writeValueAsBytes(command);
        requireFrameSize(request.length);

        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            activeChannel.set(channel);
            channel.connect(UnixDomainSocketAddress.of(socketPath));
            try (DataOutputStream output = new DataOutputStream(Channels.newOutputStream(channel));
                    DataInputStream input = new DataInputStream(Channels.newInputStream(channel))) {
                output.writeInt(request.length);
                output.write(request);
                output.flush();

                int responseLength = input.readInt();
                requireFrameSize(responseLength);
                byte[] response = input.readNBytes(responseLength);
                if (response.length != responseLength) {
                    throw new IllegalStateException("Incomplete response from privileged helper.");
                }
                PrivilegedHelperResponse helperResponse =
                        objectMapper.readValue(response, PrivilegedHelperResponse.class);
                return new CommandResult(
                        helperResponse.exitCode(),
                        nullToEmpty(helperResponse.stdout()),
                        nullToEmpty(helperResponse.stderr()));
            }
        } finally {
            activeChannel.set(null);
        }
    }

    private static void requireFrameSize(int size) {
        if (size <= 0 || size > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("Privileged helper frame size is invalid.");
        }
    }

    private static Throwable rootCause(Exception exception) {
        return exception.getCause() != null ? exception.getCause() : exception;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void closeQuietly(SocketChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignored) {
                // The original timeout or interruption is more useful to the caller.
            }
        }
    }
}
