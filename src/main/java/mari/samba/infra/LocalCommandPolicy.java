package mari.samba.infra;

import java.util.ArrayList;
import java.util.List;
import mari.samba.core.SambaCommandException;
import org.springframework.stereotype.Component;

@Component
public class LocalCommandPolicy {

    public PrivilegedCommand classify(String command, String inputData) {
        List<String> words = tokenize(command);

        if (shape(words, "cat", "--", null)) {
            return operation(PrivilegedOperation.READ_CONFIG, inputData, words.get(2));
        }
        if (shape(words, "sudo", "cat", "--", null)) {
            return operation(PrivilegedOperation.READ_CONFIG, inputData, words.get(3));
        }
        if (shape(words, "sudo", "base64", "--", null)) {
            return operation(PrivilegedOperation.READ_FILE_BASE64, inputData, words.get(3));
        }
        if (shape(words, "cat", ">", null)) {
            return operation(PrivilegedOperation.WRITE_TEMP_CONFIG, inputData, words.get(2));
        }
        if (shape(words, "base64", "-d", "|", "sudo", "tee", "--", null, ">", "/dev/null")) {
            return operation(PrivilegedOperation.UPLOAD_FILE_BASE64, inputData, words.get(6));
        }
        if (isDiskUsage(words)) {
            return operation(PrivilegedOperation.DISK_USAGE, inputData, words.get(3));
        }
        if (shape(words, "sudo", "df", "-kP", "--", null)) {
            return operation(PrivilegedOperation.FILESYSTEM_USAGE, inputData, words.get(4));
        }
        if (isDirectoryListing(words)) {
            return operation(PrivilegedOperation.LIST_DIRECTORY, inputData, words.get(3));
        }
        if (shape(words, "sudo", "mkdir", "-p", "--", null)) {
            return operation(PrivilegedOperation.CREATE_DIRECTORY, inputData, words.get(4));
        }
        if (shape(words, "sudo", "chmod", null, "--", null)) {
            return operation(PrivilegedOperation.CHANGE_MODE, inputData, words.get(2), words.get(4));
        }
        if (shape(words, "sudo", "chown", "-R", null, "--", null)) {
            return operation(PrivilegedOperation.CHANGE_OWNER, inputData, words.get(3), words.get(5));
        }
        if (shape(words, "sudo", "cp", "--", null, null)) {
            return operation(PrivilegedOperation.COPY, inputData, words.get(3), words.get(4));
        }
        if (shape(words, "sudo", "cp", "-r", "--", null, null)) {
            return operation(PrivilegedOperation.COPY_RECURSIVE, inputData, words.get(4), words.get(5));
        }
        if (shape(words, "sudo", "mv", "--", null, null)) {
            return operation(PrivilegedOperation.MOVE, inputData, words.get(3), words.get(4));
        }
        if (shape(words, "sudo", "rm", "-rf", "--", null)) {
            return operation(PrivilegedOperation.REMOVE_RECURSIVE, inputData, words.get(4));
        }
        if (isBackupListing(words)) {
            return operation(PrivilegedOperation.LIST_BACKUPS, inputData, backupDirectory(words.get(4)));
        }
        if (isBackupCleanup(words)) {
            int firstDeleted = parsePositive(words.get(7).substring(1), "backup retention");
            return operation(
                    PrivilegedOperation.CLEANUP_BACKUPS,
                    inputData,
                    backupDirectory(words.get(2)),
                    Integer.toString(firstDeleted - 1));
        }
        if (shape(words, "sudo", "tail", "-n", null, "--", null)) {
            return operation(PrivilegedOperation.READ_LOG, inputData, words.get(3), words.get(5));
        }
        if (shape(words, "testparm", "-s", "--", null, ">", "/dev/null")) {
            return operation(PrivilegedOperation.VALIDATE_CONFIG, inputData, words.get(3));
        }
        if (shape(words, "testparm", "-sv", "2>/dev/null")) {
            return operation(PrivilegedOperation.DUMP_CONFIG, inputData);
        }
        if (shape(words, "sudo", "systemctl", null, "--", null)) {
            return operation(PrivilegedOperation.SYSTEMD, inputData, words.get(2), words.get(4));
        }
        if (shape(words, "sudo", "pdbedit", "-L")) {
            return operation(PrivilegedOperation.LIST_SAMBA_USERS, inputData);
        }
        if (shape(words, "id", "--", null)) {
            return operation(PrivilegedOperation.USER_EXISTS, inputData, words.get(2));
        }
        if (shape(words, "sudo", "useradd", "-m", "-s", "/bin/bash", "-c", null, "--", null)) {
            return operation(PrivilegedOperation.ADD_SYSTEM_USER, inputData, words.get(8), words.get(6));
        }
        if (shape(words, "sudo", "userdel", "-r", "--", null)) {
            return operation(PrivilegedOperation.DELETE_SYSTEM_USER, inputData, words.get(4));
        }
        if (shape(words, "sudo", "chpasswd")) {
            return operation(PrivilegedOperation.CHANGE_SYSTEM_PASSWORD, inputData);
        }
        String sambaPasswordAction = sambaPasswordAction(words);
        if (sambaPasswordAction != null) {
            return operation(
                    PrivilegedOperation.SAMBA_PASSWORD, inputData, sambaPasswordAction, words.get(words.size() - 1));
        }
        if (shape(words, "getent", "group")) {
            return operation(PrivilegedOperation.LIST_GROUPS, inputData);
        }
        if (shape(words, "sudo", "groupadd", null)) {
            return operation(PrivilegedOperation.ADD_GROUP, inputData, words.get(2));
        }
        if (shape(words, "sudo", "groupdel", null)) {
            return operation(PrivilegedOperation.DELETE_GROUP, inputData, words.get(2));
        }
        if (isGroupMembership(words)) {
            return operation(
                    PrivilegedOperation.CHANGE_GROUP_MEMBERSHIP, inputData, words.get(2), words.get(3), words.get(4));
        }
        if (shape(words, "sudo", "smbstatus", null)) {
            return operation(PrivilegedOperation.SMB_STATUS, inputData, words.get(2));
        }
        if (shape(words, "sudo", "kill", "-9", null)) {
            return operation(PrivilegedOperation.TERMINATE_SAMBA_SESSION, inputData, words.get(3));
        }
        if (isNetAds(words)) {
            if ("testjoin".equals(words.get(3))) {
                return operation(PrivilegedOperation.NET_ADS, inputData, "testjoin");
            }
            String credentials = words.get(5);
            int separator = credentials.indexOf('%');
            if (separator <= 0) {
                throw rejected();
            }
            return operation(
                    PrivilegedOperation.NET_ADS,
                    credentials.substring(separator + 1),
                    words.get(3),
                    credentials.substring(0, separator));
        }
        if (shape(words, "wbinfo", null) && ("-u".equals(words.get(1)) || "-g".equals(words.get(1)))) {
            return operation(PrivilegedOperation.WBINFO, inputData, words.get(1));
        }
        if (shape(words, "sudo", "tee", "/etc/krb5.conf", ">", "/dev/null")) {
            return operation(PrivilegedOperation.WRITE_KRB5_CONFIG, inputData);
        }
        if (shape(words, "df", "-h")) {
            return operation(PrivilegedOperation.FILESYSTEM_OVERVIEW, inputData);
        }

        throw rejected();
    }

