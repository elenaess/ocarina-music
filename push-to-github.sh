#!/usr/bin/env bash
set -euo pipefail
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO="https://github.com/elenaess/ocarina-music.git"
for dep in git gh tar; do command -v "$dep" >/dev/null || { echo "Falta $dep; instale com pkg install -y git gh tar"; exit 1; }; done
if ! gh auth status >/dev/null 2>&1; then gh auth login; fi
gh auth setup-git
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
git clone --branch main --single-branch "$REPO" "$WORKDIR/ocarina-music"
(cd "$SOURCE_DIR" && tar --mtime='@0' --exclude='./.git' -cf - .) | (cd "$WORKDIR/ocarina-music" && tar -xf -)
cd "$WORKDIR/ocarina-music"
git add -A
if git diff --cached --quiet; then echo 'Sem mudanças novas.'; exit 0; fi
git -c user.name='elenaess' -c user.email='289515223+elenaess@users.noreply.github.com' commit -m 'feat: playlists, artistas, albuns, offline e repeat (v0.4.3)'
git push origin main
echo 'Pronto: https://github.com/elenaess/ocarina-music/actions'
