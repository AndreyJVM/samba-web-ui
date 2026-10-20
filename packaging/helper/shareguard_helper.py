#!/usr/bin/python3

import argparse
import base64
import glob
import grp
import json
import os
import pathlib
import pwd
import re
import shutil
import signal
import socket
import stat
import struct
import subprocess
import tempfile
from concurrent.futures import ThreadPoolExecutor


MAX_FRAME_SIZE = 128 * 1024 * 1024
SAFE_PATH = "/usr/sbin:/usr/bin:/sbin:/bin"
USERNAME_PATTERN = re.compile(r"^[a-z_][a-z0-9_.-]{0,31}$")
GROUP_PATTERN = re.compile(r"^[a-z_][a-z0-9_.-]{0,31}$")
OWNER_PATTERN = re.compile(r"^([a-z_][a-z0-9_.-]*(:[a-z_][a-z0-9_.-]*)?|:[a-z_][a-z0-9_.-]+)$")
BACKUP_PATTERN = re.compile(r"^smb\.conf\.backup_\d{8}_\d{6}$")
AD_USERNAME_PATTERN = re.compile(r"^[A-Za-z0-9_.@\\-]{1,128}$")
SYSTEM_USERS = {
    "root", "daemon", "bin", "sys", "sync", "games", "man", "lp", "mail", "news",
    "uucp", "proxy", "www-data", "backup", "list", "irc", "gnats", "nobody",
    "systemd-network", "systemd-resolve", "syslog", "messagebus", "_apt", "lxd",
    "uuidd", "dnsmasq", "landscape", "pollinate", "sshd", "postgres", "mysql",
    "shareguard",
}


class PolicyError(Exception):
    pass


