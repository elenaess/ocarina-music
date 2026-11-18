#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
REPO="https://github.com/elenaess/ocarina-music.git"

if ! command -v gh >/dev/null 2>&1; then
  echo 'Instale o GitHub CLI: pkg install gh'; exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo 'Entre na sua conta do GitHub para autorizar o envio:'
  gh auth login
fi
gh auth setup-git

if [ ! -d .git ]; then git init -b main; fi
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "$REPO"
fi
if [ "$(git remote get-url origin)" != "$REPO" ]; then
  echo 'Remote origin diferente: confira o repositório antes de enviar.'; exit 1
fi

git add -A
if ! git diff --cached --quiet; then
  git -c user.name='elenaess' \
      -c user.email='289515223+elenaess@users.noreply.github.com' \
      commit -m 'feat: Ocarina Music Android v0.1'
fi
git branch -M main
git push -u origin main
printf '\nCódigo enviado! Abra https://github.com/elenaess/ocarina-music/actions\n'
