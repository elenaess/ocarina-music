# Ocarina — v0.3.2 (experimental)

Player Android em Kotlin + Jetpack Compose, com interface clara/escura, marca vetorial e ícones Lucide.

## O que mudou

- Abas exibidas imediatamente, com transições de cor de 90 ms e indicador leve de três pontos.
- Player integrado à mesma janela Compose (sem Dialog) e erro de reprodução visível com botão para tentar novamente.
- Seleção de capa maior quando o servidor oferecer, com cache e dimensões de decodificação limitadas.
- Metadados de artista melhorados; taxa de áudio corrigida de bits/s para kbps quando aplicável.
- Abandona animação que renderizava duas páginas de listas simultaneamente.
- Mantém favoritos e o motor Media3 de reprodução em segundo plano.

## Limitações

Catálogo e resolução do áudio utilizam interfaces não oficiais e podem falhar em vídeos não disponíveis, restritos ou após mudanças do serviço. O app não promete áudio de 320 kbps nem funcionamento em todos os vídeos. Algumas capas só existem em baixa resolução na fonte.

JUST Sans não está incluída porque seu licenciamento para incorporação em aplicativos é separado; o Ocarina usa a tipografia padrão Android ajustada. Se você tiver licença de app, a fonte pode ser integrada depois.

## Compilar

O workflow `.github/workflows/android.yml` dispara em cada push da `main`. Baixe `app-debug.apk` em **Actions → Artifacts** depois de uma execução bem-sucedida.

## Enviar pelo Termux

```sh
unzip -o ~/storage/downloads/Ocarina-v0.3.2-fluidez.zip -d ~
bash ~/ocarina-music/push-to-github.sh
```

## Licenças

Veja `THIRD_PARTY.md` e `COPYING-GPL-3.0`. Os ícones Lucide são da biblioteca correspondente, não caracteres Unicode.