    private static boolean isDiskUsage(List<String> words) {
        return words.size() == 26
                && words.subList(0, 3).equals(List.of("if", "[", "-e"))
                && words.get(3).equals(words.get(9))
                && words.get(3).equals(words.get(17))
                && words.subList(4, 9).equals(List.of("];", "then", "du", "-sh", "--"))
                && words.subList(10, 17).equals(List.of("2>/dev/null", "||", "sudo", "-n", "du", "-sh", "--"))
                && words.subList(18, 26)
                        .equals(List.of("2>/dev/null", "||", "echo", "0 B;", "else", "echo", "0 B;", "fi"));
    }

    private static boolean isDirectoryListing(List<String> words) {
        return words.size() == 16
                && words.subList(0, 3).equals(List.of("sudo", "find", "-L"))
                && words.subList(4, 16)
                        .equals(List.of(
                                "-mindepth",
                                "1",
                                "-maxdepth",
                                "1",
                                "!",
                                "-name",
                                ".*",
                                "-printf",
                                "%y|%s|%p\\n",
                                "2>/dev/null",
                                "|",
                                "sort"));
    }

    private static boolean isBackupListing(List<String> words) {
        return shape(words, "ls", "-lh", "--time-style=+%Y-%m-%d %H:%M:%S", "--", null, "2>/dev/null")
                && words.get(4).endsWith("/smb.conf.backup_*");
    }

