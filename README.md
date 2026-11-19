# Ocarina

Player musical Android em Kotlin + Jetpack Compose. Versão **0.3.0**, atualização de identidade visual e navegação.

## Telas

- **Início** com saudação, faixas recentemente ouvidas e conteúdo real obtido por busca experimental do catálogo musical.
- **Busca** com lista de faixas e reprodução pela infraestrutura existente.
- **Biblioteca** com entradas para curtidas, playlists, álbuns, artistas e downloads. Os três últimos recursos são apenas telas indicativas nesta etapa e **não fingem conteúdo inexistente**.
- **Curtidas** persistidas localmente.
- **Perfil** visual, com estatísticas locais reais e um seletor de **modo claro/escuro** salvo no dispositivo.
- **Player nativo** (Media3) e miniplayer mantidos, com controles vetoriais Lucide.

**Atenção:** o login Firebase e a integração com Psiu ainda **não foram implementados**. A tela de perfil marca essa condição. Ocarina não se conecta a essas contas nesta versão.

## Design

- Tema claro creme rosado com bordô e tema escuro ameixa com rosa suave.
- Identidade Ocarina: `branding/ocarina-mark.svg`, vetorizada a partir da referência fornecida.
- `app/src/main/res/drawable/ocarina_logo.xml` é a versão Android VectorDrawable do mesmo símbolo.
- Todos os ícones operacionais são **Lucide** via `com.composables:icons-lucide-android:2.2.1` (MIT). Não há caracteres Unicode usados como ícones.

## Compilar via GitHub Actions

1. Extraia o ZIP em `~/ocarina-music` no Termux.
2. Execute `bash ~/ocarina-music/push-to-github.sh` após autenticar com `gh`.
3. Abra https://github.com/elenaess/ocarina-music/actions e procure a execução **Ocarina Music APK**.
4. Se a compilação passar, baixe o artifact `ocarina-v0.3.0-debug` e instale `app-debug.apk`.

O projeto mantém a integração experimental com YouTube Music e NewPipe. Essas APIs são não oficiais e podem deixar de funcionar. O **APK v0.3.0 ainda não foi compilado/testado em dispositivo** neste ambiente; o GitHub Actions fará a validação na primeira execução.

## Licenças

Consulte `THIRD_PARTY.md`, `COPYING-GPL-3.0` e `LICENSE`. A combinação com NewPipe exige respeitar a GPL do componente combinado.
