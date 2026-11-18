#!/usr/bin/env bash
# Publica esta versão corrigida no repositório existente, sem forçar histórico.
set -euo pipefail
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO="https://github.com/elenaess/ocarina-music.git"

for dep in git gh tar; do
  if ! command -v "$dep" >/dev/null 2>&1; then
    echo "Falta instalar: $dep (Termux: pkg install git gh tar)"; exit 1
  fi
done
if ! gh auth status >/dev/null 2>&1; then
  echo 'Entre na sua conta do GitHub:'
  gh auth login
fi
gh auth setup-git

# Sempre parte do último commit remoto. Evita erro non-fast-forward de ZIP antigo.
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

git clone --branch main --single-branch "$REPO" "$WORKDIR/ocarina-music"
# Sobrescreve arquivos correspondentes, preserva outros existentes no remoto.
# Evita transportar .git acidental de um diretório já usado anteriormente.
(cd "$SOURCE_DIR" && tar --exclude='./.git' -cf - .) | (cd "$WORKDIR/ocarina-music" && tar -xf -)
cd "$WORKDIR/ocarina-music"
git add -A
if git diff --cached --quiet; then
  echo 'GitHub já contém esses arquivos. Nenhuma alteração pendente.'
  exit 0
fi
git -c user.name='elenaess' \
    -c user.email='289515223+elenaess@users.noreply.github.com' \
    commit -m 'fix: alinhar Compose BOM com Android SDK 35 (v0.1.2)'
git push origin main
printf '\nCódigo enviado. Abra https://github.com/elenaess/ocarina-music/actions\n'
