package mari.samba.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LocalCommandPolicyTest {

    private final LocalCommandPolicy policy = new LocalCommandPolicy();

    @ParameterizedTest
    @MethodSource("allowedCommands")
    void classifiesGeneratedCommands(String command, String input, PrivilegedOperation operation, String... arguments) {
        PrivilegedCommand classified = policy.classify(command, input);

        assertThat(classified.operation()).isEqualTo(operation);
        assertThat(classified.arguments()).containsExactly(arguments);
        assertThat(classified.inputData()).isEqualTo(input);
    }

    @Test
    void preservesQuotedApostrophesWithoutAllowingExtraShellTokens() {
        PrivilegedCommand classified = policy.classify(LinuxCommands.mkdir("/srv/O'Connor"), null);

        assertThat(classified.arguments()).containsExactly("/srv/O'Connor");
        assertThatThrownBy(() -> policy.classify("sudo mkdir -p -- '/srv/data'; reboot", null))
                .isInstanceOf(mari.samba.core.SambaCommandException.class)
                .hasMessage("Command is not allowed in local mode.");
    }

    private static Stream<Arguments> allowedCommands() {
        return Stream.of(
                arguments(
                        LinuxCommands.cat("/etc/samba/smb.conf"),
                        null,
                        PrivilegedOperation.READ_CONFIG,
                        "/etc/samba/smb.conf"),
                arguments(LinuxCommands.base64("/srv/file"), null, PrivilegedOperation.READ_FILE_BASE64, "/srv/file"),
                arguments(
                        LinuxCommands.writeToFileStdin("/tmp/smb.conf.tmp"),
                        "config",
                        PrivilegedOperation.WRITE_TEMP_CONFIG,
                        "/tmp/smb.conf.tmp"),
                arguments(
                        LinuxCommands.uploadBase64Stdin("/srv/file"),
                        "ZmlsZQ==",
                        PrivilegedOperation.UPLOAD_FILE_BASE64,
                        "/srv/file"),
                arguments(LinuxCommands.du("/srv/share"), null, PrivilegedOperation.DISK_USAGE, "/srv/share"),
                arguments(LinuxCommands.df("/srv"), null, PrivilegedOperation.FILESYSTEM_USAGE, "/srv"),
                arguments(LinuxCommands.findDirectories("/srv"), null, PrivilegedOperation.LIST_DIRECTORY, "/srv"),
                arguments(LinuxCommands.mkdir("/srv/new"), null, PrivilegedOperation.CREATE_DIRECTORY, "/srv/new"),
                arguments(
                        LinuxCommands.chmod("0775", "/srv/new"),
                        null,
                        PrivilegedOperation.CHANGE_MODE,
                        "0775",
                        "/srv/new"),
                arguments(
                        LinuxCommands.chownRecursive("alice:smb_users", "/srv/new"),
                        null,
                        PrivilegedOperation.CHANGE_OWNER,
                        "alice:smb_users",
                        "/srv/new"),
                arguments(
                        LinuxCommands.copy("/etc/samba/smb.conf", "/etc/samba/backups/smb.conf.backup_20261018_100000"),
                        null,
                        PrivilegedOperation.COPY,
                        "/etc/samba/smb.conf",
                        "/etc/samba/backups/smb.conf.backup_20261018_100000"),
                arguments(
                        LinuxCommands.copyRecursive("/srv/a", "/srv/b"),
                        null,
                        PrivilegedOperation.COPY_RECURSIVE,
                        "/srv/a",
                        "/srv/b"),
                arguments(LinuxCommands.move("/srv/a", "/srv/b"), null, PrivilegedOperation.MOVE, "/srv/a", "/srv/b"),
                arguments(
                        LinuxCommands.removeRecursive("/srv/a"), null, PrivilegedOperation.REMOVE_RECURSIVE, "/srv/a"),
                arguments(
                        LinuxCommands.listBackupsDetailed("/etc/samba/backups"),
                        null,
                        PrivilegedOperation.LIST_BACKUPS,
                        "/etc/samba/backups"),
                arguments(
                        LinuxCommands.cleanupOldBackups("/etc/samba/backups", 10),
                        null,
                        PrivilegedOperation.CLEANUP_BACKUPS,
                        "/etc/samba/backups",
                        "10"),
                arguments(
                        LinuxCommands.tail("/var/log/samba/log.smbd", 200),
                        null,
                        PrivilegedOperation.READ_LOG,
                        "200",
                        "/var/log/samba/log.smbd"),
                arguments(
                        LinuxCommands.testparmSilent("/tmp/smb.conf.tmp"),
                        null,
                        PrivilegedOperation.VALIDATE_CONFIG,
                        "/tmp/smb.conf.tmp"),
                arguments(LinuxCommands.testparmDump(), null, PrivilegedOperation.DUMP_CONFIG),
                arguments(
                        LinuxCommands.systemctl("restart", "smbd"),
                        null,
                        PrivilegedOperation.SYSTEMD,
                        "restart",
                        "smbd"),
                arguments(LinuxCommands.listSambaUsers(), null, PrivilegedOperation.LIST_SAMBA_USERS),
                arguments(LinuxCommands.checkUserExists("alice"), null, PrivilegedOperation.USER_EXISTS, "alice"),
                arguments(
                        LinuxCommands.addSystemUserWithHome("alice", "Alice Admin"),
                        null,
                        PrivilegedOperation.ADD_SYSTEM_USER,
                        "alice",
                        "Alice Admin"),
                arguments(
                        LinuxCommands.deleteSystemUser("alice"), null, PrivilegedOperation.DELETE_SYSTEM_USER, "alice"),
                arguments(LinuxCommands.chpasswd(), "alice:secret\n", PrivilegedOperation.CHANGE_SYSTEM_PASSWORD),
                arguments(
                        LinuxCommands.addSambaUser("alice"),
                        "secret\nsecret\n",
                        PrivilegedOperation.SAMBA_PASSWORD,
                        "add",
                        "alice"),
                arguments(
                        LinuxCommands.enableSambaUser("alice"),
                        null,
                        PrivilegedOperation.SAMBA_PASSWORD,
                        "enable",
                        "alice"),
                arguments(
                        LinuxCommands.changeSambaPassword("alice"),
                        "secret\nsecret\n",
                        PrivilegedOperation.SAMBA_PASSWORD,
                        "change",
                        "alice"),
                arguments(
                        LinuxCommands.deleteSambaUser("alice"),
                        null,
                        PrivilegedOperation.SAMBA_PASSWORD,
                        "delete",
                        "alice"),
                arguments(LinuxCommands.getentGroup(), null, PrivilegedOperation.LIST_GROUPS),
                arguments(LinuxCommands.addGroup("smb_users"), null, PrivilegedOperation.ADD_GROUP, "smb_users"),
                arguments(LinuxCommands.deleteGroup("smb_users"), null, PrivilegedOperation.DELETE_GROUP, "smb_users"),
                arguments(
                        LinuxCommands.addUserToGroup("alice", "smb_users"),
                        null,
                        PrivilegedOperation.CHANGE_GROUP_MEMBERSHIP,
                        "-a",
                        "alice",
                        "smb_users"),
                arguments(
                        LinuxCommands.removeUserFromGroup("alice", "smb_users"),
                        null,
                        PrivilegedOperation.CHANGE_GROUP_MEMBERSHIP,
                        "-d",
                        "alice",
                        "smb_users"),
                arguments(LinuxCommands.smbstatus("-b"), null, PrivilegedOperation.SMB_STATUS, "-b"),
                arguments(LinuxCommands.kill("1234"), null, PrivilegedOperation.TERMINATE_SAMBA_SESSION, "1234"),
                arguments(
                        LinuxCommands.netAdsJoin("administrator", "secret"),
                        "secret",
                        PrivilegedOperation.NET_ADS,
                        "join",
                        "administrator"),
                arguments(
                        LinuxCommands.netAdsLeave("administrator", "secret"),
                        "secret",
                        PrivilegedOperation.NET_ADS,
                        "leave",
                        "administrator"),
                arguments(LinuxCommands.netAdsTestJoin(), null, PrivilegedOperation.NET_ADS, "testjoin"),
                arguments(LinuxCommands.wbinfoUsers(), null, PrivilegedOperation.WBINFO, "-u"),
                arguments(LinuxCommands.wbinfoGroups(), null, PrivilegedOperation.WBINFO, "-g"),
                arguments(LinuxCommands.writeKrb5Conf(), "[libdefaults]", PrivilegedOperation.WRITE_KRB5_CONFIG),
                arguments("df -h", null, PrivilegedOperation.FILESYSTEM_OVERVIEW));
    }

    private static Arguments arguments(
            String command, String input, PrivilegedOperation operation, String... operationArguments) {
        return Arguments.of(command, input, operation, operationArguments);
    }
}
