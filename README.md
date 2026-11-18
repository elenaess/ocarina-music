# ocarina music — Android v0.1.1 (MVP)

> Uma nova casa para a música. App Android original em Kotlin + Jetpack Compose.

## O que já foi implementado

- Interface nativa com tema escuro, detalhes em verde e navegação Descobrir / Buscar / Biblioteca.
- Busca real (experimental) de faixas no catálogo **público não autenticado** do YouTube Music, via endpoint InnerTube `WEB_REMIX`.
- Capa, título e artista nos resultados; deduplicação e tratamento de falhas de conexão.
- Player do **YouTube incorporado e visível**, dentro do aplicativo (sem extrair áudio nem ocultar vídeo).
- Favoritos persistentes no aparelho usando `SharedPreferences`.
- Animações curtas, mensagens de carregamento e erro com botão de tentar novamente.
- Workflow GitHub Actions para compilar um APK de depuração.

## O que ainda NÃO existe

- Um APK já compilado ou validado em celular real.
- Reprodução de áudio independente/em segundo plano, download offline, letras ou equalizador.
- Login Google/Spotify, importação de playlists Spotify, recomendações personalizadas ou sincronização entre aparelhos.
- Garantia de cobertura de todo o catálogo ou disponibilidade permanente da API não oficial.

## Atualizar o repositório existente pelo Android (Termux)

1. Baixe `Ocarina-Music-corrigido-v0.1.1.zip` para Downloads.
2. `pkg install git gh tar unzip && termux-setup-storage`
3. `unzip -o ~/storage/downloads/Ocarina-Music-corrigido-v0.1.1.zip -d ~`
4. `bash ~/ocarina-music/push-to-github.sh`
5. Acesse Actions para ver a execução disparada pelo push.

O script clona a versão remota mais recente num diretório temporário, copia os arquivos desta versão sem apagar arquivos extras do repositório, faz commit e push sem `--force`.

## Como gerar um APK (caminho mais fácil: GitHub Actions)

1. Crie um repositório GitHub privado ou público chamado `ocarina-music`.
2. Adicione **o conteúdo desta pasta** à raiz do repositório, incluindo `.github/workflows/android.yml`.
3. Vá em **Actions > Ocarina Music APK > Run workflow** (ou faça um push na branch `main`).
4. Ao terminar, abra a execução e baixe o artefato `ocarina-music-v0.1.1-debug`.
5. Dentro do artefato estará o arquivo `app-debug.apk` para instalar em seu Android.

O workflow instala Java 17, SDK Android 35, Gradle 8.13, compila `:app:assembleDebug` e publica o artefato. GitHub Actions pode ter limites de uso conforme o plano/repositório.

### Usando Android Studio

Abra esta pasta como projeto Gradle, instale Android SDK 35, sincronize as dependências e selecione **Build > Build APK(s)**. Como o pacote não inclui `gradle-wrapper.jar`, o Android Studio pode pedir a configuração local de Gradle 8.13. Alternativamente, use o workflow.

## Organização

```text
app/src/main/java/com/ocarina/music/
├── MainActivity.kt
├── data/
│   ├── Song.kt
│   ├── FavoritesStore.kt
│   └── YouTubeMusicSearch.kt
└── ui/
    ├── OcarinaTheme.kt
    └── OcarinaApp.kt
.github/workflows/android.yml
```

## Limites e segurança

A busca acessa endpoints internos não documentados do YouTube Music (InnerTube). O Google pode alterá-los ou restringir acesso sem aviso; isso pode quebrar as buscas. O player utiliza um iframe de vídeo do YouTube — alguns vídeos podem não permitir incorporação. Streaming por métodos não oficiais também pode estar sujeito a condições de uso do serviço; verifique antes de distribuir o projeto.

O MVP não pede senhas, não coleta contas, não instala serviço de servidor e não baixa nem armazena músicas protegidas. Favoritos ficam apenas no aparelho. O app não tem anúncios nem telemetria própria, mas o player incorporado é prestado pelo YouTube e segue as regras de privacidade dele.

Não afiliado ao Spotify, YouTube ou Google. Marcas citadas pertencem a seus titulares.

## Próximas etapas sugeridas

1. Executar o workflow e instalar o APK num dispositivo real; corrigir qualquer falha da API ou player.
2. Adicionar player nativo e MediaSession quando houver uma fonte de áudio permitida e estável.
3. Importar dados de playlists, adicionar cache inteligente e refinar acessibilidade/animações.

**Status:** correção da falha de `setup-android@v3` aplicada após leitura do log do GitHub Actions. O APK ainda não foi compilado nem validado neste ambiente (sem Android SDK/Gradle).
