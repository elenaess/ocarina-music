#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
STORE="$HOME/.ocarina-signing/ocarina-preview.jks"
PASSFILE="$HOME/.ocarina-signing/keystore-password"
if [ ! -f "$STORE" ] || [ ! -f "$PASSFILE" ]; then
  echo 'Não encontrei a chave estável usada no APK. Confira ~/.ocarina-signing.'
  exit 1
fi
command -v keytool >/dev/null || { echo 'Instale openjdk-17 no Termux'; exit 1; }
SHA=$(keytool -list -v -keystore "$STORE" -storepass "$(cat "$PASSFILE")" -alias ocarina \
 | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -n 1 | tr -d '\r')
if ! printf '%s' "$SHA" | grep -Eq '^([A-Fa-f0-9]{2}:){31}[A-Fa-f0-9]{2}$'; then
 echo 'SHA-256 não encontrado. Confira a chave do APK.'
 exit 1
fi
mkdir -p "$REPO_ROOT/website/.well-known"
printf '{"relation":["delegate_permission/common.handle_all_urls"],"target":{"namespace":"android_app","package_name":"com.ocarina.app","sha256_cert_fingerprints":["%s"]}}\n' "$SHA" > "$REPO_ROOT/website/.well-known/assetlinks.json"
echo 'Arquivo de associação criado: website/.well-known/assetlinks.json'
echo 'Publique a pasta website/ na RAIZ do GitHub Pages do domínio elena.services.'
echo 'Teste depois: https://elena.services/.well-known/assetlinks.json'
