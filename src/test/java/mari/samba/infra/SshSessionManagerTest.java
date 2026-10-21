package mari.samba.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import mari.samba.config.SambaProperties;
import mari.samba.core.SambaCommandException;
import mari.samba.core.SshSessionExpiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class SshSessionManagerTest {

    private static final String SECRET = "synthetic-password-for-regression";
    private static final String COMMAND = LinuxCommands.netAdsJoin("admin", SECRET);
    private final Session session = mock(Session.class);
    private final ChannelExec channel = mock(ChannelExec.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(SshSessionManager.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void captureLogs() throws Exception {
        events.start();
        logger.addAppender(events);
        when(session.openChannel("exec")).thenReturn(channel);
        when(channel.getInputStream()).thenReturn(stream(""));
        when(channel.getErrStream()).thenReturn(stream(""));
        when(channel.isConnected()).thenReturn(true);
    }

    @AfterEach
    void detachLogs() {
        logger.detachAppender(events);
        events.stop();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedCommandDoesNotExposeCommandOrRemoteOutput(boolean secretInStderr) throws Exception {
        when(channel.isClosed()).thenReturn(true);
        when(channel.getExitStatus()).thenReturn(7);
        when(channel.getInputStream()).thenReturn(stream(secretInStderr ? "" : SECRET));
        when(channel.getErrStream()).thenReturn(stream(secretInStderr ? SECRET : ""));

        assertThatThrownBy(() -> manager(Duration.ofSeconds(30)).execute("session", COMMAND))
                .isInstanceOf(SambaCommandException.class)
                .hasMessage("SSH command failed with code 7.")
                .satisfies(this::assertSafeException);

        assertSafeLogs();
        verify(channel).setCommand(COMMAND);
        verify(channel).disconnect();
    }

    @Test
    void channelFailureDoesNotRetainSensitiveExceptionCause() throws Exception {
        when(session.openChannel("exec")).thenThrow(new JSchException("failed " + COMMAND));

        assertThatThrownBy(() -> manager(Duration.ofSeconds(30)).execute("session", COMMAND))
                .isInstanceOf(SambaCommandException.class)
                .hasMessage("SSH command execution failed.")
                .hasNoCause()
                .satisfies(this::assertSafeException);

        assertSafeLogs();
    }

    @Test
    void timeoutDoesNotExposeCommandAndDisconnectsChannel() {
        when(channel.isClosed()).thenReturn(false);

        assertThatThrownBy(() -> manager(Duration.ofMillis(-1)).execute("session", COMMAND))
                .isInstanceOf(SambaCommandException.class)
                .hasMessage("SSH command timed out after -1 ms.")
                .satisfies(this::assertSafeException);

        assertSafeLogs();
        verify(channel, org.mockito.Mockito.atLeastOnce()).disconnect();
    }

    @Test
    void successfulCommandPreservesOutputWithoutLoggingIt() throws Exception {
        when(channel.isClosed()).thenReturn(true);
        when(channel.getExitStatus()).thenReturn(0);
        when(channel.getInputStream()).thenReturn(stream(SECRET));

        assertThat(manager(Duration.ofSeconds(30)).execute("session", COMMAND)).isEqualTo(SECRET);

        assertSafeLogs();
        verify(channel).disconnect();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"synthetic-http-session-token"})
    void missingSessionDoesNotExposeSessionToken(String sessionId) {
        SshSessionManager manager = new SshSessionManager(null);

        assertThatThrownBy(() -> manager.getSession(sessionId))
                .isInstanceOf(SshSessionExpiredException.class)
                .hasMessage("SSH session not found or disconnected. Please log in again.");
    }

    private SshSessionManager manager(Duration timeout) {
        SambaProperties properties = new SambaProperties(
                new SambaProperties.Ssh(Duration.ofSeconds(1), timeout, Duration.ofSeconds(30), 3), null, null, null);
        return new SshSessionManager(properties) {
            @Override
            public Session getSession(String sessionId) {
                return session;
            }
        };
    }

    private void assertSafeException(Throwable error) {
        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
        assertThat(stackTrace.toString()).doesNotContain(SECRET, COMMAND);
    }

    private void assertSafeLogs() {
        assertThat(events.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET, COMMAND);
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
