package mari.samba.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import mari.samba.config.ExecutionProperties;
import mari.samba.core.GlobalExceptionHandler;
import mari.samba.infra.CommandResult;
import mari.samba.infra.LocalCommandExecutor;
import mari.samba.infra.LocalCommandPolicy;
import mari.samba.infra.PrivilegedCommand;
import mari.samba.infra.PrivilegedHelperClient;
import mari.samba.infra.PrivilegedOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Component test for diagnostics; HTTP authentication filters are covered separately. */
class AdDiagnosticsIntegrationTest {

    private static final String SECRET = "synthetic-ad-password-for-regression";
    private final PrivilegedHelperClient helper = mock(PrivilegedHelperClient.class);
    private final List<PrivilegedCommand> commands = new ArrayList<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ExecutionProperties properties = new ExecutionProperties(
                ExecutionProperties.Mode.LOCAL, "admin", "local-fixture", Duration.ofSeconds(1), "/unused/helper.sock");
        LocalCommandExecutor executor = new LocalCommandExecutor(properties, new LocalCommandPolicy(), helper);
        mvc = MockMvcBuilders.standaloneSetup(new AdApiController(new AdIntegrationService(executor)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void detachLogs() {
        logger.detachAppender(events);
        events.stop();
    }

    @ParameterizedTest
    @CsvSource({"join, true", "join, false", "leave, true", "leave, false"})
    void helperFailureDoesNotExposeOutputOrContinueWorkflow(String action, boolean secretInStderr) throws Exception {
        respondToDomainCommand(new CommandResult(7, secretInStderr ? "" : SECRET, secretInStderr ? SECRET : ""));

        String body = request(action, 500);

        assertThat(body).contains("Local command failed with code 7.");
        assertSafeDiagnostics(body);
        assertStoppedAfterDomainCommand(action);
    }

    @ParameterizedTest
    @CsvSource({"failed", "ERROR"})
    void rejectedJoinOutputDoesNotExposeCredentialsOrRestartService(String marker) throws Exception {
        respondToDomainCommand(new CommandResult(0, marker + ": " + SECRET, ""));

        String body = request("join", 500);

        assertThat(body).contains("Domain join failed.");
        assertSafeDiagnostics(body);
        assertStoppedAfterDomainCommand("join");
    }

    @Test
    void successfulJoinStillRestartsServiceThenReadsStatus() throws Exception {
        respondToDomainCommand(new CommandResult(0, "Joined domain", ""));

        String body = request("join", 200);

        assertThat(body).contains("\"isJoined\":true");
        assertThat(commands)
                .extracting(PrivilegedCommand::operation)
                .containsExactly(
                        PrivilegedOperation.WRITE_KRB5_CONFIG,
                        PrivilegedOperation.NET_ADS,
                        PrivilegedOperation.SYSTEMD,
                        PrivilegedOperation.NET_ADS);
        assertThat(commands.get(2).arguments()).containsExactly("restart", "winbind");
        assertThat(commands.get(3).arguments()).containsExactly("testjoin");
        assertSafeDiagnostics(body);
    }

    private void respondToDomainCommand(CommandResult result) {
        when(helper.execute(any())).thenAnswer(invocation -> {
            PrivilegedCommand command = invocation.getArgument(0);
            commands.add(command);
            if (command.operation() == PrivilegedOperation.NET_ADS) {
                if (command.arguments().equals(List.of("testjoin"))) {
                    return new CommandResult(0, "Join is OK", "");
                }
                return result;
            }
            return new CommandResult(0, "", "");
        });
    }

    private String request(String action, int expectedStatus) throws Exception {
        String payload = new ObjectMapper()
                .writeValueAsString(new AdJoinRequestDto("example.test", "Administrator", SECRET, null));
        return mvc.perform(post("/api/ad/" + action)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void assertStoppedAfterDomainCommand(String action) {
        assertThat(commands)
                .extracting(PrivilegedCommand::operation)
                .containsExactlyElementsOf(
                        action.equals("join")
                                ? List.of(PrivilegedOperation.WRITE_KRB5_CONFIG, PrivilegedOperation.NET_ADS)
                                : List.of(PrivilegedOperation.NET_ADS));
        PrivilegedCommand domainCommand = commands.getLast();
        assertThat(domainCommand.arguments()).containsExactly(action, "Administrator");
        assertThat(domainCommand.inputData()).isEqualTo(SECRET);
    }

    private void assertSafeDiagnostics(String body) {
        assertThat(body).doesNotContain(SECRET);
        assertThat(events.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET);
            if (event.getThrowableProxy() != null) {
                assertThat(ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(event.getThrowableProxy()))
                        .doesNotContain(SECRET);
            }
        });
    }
}
