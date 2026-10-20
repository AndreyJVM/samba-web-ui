import base64
import importlib.util
import json
import os
import pathlib
import socket
import struct
import tempfile
import unittest
from unittest.mock import patch


MODULE_PATH = pathlib.Path(__file__).with_name("shareguard_helper.py")
SPEC = importlib.util.spec_from_file_location("shareguard_helper", MODULE_PATH)
helper = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helper)


class HelperPolicyTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        root = pathlib.Path(self.temporary.name)
        self.data = root / "data"
        self.outside = root / "outside"
        self.system = root / "system"
        self.data.mkdir()
        self.outside.mkdir()
        self.system.mkdir()
        self.config = self.system / "smb.conf"
        self.backups = self.system / "backups"
        self.log = self.system / "log.smbd"
        self.temp_config = self.system / "smb.conf.tmp"
        self.krb5 = self.system / "krb5.conf"
        self.config.write_text("[global]\n", encoding="utf-8")
        self.backups.mkdir()
        self.log.write_text("log\n", encoding="utf-8")
        self.policy = helper.HelperPolicy(
            {
                "SHAREGUARD_ALLOWED_ROOTS": str(self.data),
                "SHAREGUARD_CONFIG_PATH": str(self.config),
                "SHAREGUARD_BACKUP_DIR": str(self.backups),
                "SHAREGUARD_LOG_PATH": str(self.log),
                "SHAREGUARD_TEMP_CONFIG": str(self.temp_config),
                "SHAREGUARD_KRB5_CONFIG": str(self.krb5),
                "SHAREGUARD_GROUP_PREFIX": "smb_",
                "SHAREGUARD_HELPER_TIMEOUT": "1",
            }
        )

    def tearDown(self):
        self.temporary.cleanup()

    def test_writes_only_configured_temp_file(self):
        response = self.policy.dispatch(
            {"operation": "WRITE_TEMP_CONFIG", "arguments": [str(self.temp_config)], "inputData": "content"}
        )

        self.assertEqual(0, response["exitCode"])
        self.assertEqual("content", self.temp_config.read_text(encoding="utf-8"))
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "WRITE_TEMP_CONFIG", "arguments": [str(self.data / "other")], "inputData": "x"}
            )

    def test_upload_decodes_base64_inside_data_root(self):
        destination = self.data / "file.txt"
        response = self.policy.dispatch(
            {
                "operation": "UPLOAD_FILE_BASE64",
                "arguments": [str(destination)],
                "inputData": base64.b64encode(b"payload").decode(),
            }
        )

        self.assertEqual(0, response["exitCode"])
        self.assertEqual(b"payload", destination.read_bytes())

    def test_symlink_is_allowed_only_when_target_remains_inside_root(self):
        inside = self.data / "inside.txt"
        inside.write_text("inside", encoding="utf-8")
        safe_link = self.data / "safe-link"
        safe_link.symlink_to(inside)
        outside = self.outside / "outside.txt"
        outside.write_text("outside", encoding="utf-8")
        unsafe_link = self.data / "unsafe-link"
        unsafe_link.symlink_to(outside)

        with patch.object(self.policy, "_run", return_value={"exitCode": 0, "stdout": "", "stderr": ""}) as run:
            self.policy.dispatch(
                {"operation": "READ_FILE_BASE64", "arguments": [str(safe_link)], "inputData": None}
            )
            self.assertEqual(str(inside), run.call_args.args[0][-1])

        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "READ_FILE_BASE64", "arguments": [str(unsafe_link)], "inputData": None}
            )

    def test_rejects_destructive_root_and_protected_accounts(self):
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "REMOVE_RECURSIVE", "arguments": [str(self.data)], "inputData": None}
            )
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "DELETE_SYSTEM_USER", "arguments": ["root"], "inputData": None}
            )
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "ADD_GROUP", "arguments": ["wheel"], "inputData": None}
            )

    def test_system_user_lookup_uses_fixed_read_only_command(self):
        response = {"exitCode": 0, "stdout": "alice:x:1000:1000::/home/alice:/bin/bash\n", "stderr": ""}
        with patch.object(self.policy, "_run", return_value=response) as run:
            self.assertEqual(response, self.policy.dispatch({"operation": "LIST_SYSTEM_USERS", "arguments": [], "inputData": None}))
            run.assert_called_once_with(["getent", "passwd"])

    def test_system_user_lookup_rejects_extra_database_argument(self):
        with patch.object(self.policy, "_run") as run:
            with self.assertRaises(helper.PolicyError):
                self.policy.dispatch({"operation": "LIST_SYSTEM_USERS", "arguments": ["shadow"], "inputData": None})
            run.assert_not_called()

    def test_rejects_unlisted_service_and_operation(self):
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "SYSTEMD", "arguments": ["restart", "sshd"], "inputData": None}
            )
        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch({"operation": "RUN_SHELL", "arguments": [], "inputData": "id"})

    def test_backup_copy_is_limited_to_named_backups(self):
        valid = self.backups / "smb.conf.backup_20261018_120000"
        with patch.object(self.policy, "_run", return_value={"exitCode": 0, "stdout": "", "stderr": ""}) as run:
            self.policy.dispatch(
                {"operation": "COPY", "arguments": [str(self.config), str(valid)], "inputData": None}
            )
            self.assertEqual(["cp", "--", str(self.config), str(valid)], run.call_args.args[0])

        with self.assertRaises(helper.PolicyError):
            self.policy.dispatch(
                {"operation": "COPY", "arguments": [str(self.config), str(self.backups / "arbitrary")], "inputData": None}
            )


class FramingTest(unittest.TestCase):
    def test_reads_and_writes_length_prefixed_json(self):
        left, right = socket.socketpair()
        with left, right:
            request = {"operation": "DUMP_CONFIG", "arguments": [], "inputData": None}
            encoded = json.dumps(request).encode()
            left.sendall(struct.pack(">I", len(encoded)) + encoded)
            self.assertEqual(request, helper.read_frame(right))

            response = {"exitCode": 0, "stdout": "ok", "stderr": ""}
            helper.write_frame(right, response)
            size = struct.unpack(">I", left.recv(4))[0]
            self.assertEqual(response, json.loads(left.recv(size).decode()))


if __name__ == "__main__":
    unittest.main()
