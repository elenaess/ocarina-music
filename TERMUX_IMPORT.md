# Ocarina v0.5.0 no Termux

1. Faça o download do ZIP no Android. Se o arquivo aparece no aplicativo Arquivos, mas
não em `/sdcard/Download`, use o seletor de documentos do Termux em vez de `find`.

```bash
pkg install -y termux-api unzip git gh
termux-storage-get "$HOME/ocarina-v050.zip"
```

Selecione **Ocarina-v0.5.0-Redesign.zip** no seletor. O recurso pode exigir o app
complementar Termux:API, além do pacote `termux-api` do Termux.

2. Extraia e envie:

```bash
mkdir -p "$HOME/ocarina-v050"
unzip -tq "$HOME/ocarina-v050.zip" && unzip -oq "$HOME/ocarina-v050.zip" -d "$HOME/ocarina-v050"
bash "$HOME/ocarina-v050/ocarina-music/push-to-github.sh"
```

3. Abra https://github.com/elenaess/ocarina-music/actions e verifique a compilação;
APKs só existem se o workflow concluir com sucesso.

4. No Firebase Console / Firestore / Rules, substitua as regras atuais pelo conteúdo de
`firestore.rules` e publique para permitir apenas o diretório opt-in.

**Não atualize o SHA-1** se o APK continuar assinado com a mesma chave estável do GitHub.