class HelperPolicy:
    def __init__(self, environ=None):
        env = os.environ if environ is None else environ
        self.allowed_roots = tuple(
            self._absolute(item)
            for item in env.get("SHAREGUARD_ALLOWED_ROOTS", "/mnt:/media:/srv:/data:/home").split(":")
            if item
        )
        self.config_path = self._configured_path(env.get("SHAREGUARD_CONFIG_PATH", "/etc/samba/smb.conf"))
        self.backup_dir = self._configured_path(env.get("SHAREGUARD_BACKUP_DIR", "/etc/samba/backups"))
        self.log_path = self._configured_path(env.get("SHAREGUARD_LOG_PATH", "/var/log/samba/log.smbd"))
        self.temp_config = self._configured_path(env.get("SHAREGUARD_TEMP_CONFIG", "/tmp/smb.conf.tmp"))
        self.krb5_config = self._configured_path(env.get("SHAREGUARD_KRB5_CONFIG", "/etc/krb5.conf"))
        self.group_prefix = env.get("SHAREGUARD_GROUP_PREFIX", "smb_")
        self.timeout = int(env.get("SHAREGUARD_HELPER_TIMEOUT", "30"))

    def dispatch(self, request):
        operation = request.get("operation")
        arguments = request.get("arguments")
        input_data = request.get("inputData")
        if not isinstance(operation, str) or not isinstance(arguments, list):
            raise PolicyError("Malformed helper request.")
        if any(not isinstance(argument, str) for argument in arguments):
            raise PolicyError("Operation arguments must be strings.")
        if input_data is not None and not isinstance(input_data, str):
            raise PolicyError("Operation input must be text.")

        handler = getattr(self, "op_" + operation.lower(), None)
        if handler is None:
            raise PolicyError("Operation is not allowed.")
        return handler(arguments, input_data)

    def op_read_config(self, args, input_data):
        self._count(args, 1)
        path = self._exact_path(args[0], self.config_path, pathlib.Path(self.config_path).parent)
        return self._run(["cat", "--", path])

    def op_read_file_base64(self, args, input_data):
        self._count(args, 1)
        path = self._data_path(args[0])
        return self._run(["base64", "--", path])

    def op_write_temp_config(self, args, input_data):
        self._count(args, 1)
        path = self._exact_path(args[0], self.temp_config, pathlib.Path(self.temp_config).parent)
        self._replace_file(path, self._required_input(input_data).encode(), 0o600)
        return self._ok()

    def op_upload_file_base64(self, args, input_data):
        self._count(args, 1)
        path = self._data_path(args[0])
        try:
            content = base64.b64decode(self._required_input(input_data), validate=True)
        except ValueError as error:
            raise PolicyError("Uploaded content is not valid base64.") from error
        self._replace_file(path, content, 0o640)
        return self._ok()

    def op_disk_usage(self, args, input_data):
        self._count(args, 1)
        path = self._data_path(args[0])
        if not os.path.exists(path):
            return self._ok("0 B\n")
        return self._run(["du", "-sh", "--", path])

    def op_filesystem_usage(self, args, input_data):
        self._count(args, 1)
        if args[0] != "/":
            path = self._data_path(args[0])
        else:
            path = "/"
        return self._run(["df", "-kP", "--", path])

    def op_list_directory(self, args, input_data):
        self._count(args, 1)
        directory = self._data_path(args[0])
        rows = []
        with os.scandir(directory) as entries:
            for entry in entries:
                if entry.name.startswith("."):
                    continue
                resolved = self._data_path(entry.path)
                info = os.stat(resolved)
                kind = "d" if stat.S_ISDIR(info.st_mode) else "f" if stat.S_ISREG(info.st_mode) else "l"
                rows.append(f"{kind}|{info.st_size}|{entry.path}")
        return self._ok("\n".join(sorted(rows)) + ("\n" if rows else ""))

    def op_create_directory(self, args, input_data):
        self._count(args, 1)
        path = self._data_path(args[0])
        os.makedirs(path, mode=0o775, exist_ok=True)
        return self._ok()

    def op_change_mode(self, args, input_data):
        self._count(args, 2)
        if not re.fullmatch(r"[0-7]{3,4}", args[0]):
            raise PolicyError("Invalid file mode.")
        path = self._data_path(args[1])
        os.chmod(path, int(args[0], 8), follow_symlinks=True)
        return self._ok()

    def op_change_owner(self, args, input_data):
        self._count(args, 2)
        if not OWNER_PATTERN.fullmatch(args[0]):
            raise PolicyError("Invalid owner.")
        path = self._data_path(args[1])
        return self._run(["chown", "-R", args[0], "--", path])

    def op_copy(self, args, input_data):
        self._count(args, 2)
        source, destination = self._copy_paths(args[0], args[1])
        return self._run(["cp", "--", source, destination])

    def op_copy_recursive(self, args, input_data):
        self._count(args, 2)
        source = self._data_path(args[0])
        destination = self._data_path(args[1])
        return self._run(["cp", "-r", "--", source, destination])

    def op_move(self, args, input_data):
        self._count(args, 2)
        if args[0] == self.temp_config and args[1] == self.config_path:
            source = self._exact_path(args[0], self.temp_config, pathlib.Path(self.temp_config).parent)
            destination = self._exact_path(args[1], self.config_path, pathlib.Path(self.config_path).parent)
        else:
            source = self._data_path(args[0])
            destination = self._data_path(args[1])
            self._not_root(args[0])
        return self._run(["mv", "--", source, destination])

    def op_remove_recursive(self, args, input_data):
        self._count(args, 1)
        path = self._data_path(args[0])
        self._not_root(args[0])
        return self._run(["rm", "-rf", "--", path])

    def op_list_backups(self, args, input_data):
        self._count(args, 1)
        self._backup_directory(args[0])
        files = sorted(glob.glob(os.path.join(self.backup_dir, "smb.conf.backup_*")))
        if not files:
            return self._ok()
        return self._run(["ls", "-lh", "--time-style=+%Y-%m-%d %H:%M:%S", "--", *files])

    def op_cleanup_backups(self, args, input_data):
        self._count(args, 2)
        self._backup_directory(args[0])
        keep = self._bounded_int(args[1], 1, 100, "backup retention")
        files = sorted(
            glob.glob(os.path.join(self.backup_dir, "smb.conf.backup_*")),
            key=os.path.getmtime,
            reverse=True,
        )
        for filename in files[keep:]:
            if BACKUP_PATTERN.fullmatch(os.path.basename(filename)):
                os.unlink(filename)
        return self._ok()

    def op_read_log(self, args, input_data):
        self._count(args, 2)
        lines = self._bounded_int(args[0], 1, 5000, "line count")
        path = self._exact_path(args[1], self.log_path, pathlib.Path(self.log_path).parent)
        return self._run(["tail", "-n", str(lines), "--", path])

    def op_validate_config(self, args, input_data):
        self._count(args, 1)
        path = self._exact_path(args[0], self.temp_config, pathlib.Path(self.temp_config).parent)
        return self._run(["testparm", "-s", "--", path])

    def op_dump_config(self, args, input_data):
        self._count(args, 0)
        return self._run(["testparm", "-sv"])

    def op_systemd(self, args, input_data):
        self._count(args, 2)
        if args[0] not in {"start", "stop", "restart", "reload", "status", "is-active"}:
            raise PolicyError("Systemd action is not allowed.")
        if args[1] not in {"smbd", "winbind"}:
            raise PolicyError("Systemd service is not allowed.")
        return self._run(["systemctl", args[0], "--", args[1]])

    def op_list_samba_users(self, args, input_data):
        self._count(args, 0)
        return self._run(["pdbedit", "-L"])

    def op_list_system_users(self, args, input_data):
        self._count(args, 0)
        return self._run(["getent", "passwd"])

    def op_user_exists(self, args, input_data):
        self._count(args, 1)
        self._username(args[0], allow_system=True)
        return self._run(["id", "--", args[0]])

    def op_add_system_user(self, args, input_data):
        self._count(args, 2)
        self._username(args[0])
        if "\n" in args[1] or "\r" in args[1] or len(args[1]) > 256:
            raise PolicyError("Invalid user comment.")
        return self._run(["useradd", "-m", "-s", "/bin/bash", "-c", args[1], "--", args[0]])

    def op_delete_system_user(self, args, input_data):
        self._count(args, 1)
        self._username(args[0])
        return self._run(["userdel", "-r", "--", args[0]])

    def op_change_system_password(self, args, input_data):
        self._count(args, 0)
        value = self._required_input(input_data)
        if value.count("\n") != 1 or not value.endswith("\n") or ":" not in value:
            raise PolicyError("Invalid password update payload.")
        username, password = value[:-1].split(":", 1)
        self._username(username)
        self._password(password)
        return self._run(["chpasswd"], value)

    def op_samba_password(self, args, input_data):
        self._count(args, 2)
        action, username = args
        self._username(username)
        commands = {
            "add": ["smbpasswd", "-s", "-a", username],
            "enable": ["smbpasswd", "-e", username],
            "change": ["smbpasswd", "-s", username],
            "delete": ["smbpasswd", "-x", username],
        }
        if action not in commands:
            raise PolicyError("Samba password action is not allowed.")
        if action in {"add", "change"}:
            values = self._required_input(input_data).splitlines()
            if len(values) != 2 or values[0] != values[1]:
                raise PolicyError("Invalid Samba password payload.")
            self._password(values[0])
        elif input_data not in {None, ""}:
            raise PolicyError("Unexpected Samba password payload.")
        return self._run(commands[action], input_data)

    def op_list_groups(self, args, input_data):
        self._count(args, 0)
        return self._run(["getent", "group"])

    def op_add_group(self, args, input_data):
        self._count(args, 1)
        self._group(args[0])
        return self._run(["groupadd", args[0]])

    def op_delete_group(self, args, input_data):
        self._count(args, 1)
        self._group(args[0])
        return self._run(["groupdel", args[0]])

    def op_change_group_membership(self, args, input_data):
        self._count(args, 3)
        if args[0] not in {"-a", "-d"}:
            raise PolicyError("Group membership action is not allowed.")
        self._username(args[1])
        self._group(args[2])
        return self._run(["gpasswd", args[0], args[1], args[2]])

    def op_smb_status(self, args, input_data):
        self._count(args, 1)
        if args[0] not in {"-b", "-L"}:
            raise PolicyError("smbstatus flag is not allowed.")
        return self._run(["smbstatus", args[0]])

    def op_terminate_samba_session(self, args, input_data):
        self._count(args, 1)
        pid = self._bounded_int(args[0], 2, 2**31 - 1, "PID")
        try:
            process_name = pathlib.Path(f"/proc/{pid}/comm").read_text(encoding="utf-8").strip()
        except OSError as error:
            raise PolicyError("Samba process does not exist.") from error
        if process_name not in {"smbd", "samba"}:
            raise PolicyError("Only Samba worker processes may be terminated.")
        os.kill(pid, signal.SIGKILL)
        return self._ok()

    def op_net_ads(self, args, input_data):
        if args == ["testjoin"]:
            return self._run(["net", "ads", "testjoin"])
        self._count(args, 2)
        action, username = args
        if action not in {"join", "leave"} or not AD_USERNAME_PATTERN.fullmatch(username):
            raise PolicyError("Invalid Active Directory operation.")
        password = self._required_input(input_data)
        self._password(password)
        return self._run(["net", "ads", action, "-U", f"{username}%{password}"])

    def op_wbinfo(self, args, input_data):
        self._count(args, 1)
        if args[0] not in {"-u", "-g"}:
            raise PolicyError("wbinfo flag is not allowed.")
        return self._run(["wbinfo", args[0]])

    def op_write_krb5_config(self, args, input_data):
        self._count(args, 0)
        content = self._required_input(input_data)
        if len(content) > 1024 * 1024:
            raise PolicyError("Kerberos configuration is too large.")
        path = self._exact_path(self.krb5_config, self.krb5_config, pathlib.Path(self.krb5_config).parent)
        self._replace_file(path, content.encode(), 0o644)
        return self._ok()

    def op_filesystem_overview(self, args, input_data):
        self._count(args, 0)
        return self._run(["df", "-h"])

    def _run(self, command, input_data=None):
        executable = shutil.which(command[0], path=SAFE_PATH)
        if executable is None:
            return self._response(127, "", f"Required executable is unavailable: {command[0]}")
        env = {"PATH": SAFE_PATH, "LANG": "C.UTF-8", "LC_ALL": "C.UTF-8"}
        try:
            completed = subprocess.run(
                [executable, *command[1:]], input=input_data, text=True, capture_output=True,
                timeout=self.timeout, check=False, env=env,
            )
            return self._response(completed.returncode, completed.stdout, completed.stderr)
        except subprocess.TimeoutExpired:
            return self._response(124, "", "Operation timed out.")

    def _copy_paths(self, source, destination):
        if self._is_data_path(source) and self._is_data_path(destination):
            return self._data_path(source), self._data_path(destination)
        source_path = pathlib.Path(source)
        destination_path = pathlib.Path(destination)
        config_to_backup = source == self.config_path and destination_path.parent == pathlib.Path(self.backup_dir) and BACKUP_PATTERN.fullmatch(destination_path.name)
        backup_to_config = destination == self.config_path and source_path.parent == pathlib.Path(self.backup_dir) and BACKUP_PATTERN.fullmatch(source_path.name)
        if not (config_to_backup or backup_to_config):
            raise PolicyError("Copy paths are not allowed.")
        config = self._exact_path(self.config_path, self.config_path, pathlib.Path(self.config_path).parent)
        backup_root = self._backup_directory(self.backup_dir)
        if config_to_backup:
            backup_file = self._absolute(destination)
            if not self._within(backup_file, backup_root):
                raise PolicyError("Backup path resolves outside the backup directory.")
            return config, backup_file
        backup_file = self._absolute(source)
        if not self._within(backup_file, backup_root):
            raise PolicyError("Backup path resolves outside the backup directory.")
        return backup_file, config

    def _data_path(self, value):
        path = self._absolute(value)
        if not any(self._within(path, root) for root in self.allowed_roots):
            raise PolicyError("Path is outside configured data roots.")
        return path

    def _is_data_path(self, value):
        try:
            self._data_path(value)
            return True
        except PolicyError:
            return False

    def _not_root(self, value):
        canonical = self._absolute(value)
        if any(canonical == root for root in self.allowed_roots):
            raise PolicyError("Configured data roots cannot be modified or removed.")

    def _backup_directory(self, value):
        return self._exact_path(value, self.backup_dir, pathlib.Path(self.backup_dir).parent)

    def _exact_path(self, value, expected, allowed_parent):
        if value != expected:
            raise PolicyError("Path does not match the configured system path.")
        canonical = self._absolute(value)
        parent = self._absolute(str(allowed_parent))
        if not self._within(canonical, parent):
            raise PolicyError("Configured system path resolves outside its directory.")
        return canonical

    @staticmethod
    def _absolute(value):
        if not isinstance(value, str) or not value.startswith("/") or "\0" in value:
            raise PolicyError("Absolute path is required.")
        lexical = pathlib.PurePosixPath(value)
        if ".." in lexical.parts:
            raise PolicyError("Parent path segments are not allowed.")
        return os.path.realpath(value)

    @staticmethod
    def _configured_path(value):
        if not isinstance(value, str) or not value.startswith("/") or "\0" in value:
            raise PolicyError("Absolute path is required.")
        normalized = os.path.normpath(value)
        if ".." in pathlib.PurePosixPath(value).parts:
            raise PolicyError("Parent path segments are not allowed.")
        return normalized

    @staticmethod
    def _within(path, root):
        try:
            return os.path.commonpath((path, root)) == root
        except ValueError:
            return False

    def _username(self, username, allow_system=False):
        if not USERNAME_PATTERN.fullmatch(username):
            raise PolicyError("Invalid username.")
        if not allow_system and username.lower() in SYSTEM_USERS:
            raise PolicyError("Protected system user cannot be modified.")

    def _group(self, group_name):
        if not GROUP_PATTERN.fullmatch(group_name) or not group_name.startswith(self.group_prefix):
            raise PolicyError("Only configured Samba groups may be modified.")

    @staticmethod
    def _password(value):
        if not value or "\n" in value or "\r" in value or len(value) > 1024:
            raise PolicyError("Invalid password payload.")

    @staticmethod
    def _required_input(value):
        if value is None:
            raise PolicyError("Operation input is required.")
        return value

    @staticmethod
    def _count(args, expected):
        if len(args) != expected:
            raise PolicyError("Unexpected operation argument count.")

    @staticmethod
    def _bounded_int(value, minimum, maximum, field):
        try:
            parsed = int(value)
        except (TypeError, ValueError) as error:
            raise PolicyError(f"Invalid {field}.") from error
        if parsed < minimum or parsed > maximum:
            raise PolicyError(f"Invalid {field}.")
        return parsed

    @staticmethod
    def _replace_file(filename, content, mode):
        parent = os.path.dirname(filename)
        os.makedirs(parent, mode=0o750, exist_ok=True)
        descriptor, temporary = tempfile.mkstemp(prefix=".shareguard-", dir=parent)
        try:
            os.fchmod(descriptor, mode)
            with os.fdopen(descriptor, "wb") as stream:
                stream.write(content)
                stream.flush()
                os.fsync(stream.fileno())
            descriptor = -1
            os.replace(temporary, filename)
        except Exception:
            if descriptor >= 0:
                try:
                    os.close(descriptor)
                except OSError:
                    pass
            try:
                os.unlink(temporary)
            except OSError:
                pass
            raise

    @staticmethod
    def _ok(stdout=""):
        return HelperPolicy._response(0, stdout, "")

    @staticmethod
    def _response(exit_code, stdout, stderr):
        return {"exitCode": exit_code, "stdout": stdout, "stderr": stderr}


