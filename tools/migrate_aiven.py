"""Copy OneShop's local MySQL tables to an empty Aiven database using verified TLS.

No credentials in arguments/output. Private dumps stay in ignored backups/.
Default is read-only; --migrate explicitly enables import, never DROP/overwrite.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime
import getpass
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
TABLES = ("product_watches", "roles", "stores", "categories", "brands", "users", "products",
          "product_sizes", "product_shades", "store_stocks", "shopping_carts",
          "shopping_cart_items", "orders", "order_items", "order_events", "password_reset_accounts",
          "stock_movements", "customer_addresses")
SESSIONS = {"SPRING_SESSION", "SPRING_SESSION_ATTRIBUTES"}


def settings(path, dotenv=False):
    values = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key.strip()):
            raise ValueError("Invalid KEY=value line in " + path.name)
        value = value.strip()
        if dotenv and len(value) >= 2 and value[0] in "\"'" and value[-1] == value[0]:
            value = value[1:-1]
        values[key.strip()] = value
    return values


def mysql_bin(name):
    located = shutil.which(name)
    fallback = Path(r"C:\Program Files\MySQL\MySQL Server 8.4\bin") / (name + ".exe")
    if located:
        return located
    if fallback.is_file():
        return str(fallback)
    raise ValueError("Install MySQL 8.4 client tools: missing " + name)


def private_directory(path):
    path.mkdir(parents=True, exist_ok=True)
    if os.name == "nt":
        account = os.environ.get("USERDOMAIN", "") + "\\" + getpass.getuser()
        result = subprocess.run(["icacls", str(path), "/inheritance:r", "/grant:r", account + ":(OI)(CI)F"],
                                capture_output=True)
        if result.returncode:
            raise ValueError("Cannot protect private backup directory")
    else:
        path.chmod(0o700)


@contextmanager
def client_options(config, remote, folder):
    handle, filename = tempfile.mkstemp(prefix="mysql-client-", suffix=".cnf", dir=folder)
    os.close(handle)
    path = Path(filename)
    try:
        def quoted(value):
            return '"' + str(value).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r") + '"'
        fields = {"host": config["host"], "port": config["port"], "user": config["user"],
                  "password": config["password"], "default-character-set": "utf8mb4",
                  "ssl-mode": "VERIFY_IDENTITY" if remote else "DISABLED"}
        if remote:
            fields["ssl-ca"] = str(config["ca"])
        mysql_only = "[mysql]\nconnect-timeout=15\n" + ("get-server-public-key=TRUE\n" if not remote else "")
        path.write_text("[client]\n" + "\n".join(key + "=" + quoted(value) for key, value in fields.items()) + "\n" + mysql_only, encoding="utf-8")
        yield "--defaults-extra-file=" + str(path)
    finally:
        path.unlink(missing_ok=True)


def run_mysql(option, database, sql):
    result = subprocess.run([mysql_bin("mysql"), option, "--batch", "--skip-column-names", database],
                            input=sql.encode("utf-8"), capture_output=True, timeout=90)
    if result.returncode:
        code = re.search(rb"ERROR (\d+)", result.stderr)
        raise ValueError("MySQL connection/query failed" + (" (code " + code[1].decode() + ")" if code else "")
                         + "; check local credentials, CA and network. Credentials were not logged.")
    return result.stdout.decode("utf-8").strip()


def tables(option, database):
    return set(run_mysql(option, database, "SHOW TABLES;").splitlines())


def counts(option, database):
    sql = " UNION ALL ".join("SELECT '" + table + "', COUNT(*) FROM `" + table + "`" for table in TABLES) + ";"
    return {table: int(count) for table, count in (row.split("\t") for row in run_mysql(option, database, sql).splitlines())}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--migrate", action="store_true", help="Import into an EMPTY Aiven database after verified checks")
    args = parser.parse_args()
    os.chdir(ROOT)
    local, cloud = settings(ROOT / ".env"), settings(ROOT / ".env.aiven")
    source_url = urlsplit(local["MYSQL_URL"].removeprefix("jdbc:"))
    if source_url.scheme != "mysql" or source_url.hostname not in {"localhost", "127.0.0.1"}:
        raise ValueError("Source must be the local MySQL server")
    source = {"host": source_url.hostname, "port": source_url.port or 3306,
              "database": source_url.path.lstrip("/"), "user": local["MYSQL_USERNAME"], "password": local["MYSQL_PASSWORD"]}
    target = {"host": cloud["AIVEN_HOST"], "port": int(cloud["AIVEN_PORT"]), "database": cloud["AIVEN_DATABASE"],
              "user": cloud["AIVEN_USERNAME"], "password": cloud["AIVEN_PASSWORD"], "ca": ROOT / cloud["AIVEN_CA_CERT"]}
    if not re.fullmatch(r"[a-zA-Z0-9.-]+\.aivencloud\.com", target["host"]) or not 1 <= target["port"] <= 65535:
        raise ValueError("Target must be an Aiven MySQL hostname and valid port")
    for config in (source, target):
        if not re.fullmatch(r"[a-zA-Z0-9_]+", config["database"]):
            raise ValueError("Invalid database name")
        if not config["password"] or config["password"].startswith("FILL_"):
            raise ValueError("Fill database passwords locally before running")
    if not target["ca"].is_file():
        raise ValueError("Download the Aiven CA certificate to the configured local path")
    folder = ROOT / "backups" / "aiven-migration"
    private_directory(folder)
    with client_options(source, False, folder) as src, client_options(target, True, folder) as dst:
        source_tables = tables(src, source["database"])
        if source_tables - SESSIONS != set(TABLES):
            raise ValueError("Local application tables differ from the reviewed migration list; refusing partial migration")
        target_tables = tables(dst, target["database"])
        print("Aiven verified TLS connection: OK")
        print("Aiven existing tables:", len(target_tables))
        baseline = counts(src, source["database"])
        for table, count in baseline.items():
            print(table + ": " + str(count))
        if target_tables:
            if set(TABLES).issubset(target_tables):
                print("Aiven row counts match local:", counts(dst, target["database"]) == baseline)
            if args.migrate:
                raise ValueError("Aiven is not empty; no import/overwrite performed")
            return
        if not args.migrate:
            print("Read-only check complete; --migrate is required to copy data.")
            return
        # A single consistent InnoDB snapshot, no DROP/CREATE DATABASE, no routines/triggers/GTID privilege changes.
        dump = folder / ("oneshop-" + datetime.now().strftime("%Y%m%d-%H%M%S-%f") + ".sql")
        command = [mysql_bin("mysqldump"), src, "--single-transaction", "--quick", "--no-tablespaces",
                   "--set-gtid-purged=OFF", "--column-statistics=0", "--skip-add-drop-table", "--skip-add-locks",
                   "--skip-triggers", "--skip-comments", "--hex-blob", source["database"], *TABLES]
        with dump.open("xb") as output:
            result = subprocess.run(command, stdout=output, stderr=subprocess.PIPE, timeout=180)
        if result.returncode:
            unknown = re.search(rb"unknown (?:variable|option) ['-]*([a-zA-Z0-9_-]+)", result.stderr)
            code = re.search(rb"(?:ERROR |Got error: )(\d+)", result.stderr)
            detail = " Unsupported client option: " + unknown[1].decode() if unknown else ""
            if code:
                detail += " MySQL error code: " + code[1].decode()
            raise ValueError("Local export failed; Aiven was not modified." + detail)
        if counts(src, source["database"]) != baseline:
            raise ValueError("Local data changed during export; retry while no local checkout/admin writes are running")
        if tables(dst, target["database"]):
            raise ValueError("Aiven is no longer empty; refusing import")
        with dump.open("rb") as input_file:
            result = subprocess.run([mysql_bin("mysql"), dst, "--binary-mode", target["database"]],
                                    stdin=input_file, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=240)
        if result.returncode:
            raise ValueError("Import failed and may be partial. No automatic cleanup/retry. Keep private backup and inspect Aiven.")
        if counts(dst, target["database"]) != baseline:
            raise ValueError("Imported row counts differ; inspect before deploying. No automatic cleanup performed.")
        print("Migration complete: all " + str(len(TABLES)) + " application tables and row counts verified. Local data unchanged.")
        print("Private backup:", dump.relative_to(ROOT))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.TimeoutExpired) as failure:
        # Never include config values or captured database errors in output.
        print("Stopped:", str(failure) if isinstance(failure, ValueError) else type(failure).__name__)
        raise SystemExit(1)
