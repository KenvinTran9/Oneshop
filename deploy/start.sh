#!/bin/sh
set -eu
certificate="${MYSQL_CA_CERT_PATH:-/etc/secrets/aiven-ca.pem}"
if [ ! -s "$certificate" ] && [ -n "${MYSQL_CA_CERT_BASE64:-}" ]; then
    certificate=/tmp/aiven-ca.pem
    umask 077
    printf '%s' "$MYSQL_CA_CERT_BASE64" | base64 --decode > "$certificate"
fi
if [ ! -s "$certificate" ]; then
    echo "Missing Aiven CA certificate. Import .env.render or add Secret File aiven-ca.pem in Render." >&2
    exit 1
fi
# This truststore is for MySQL only. HTTPS mail/Cloudinary retain the default JVM CA store.
rm -f /tmp/aiven-truststore.p12
keytool -importcert -noprompt -alias aiven-mysql -file "$certificate" \
    -keystore /tmp/aiven-truststore.p12 -storetype PKCS12 -storepass changeit >/dev/null 2>&1
chmod 0600 /tmp/aiven-truststore.p12
exec java -jar /app/oneshop.jar