def read_exact(connection, size):
    chunks = bytearray()
    while len(chunks) < size:
        chunk = connection.recv(size - len(chunks))
        if not chunk:
            raise ConnectionError("Incomplete helper frame.")
        chunks.extend(chunk)
    return bytes(chunks)


def read_frame(connection):
    size = struct.unpack(">I", read_exact(connection, 4))[0]
    if size < 1 or size > MAX_FRAME_SIZE:
        raise PolicyError("Invalid helper frame size.")
    return json.loads(read_exact(connection, size).decode("utf-8"))


def write_frame(connection, response):
    encoded = json.dumps(response, separators=(",", ":")).encode("utf-8")
    if len(encoded) > MAX_FRAME_SIZE:
        encoded = json.dumps(HelperPolicy._response(1, "", "Helper response is too large.")).encode("utf-8")
    connection.sendall(struct.pack(">I", len(encoded)) + encoded)


def handle_connection(connection, policy, allowed_uid):
    with connection:
        _, peer_uid, _ = struct.unpack("3i", connection.getsockopt(socket.SOL_SOCKET, socket.SO_PEERCRED, 12))
        if peer_uid != allowed_uid:
            write_frame(connection, HelperPolicy._response(126, "", "Peer is not authorized."))
            return
        try:
            response = policy.dispatch(read_frame(connection))
        except PolicyError as error:
            response = HelperPolicy._response(126, "", str(error))
        except Exception:
            response = HelperPolicy._response(1, "", "Privileged operation failed.")
        write_frame(connection, response)


