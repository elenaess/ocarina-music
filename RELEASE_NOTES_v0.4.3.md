# Ocarina v0.4.3 — Biblioteca e coleções

Projeto Android Kotlin/Jetpack Compose. Mantém as cores, tipografia, navbar e o login Google/Firebase da v0.4.2.

## Implementado

- Playlists locais por conta: criar, renomear, remover, adicionar músicas via menu de três pontos e trocar a capa com uma foto da galeria. A capa é copiada para o armazenamento privado do app.
- Carrossel das playlists na página inicial, sem alterar a identidade visual do Ocarina.
- Página de artistas e álbuns: pesquisa no catálogo experimental do YouTube Music, capas e visualização de faixas. A disponibilidade dos detalhes depende das respostas da fonte não oficial.
- Fila de escuta: adicionar músicas pelo menu, visualizar, remover e reproduzir da fila. Ao terminar uma faixa, o app avança para a próxima quando disponível.
- Repeat: desligado / repetir uma faixa / repetir a lista. O modo selecionado fica salvo localmente.
- Compartilhamento via menu nativo do Android: link da música (quando YouTube Music), lista textual de músicas da playlist, descrição do artista/álbum e nome do perfil. **Não há links públicos de perfil ou playlists hospedados no Firebase.**
- Downloads: importação de arquivos de áudio escolhidos pelo usuário, copiados para o armazenamento privado do app e reproduzidos sem conexão. **Não baixa conteúdo do YouTube**.
- Perfil: avatar maior, cartão de estatísticas, compartilhamento e acesso às playlists, usando a paleta anterior.

## Armazenamento e privacidade

- Login Google, avatar e favoritos online continuam usando Firebase, sem alterar as Firestore Rules da v0.4.2.
- Playlists, capas e arquivos de áudio offline são locais ao dispositivo. Playlists são separadas por conta dentro da instalação, mas **ainda não sincronizadas entre aparelhos**.
- Os arquivos offline não são enviados ao Firebase.

## Verificação / limitações

- Passou na checagem estrutural `python tools/verify_release.py`, incluindo parsing de AndroidManifest e revisão de referências no código.
- Sintaxe Kotlin analisada com `kotlinc` 1.9; esta verificação não compila bibliotecas Android/Compose.
- **A compilação do APK e os testes em aparelho ainda são obrigatórios**, pois este ambiente não dispõe do Android SDK/Gradle e o conector do GitHub não permite escrever neste repositório.

## Compilar pelo GitHub Actions

Depois de enviar o conteúdo do ZIP ao repositório em `main`, o fluxo `.github/workflows/android.yml` gera `ocarina-v0.4.3-library-debug` se a compilação passar. Seu certificado de assinatura estável configurado nos segredos do GitHub foi preservado.
