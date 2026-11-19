#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
REPO="elenaess/ocarina-music"
command -v keytool >/dev/null || { echo "Instale: pkg install openjdk-17"; exit 1; }
command -v gh >/dev/null || { echo "Instale: pkg install gh"; exit 1; }
gh auth status >/dev/null || { echo "Faça login: gh auth login"; exit 1; }
mkdir -p "$HOME/.ocarina-signing"
chmod 700 "$HOME/.ocarina-signing"
STORE="$HOME/.ocarina-signing/ocarina-preview.jks"
PASSFILE="$HOME/.ocarina-signing/keystore-password"
if [ ! -f "$STORE" ]; then
  PASS="$(openssl rand -hex 20)"
  printf '%s' "$PASS" > "$PASSFILE"
  chmod 600 "$PASSFILE"
  keytool -genkeypair -noprompt -v -keystore "$STORE" -storetype JKS \
    -storepass "$PASS" -keypass "$PASS" -alias ocarina \
    -keyalg RSA -keysize 2048 -validity 3650 \
    -dname 'CN=Ocarina Preview,O=Ocarina,C=BR'
fi
[ -f "$PASSFILE" ] || { echo "Falta senha da chave original; não gere outra assinatura sem planejamento."; exit 1; }
PASS="$(cat "$PASSFILE")"
base64 -w 0 "$STORE" | gh secret set OCARINA_SIGNING_KEYSTORE_B64 -R "$REPO"
printf '%s' "$PASS" | gh secret set OCARINA_SIGNING_STORE_PASSWORD -R "$REPO"
printf '%s' "$PASS" | gh secret set OCARINA_SIGNING_KEY_PASSWORD -R "$REPO"
echo 'Copie a impressão SHA1 abaixo para Firebase > Configurações do projeto > Android > SHA:'
keytool -list -v -keystore "$STORE" -storepass "$PASS" -alias ocarina | grep -E 'SHA1:|SHA256:'
echo 'Segredos de assinatura configurados no GitHub. Faça backup PRIVADO da pasta ~/.ocarina-signing.'