def serve(socket_path, service_user, workers):
    policy = HelperPolicy()
    account = pwd.getpwnam(service_user)
    group = grp.getgrnam(service_user)
    socket_file = pathlib.Path(socket_path)
    socket_file.parent.mkdir(mode=0o750, parents=True, exist_ok=True)
    try:
        socket_file.unlink()
    except FileNotFoundError:
        pass

    with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as server:
        server.bind(socket_path)
        socket_owner = 0 if os.geteuid() == 0 else account.pw_uid
        os.chown(socket_path, socket_owner, group.gr_gid)
        os.chmod(socket_path, 0o660)
        server.listen(workers * 2)
        with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="shareguard-helper") as pool:
            while True:
                connection, _ = server.accept()
                pool.submit(handle_connection, connection, policy, account.pw_uid)


def main():
    parser = argparse.ArgumentParser(description="ShareGuard privileged operation helper")
    parser.add_argument("--socket", default=os.environ.get("SHAREGUARD_HELPER_SOCKET", "/run/shareguard/helper.sock"))
    parser.add_argument("--user", default=os.environ.get("SHAREGUARD_HELPER_USER", "shareguard"))
    parser.add_argument("--workers", type=int, default=4)
    args = parser.parse_args()
    if args.workers < 1 or args.workers > 32:
        parser.error("workers must be between 1 and 32")
    serve(args.socket, args.user, args.workers)


if __name__ == "__main__":
    main()
