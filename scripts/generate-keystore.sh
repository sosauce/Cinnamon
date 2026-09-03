#!/usr/bin/env bash
set -euo pipefail

# Linux/macOS — generate keystore + base64 for GitHub Secrets
# Usage: bash scripts/generate-keystore.sh

ALIAS="cinnamon"
KEYSTORE="cinnamon-release.jks"
DNAME="CN=Cinnamon, OU=Mobile, O=sosauce, L=Cairo, S=Cairo, C=EG"

read -rsp "Enter KEYSTORE_PASSWORD (storepass): " STOREPASS; echo
read -rsp "Enter KEY_PASSWORD (keypass, often same): " KEYPASS; echo

if [[ -z "$STOREPASS" || -z "$KEYPASS" ]]; then
  echo "Passwords cannot be empty" >&2; exit 1
fi

echo "Generating $KEYSTORE with alias '$ALIAS'..."
keytool -genkey -v -keystore "$KEYSTORE" -keyalg RSA -keysize 2048 -validity 10000 \
  -alias "$ALIAS" -storepass "$STOREPASS" -keypass "$KEYPASS" -dname "$DNAME"

echo "Encoding to base64..."
base64 -w 0 "$KEYSTORE" > keystore.b64 2>/dev/null || base64 "$KEYSTORE" | tr -d '\n' > keystore.b64
echo "Wrote keystore.b64 (length: $(wc -c < keystore.b64) chars)"

echo "Verifying..."
keytool -list -v -keystore "$KEYSTORE" -alias "$ALIAS" -storepass "$STOREPASS" | head -n 20

cat <<EOF

=== GitHub Secrets to create ===
KEYSTORE_FILE_B64  -> paste entire content of keystore.b64 (single line)
KEYSTORE_PASSWORD  -> $STOREPASS
KEY_ALIAS          -> $ALIAS
KEY_PASSWORD       -> $KEYPASS

Add at: https://github.com/MoHamed-B-M/Cinnamon/settings/secrets/actions
DO NOT commit $KEYSTORE or keystore.b64 to git!
EOF