    private static boolean isBackupCleanup(List<String> words) {
        return words.size() == 14
                && words.get(0).equals("ls")
                && words.get(1).equals("-t")
                && words.get(2).endsWith("/smb.conf.backup_*")
                && words.subList(3, 7).equals(List.of("2>/dev/null", "|", "tail", "-n"))
                && words.get(7).matches("^\\+\\d+$")
                && words.subList(8, 14).equals(List.of("|", "xargs", "-r", "sudo", "rm", "--"));
    }

    private static String sambaPasswordAction(List<String> words) {
        if (shape(words, "sudo", "smbpasswd", "-s", "-a", null)) {
            return "add";
        }
        if (shape(words, "sudo", "smbpasswd", "-e", null)) {
            return "enable";
        }
        if (shape(words, "sudo", "smbpasswd", "-x", null)) {
            return "delete";
        }
        if (shape(words, "sudo", "smbpasswd", "-s", null)) {
            return "change";
        }
        return null;
    }

    private static boolean isGroupMembership(List<String> words) {
        return words.size() == 5
                && words.get(0).equals("sudo")
                && words.get(1).equals("gpasswd")
                && List.of("-a", "-d").contains(words.get(2));
    }

    private static boolean isNetAds(List<String> words) {
        if (shape(words, "sudo", "net", "ads", "testjoin")) {
            return true;
        }
        return words.size() == 6
                && words.subList(0, 3).equals(List.of("sudo", "net", "ads"))
                && List.of("join", "leave").contains(words.get(3))
                && "-U".equals(words.get(4));
    }

    private static String backupDirectory(String glob) {
        return glob.substring(0, glob.length() - "/smb.conf.backup_*".length());
    }

    private static int parsePositive(String value, String field) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new SambaCommandException("Invalid " + field + ".");
        }
    }

    private static PrivilegedCommand operation(PrivilegedOperation operation, String inputData, String... arguments) {
        return new PrivilegedCommand(operation, List.of(arguments), inputData);
    }

    private static boolean shape(List<String> words, String... expected) {
        if (words.size() != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != null && !expected[i].equals(words.get(i))) {
                return false;
            }
        }
        return true;
    }

    static List<String> tokenize(String command) {
        if (command == null || command.isBlank() || command.indexOf('\0') >= 0) {
            throw rejected();
        }
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        boolean tokenStarted = false;

        for (int i = 0; i < command.length(); i++) {
            char character = command.charAt(i);
            if (escaped) {
                current.append(character);
                escaped = false;
                tokenStarted = true;
            } else if (character == '\\' && !singleQuoted) {
                escaped = true;
                tokenStarted = true;
            } else if (character == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
                tokenStarted = true;
            } else if (character == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
                tokenStarted = true;
            } else if (Character.isWhitespace(character) && !singleQuoted && !doubleQuoted) {
                if (tokenStarted) {
                    words.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
            } else {
                current.append(character);
                tokenStarted = true;
            }
        }

        if (escaped || singleQuoted || doubleQuoted) {
            throw rejected();
        }
        if (tokenStarted) {
            words.add(current.toString());
        }
        return List.copyOf(words);
    }

    private static SambaCommandException rejected() {
        return new SambaCommandException("Command is not allowed in local mode.");
    }
}
