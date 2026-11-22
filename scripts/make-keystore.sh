#!/usr/bin/env bash
# Creates Lumora's release keystore on YOUR device and stores the signing secrets in GitHub.
# Run it in Termux:   pkg install -y openjdk-17 gh && gh auth login
# The keystore and password never leave your device except as encrypted GitHub repository secrets.
# BACK UP the keystore file: if you lose it you can never publish an update that installs over this one.
set -euo pipefail

REPO="${1:-Aasimabdul/lumora}"
KS="${LUMORA_KEYSTORE_OUT:-$HOME/lumora-release.jks}"
ALIAS="lumora"

command -v keytool >/dev/null || { echo "keytool not found. In Termux: pkg install openjdk-17"; exit 1; }
command -v gh >/dev/null || { echo "gh not found. In Termux: pkg install gh"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Run: gh auth login"; exit 1; }
[ ! -e "$KS" ] || { echo "$KS already exists; refusing to overwrite it."; exit 1; }

read -r -s -p "Choose a keystore password (at least 8 characters): " PW; echo
read -r -s -p "Repeat the password: " PW2; echo
[ "$PW" = "$PW2" ] || { echo "Passwords do not match."; exit 1; }
[ "${#PW}" -ge 8 ] || { echo "Password is too short."; exit 1; }
read -r -p "Your name (shown in the signing certificate): " NAME

keytool -genkeypair -keystore "$KS" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PW" -keypass "$PW" -dname "CN=${NAME:-Lumora}, O=Lumora"

base64 -w0 "$KS" | gh secret set LUMORA_KEYSTORE_B64 -R "$REPO"
printf %s "$PW" | gh secret set LUMORA_KEYSTORE_PASSWORD -R "$REPO"
printf %s "$ALIAS" | gh secret set LUMORA_KEY_ALIAS -R "$REPO"
printf %s "$PW" | gh secret set LUMORA_KEY_PASSWORD -R "$REPO"

echo
echo "Done. Secrets set on $REPO. Keystore saved at: $KS"
echo "Copy that file somewhere safe now (cloud drive, PC). Never commit it."
