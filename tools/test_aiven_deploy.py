import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import migrate_aiven as migration
import prepare_render


class AivenDeploymentTest(unittest.TestCase):
    def test_dotenv_quotes_preserve_password_punctuation_and_jwt_padding(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "config.env"
            path.write_text("PASSWORD='test # spaced=password'\nJWT_SECRET='base64=='\n", encoding="utf-8")
            values = migration.settings(path, dotenv=True)
            self.assertEqual(values["PASSWORD"], "test # spaced=password")
            self.assertEqual(values["JWT_SECRET"], "base64==")

    def test_credentials_file_is_removed_even_when_client_fails(self):
        with tempfile.TemporaryDirectory() as folder:
            config = dict(host="127.0.0.1", port=3306, user="test", password='quote"slash\\hash#')
            with self.assertRaises(RuntimeError):
                with migration.client_options(config, False, folder) as option:
                    path = Path(option.split("=", 1)[1])
                    self.assertTrue(path.is_file())
                    self.assertNotIn(config["password"], option)
                    raise RuntimeError("simulated client failure")
            self.assertFalse(path.exists())

    def test_migration_refuses_populated_target_before_dump_or_import(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "ca.pem").write_text("test fixture", encoding="utf-8")
            local = dict(MYSQL_URL="jdbc:mysql://127.0.0.1:3306/oneshop_mysql", MYSQL_USERNAME="test", MYSQL_PASSWORD="test-only")
            cloud = dict(AIVEN_HOST="test.aivencloud.com", AIVEN_PORT="12345", AIVEN_DATABASE="defaultdb",
                         AIVEN_USERNAME="test", AIVEN_PASSWORD="test-only", AIVEN_CA_CERT="ca.pem")
            @contextlib.contextmanager
            def options(*args):
                yield "--test-options"
            with patch.object(migration, "ROOT", root), patch.object(migration, "settings", side_effect=[local, cloud]), \
                    patch.object(migration, "private_directory"), patch.object(migration, "client_options", options), \
                    patch.object(migration, "tables", side_effect=[set(migration.TABLES), {"existing_customer_data"}]), \
                    patch.object(migration, "counts", return_value={table: 1 for table in migration.TABLES}), \
                    patch.object(migration.os, "chdir"), patch.object(migration.subprocess, "run") as commands, \
                    patch("sys.argv", ["migrate_aiven.py", "--migrate"]), contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaisesRegex(ValueError, "not empty"):
                    migration.main()
                commands.assert_not_called()

    def test_regenerating_render_config_preserves_signing_key_and_email_settings(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "ca.pem").write_text("test public certificate", encoding="utf-8")
            (root / ".env").write_text("CLOUDINARY_API_SECRET=private-test-value\n", encoding="utf-8")
            (root / ".env.aiven").write_text("AIVEN_HOST=test.aivencloud.com\nAIVEN_PORT=12345\n"
                "AIVEN_DATABASE=defaultdb\nAIVEN_USERNAME=test\nAIVEN_PASSWORD=test-only\nAIVEN_CA_CERT=ca.pem\n", encoding="utf-8")
            with patch.object(prepare_render, "ROOT", root), contextlib.redirect_stdout(io.StringIO()):
                prepare_render.prepare()
                first = migration.settings(root / ".env.render", dotenv=True)
                with (root / ".env.render").open("a", encoding="utf-8") as output:
                    output.write("RESEND_API_KEY='test-key'\nMAIL_ENABLED='true'\n")
                prepare_render.prepare()
                second = migration.settings(root / ".env.render", dotenv=True)
            self.assertEqual(first["JWT_SECRET"], second["JWT_SECRET"])
            self.assertEqual(second["RESEND_API_KEY"], "test-key")
            self.assertEqual(second["MAIL_ENABLED"], "true")
            self.assertEqual(second["CLOUDINARY_API_SECRET"], "private-test-value")


if __name__ == "__main__":
    unittest.main()
