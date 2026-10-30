"""Prepare an ignored Render environment file without displaying secret values."""
import base64
from pathlib import Path
import secrets

from migrate_aiven import ROOT, settings


def prepare():
    local = settings(ROOT / ".env")
    cloud = settings(ROOT / ".env.aiven")
    output = ROOT / ".env.render"
    previous = settings(output, dotenv=True) if output.exists() else {}
    values = {
        "SPRING_PROFILES_ACTIVE": "prod",
        "MYSQL_URL": "jdbc:mysql://" + cloud["AIVEN_HOST"] + ":" + cloud["AIVEN_PORT"]
                     + "/" + cloud["AIVEN_DATABASE"] + "?sslMode=VERIFY_IDENTITY&connectionTimeZone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8",
        "MYSQL_USERNAME": cloud["AIVEN_USERNAME"],
        "MYSQL_PASSWORD": cloud["AIVEN_PASSWORD"],
        "MYSQL_CA_CERT_PATH": "/etc/secrets/aiven-ca.pem",
        "MYSQL_CA_CERT_BASE64": base64.b64encode((ROOT / cloud["AIVEN_CA_CERT"]).read_bytes()).decode("ascii"),
        "JWT_SECRET": previous.get("JWT_SECRET") or base64.b64encode(secrets.token_bytes(32)).decode("ascii"),
        "JWT_ISSUER": "oneshop",
        "JWT_ACCESS_TOKEN_EXPIRATION_MILLIS": "900000",
        "JWT_REFRESH_TOKEN_EXPIRATION_MILLIS": "604800000",
        "MAIL_ENABLED": previous.get("MAIL_ENABLED", "false"),
        "MAIL_PROVIDER": "resend",
        "MAIL_FROM": previous.get("MAIL_FROM", "onboarding@resend.dev"),
        "RESEND_API_KEY": previous.get("RESEND_API_KEY", ""),
    }
    # Render provides RENDER_EXTERNAL_URL after creation; APP_BASE_URL is optional.
    if previous.get("APP_BASE_URL"):
        values["APP_BASE_URL"] = previous["APP_BASE_URL"]
    for key in ("CLOUDINARY_CLOUD_NAME", "CLOUDINARY_API_KEY", "CLOUDINARY_API_SECRET"):
        values[key] = previous.get(key) or local.get(key, "")
    if any("\n" in value or "\r" in value for value in values.values()):
        raise ValueError("Environment values must be single-line")
    # Quoted dotenv values preserve #, whitespace and passwords with punctuation.
    def quoted(value):
        if "'" not in value:
            return "'" + value + "'"
        if '"' not in value:
            return '"' + value + '"'
        raise ValueError("A value contains both quote types; enter that value directly in Render instead")
    content = "# Private Render configuration. Never commit or share this file.\n"
    content += "\n".join(key + "=" + quoted(value) for key, value in values.items()) + "\n"
    output.write_text(content, encoding="utf-8")
    print("Prepared .env.render (ignored by Git). Secret values were not printed.")
    print("Email enabled:", values["MAIL_ENABLED"] == "true")
    print("Cloudinary configured:", all(values[key] for key in ("CLOUDINARY_CLOUD_NAME", "CLOUDINARY_API_KEY", "CLOUDINARY_API_SECRET")))


if __name__ == "__main__":
    prepare()
