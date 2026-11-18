# Ocarina Music — v0.2.0 nativo 🎧

Player Android com identidade própria em Kotlin / Jetpack Compose. **Não é um WebView do YouTube:** reproduz áudio usando `Media3 ExoPlayer`, com controles da notificação e reprodução em segundo plano (`MediaSessionService`).

## Recursos
- Pesquisa experimental de músicas pelo catálogo não oficial do YouTube Music.
- Busca de URL de áudio por `NewPipeExtractor` (v0.27.1). Links temporários, resolvidos no momento da reprodução.
- Player nativo com capa, progresso, play/pause, busca no tempo e faixa seguinte/anterior enquanto o app está aberto.
- Miniplayer fixo e favoritos locais.
- Serviço de áudio em segundo plano e controles na área de notificações.
- Mensagens de erro quando a fonte não libera áudio.

## Importante
Essa integração não é oficial e pode falhar para certos vídeos, restrições, regiões ou quando o YouTube muda os endpoints. Ela também pode estar sujeita a restrições nos termos do YouTube. Não representa acesso garantido ao catálogo, não implementa login YouTube/Spotify, nem ignora DRM ou restrições de idade. Para distribuição, observe a GPL-3.0 do `NewPipeExtractor` (veja `THIRD_PARTY.md`).

## Compilar no GitHub Actions
Faça push deste ZIP (descompactado) na sua branch main. Em Actions -> Ocarina Music APK, baixe `ocarina-music-v0.2.0-debug` após build verde.

## Publicar via Termux
```sh
pkg install -y git gh tar unzip
termux-setup-storage
unzip -o ~/storage/downloads/Ocarina-Music-v0.2.0-nativo.zip -d ~
bash ~/ocarina-music/push-to-github.sh
```

A compilação do APK requer conexão para baixar dependências, inclusive JitPack. Ainda não foi testada neste ambiente de trabalho por indisponibilidade do Android SDK e do acesso externo à build.
