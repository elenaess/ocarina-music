# Importar o ZIP no Termux, sem adivinhar o nome

O ChatGPT para Android pode guardar o arquivo numa área interna do próprio aplicativo, invisível ao Termux. Nessa situação, `find` não resolve. Abra esta conversa no navegador do Android, toque no link do ZIP e escolha **Baixar**; depois confirme em **Arquivos → Armazenamento interno → Download**. Se for salvo em outra pasta, mova para **Download** no gerenciador de arquivos.

Depois de confirmar no app Arquivos, execute:

```bash
pkg install -y git gh unzip openjdk-17 openssl coreutils
termux-setup-storage
ZIP=$(find /sdcard/Download /sdcard/Documents -type f -iname '*ocarina*v0.4.3*.zip' -print -quit 2>/dev/null)
if [ -z "$ZIP" ]; then echo 'O arquivo ainda não está em /sdcard/Download. Mova pelo app Arquivos.'; exit 1; fi
unzip -oq "$ZIP" -d "$HOME"
grep versionName "$HOME/ocarina-music/app/build.gradle.kts"
gh auth status || gh auth login
gh auth setup-git
bash "$HOME/ocarina-music/push-to-github.sh"
```

**Não execute o script de assinatura novamente se o login Google já está funcionando.** A compilação reutiliza a chave estável dos segredos atuais do GitHub.
