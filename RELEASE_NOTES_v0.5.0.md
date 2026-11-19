# Ocarina v0.5.0 — redesign

## Entregas desta atualização

- Login como porta de entrada: interface de autenticação separada do aplicativo. Google Sign-In e e-mail/senha existentes preservados.
- Novo cabeçalho persistente com símbolo Ocarina, avatar da conta, atalhos de busca e criação na Biblioteca.
- Navegação principal Home, Buscar, Biblioteca e Perfil; o quarto ícone agora é o avatar da conta.
- Home com filtros, playlists primeiro quando existentes, recomendações e histórico.
- Busca com campo limpo, descobertas e categorias Música, Vídeos, Podcast, Ao vivo e Perfis; tipo música exibido antes do vídeo quando identificado pela origem.
- Biblioteca mais enxuta com playlists, curtidas, artistas, álbuns, downloads e histórico recente.
- Perfil de visualização separado da edição, com lápis, foto, nome, bio, tema e opção de perfil público.
- Diretório público em Firestore somente após consentimento explícito. Busca por prefixo do nome; e-mail e curtidas continuam privados.
- Histórico de reprodução local isolado por UID.
- Player com arte circular no estilo CD; sem animação contínua pesada. Media3, fila, repeat e controles preservados.
- Ícone grafite com símbolo central rosa em degradê. Aplicação mantém `com.ocarina.app` e assinatura estável do GitHub.

## Configuração obrigatória

**Publique `firestore.rules`** no Firebase Console: Firestore Database → Regras. Sem isso, a busca de perfis e a bio podem dar `PERMISSION_DENIED`. Os demais segredos e contas existentes não precisam ser refeitos.

## Limites

- `WEB_REMIX` e NewPipe são fontes não oficiais: etiquetas de tipo nem sempre vêm no catálogo e não há garantia universal de reprodução.
- Categorias Podcasts/Ao vivo só mostram faixas identificadas como tais pela origem; não há APIs independentes de podcasts ou livestreams.
- O perfil público não expõe playlists públicas; o compartilhamento ainda é textual, não um link de perfil universal.
- Downloads continuam somente importação de arquivos próprios do dispositivo.
- Os favoritos Firestore continuam; playlists locais não foram migradas para sync entre aparelhos.
- A primeira versão com histórico por conta inicia lista recente separada por login; o histórico antigo global não é migrado automaticamente para evitar misturar contas.
- Validação de integração Android e runtime será feita ao compilar no GitHub Actions e testar no aparelho; não confundir com validação estática.
