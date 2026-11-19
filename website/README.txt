Ocarina • elena.services • GitHub Pages

1. O aplicativo Android da versão 0.5.3 compartilha URLs como:
   https://elena.services/ocarina/track/<videoId>
   https://elena.services/ocarina/playlist/<uid>/<uuid-da-playlist>
   https://elena.services/ocarina/profile/<uid>

2. No Termux, na pasta do projeto, execute:
   bash setup-ocarina-pages-termux.sh
   Ele cria website/.well-known/assetlinks.json usando SHA-256 do certificado
   do APK. Isso NÃO é o SHA-1 do login Google nem o SHA-256 do ZIP.

3. No repositório que publica elena.services, copie o CONTEÚDO de website/
   para a raiz de publicação (não a pasta website inteira). Se já existir um
   site, integre os arquivos; NÃO substitua a homepage ou o 404 existente sem
   fazer backup. Os arquivos CNAME e .nojekyll são opcionais se já configurados.
   O arquivo assetlinks.json deve ficar EXATAMENTE em:
   https://elena.services/.well-known/assetlinks.json
   com HTTPS e resposta HTTP 200.

4. GitHub Pages é hospedagem estática. 404.html apresenta uma página amigável
   para URLs dinâmicas que não estejam abertas pelo app. O conteúdo de playlist
   é carregado pelo aplicativo via Firebase, não pelo JavaScript do Pages.

5. Compartilhar playlist publica uma cópia limitada da lista no Firestore,
   visível para pessoas autenticadas com o link. A lista local permanece privada.
   O link pode ficar com a versão compartilhada até você compartilhar novamente.
   Para remover uma publicação, exclua o documento em
   public_playlists/{uid}/items/{playlistId} no Firestore.
   Compartilhamento do perfil requer ativar "Aparecer na busca" na edição.

6. O Android só verifica App Links após o domínio publicar assetlinks.json.
   Caso contrário, o link pode abrir primeiro no navegador.
