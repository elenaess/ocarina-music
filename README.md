# Ocarina Music v0.2.1 — performance antes da estética 🎧

Protótipo Android nativo (Kotlin/Jetpack Compose + Media3), com busca experimental e áudio de fontes públicas. Esta versão prioriza **qualidade real da fonte**, início rápido e menor uso de rede.

## O que mudou
- Reproduz o fluxo de áudio **original de maior bitrate oferecido** pela integração não oficial; não faz upscaling fictício de 128 para 320 kbps.
- Mostra no player o bitrate aproximado **informado pelo extrator**, quando conhecido (não é uma medição do áudio reproduzido).
- `ExoPlayer` com buffer de início de 500 ms e rebuffer de 1,5 s; buffer dinâmico de 10–35 s. Em redes instáveis o player pode precisar de mais tempo.
- Cache LRU em disco de 48 MiB para partes de áudio reproduzido anteriormente. Cache é limitado e não representa download/offline.
- Reutiliza URLs temporárias por até 90 s, pré-resolve a próxima música nos últimos 25 segundos da faixa atual, sem competir durante o carregamento inicial.
- Cache de buscas em memória por 2 minutos para reduzir tráfego e transições entre abas.
- Uma única tentativa de recuperação de falhas de rede ou URL expirada. O streaming pode falhar se a plataforma bloquear a reprodução.

**Sobre 320 kbps:** YouTube Music anuncia até 256 kbps AAC/Opus no Premium. O catálogo independente pode entregar menos. Aumentar bitrate por transcodificação não aumenta fidelidade e adiciona latência/bateria. Se uma fonte fornecer 320 kbps reais, o player decodifica o original. Não implementamos transcodificação em tempo real nem promessas de 320 kbps em todas as faixas.

## Build
`Actions` → `Ocarina Music APK` → artefato `ocarina-music-v0.2.1-debug` em execução bem-sucedida.

## Enviar pelo Termux
```sh
pkg install -y git gh unzip tar
termux-setup-storage
unzip -o ~/storage/downloads/Ocarina-Music-v0.2.1-rapido.zip -d ~
bash ~/ocarina-music/push-to-github.sh
```

Código experimental: integrações não oficiais podem parar de funcionar ou ser restringidas por termos das plataformas. Não inclui bypass de DRM, login ou acesso a conteúdo restrito. Veja `THIRD_PARTY.md` para licença GPL do NewPipeExtractor.

**Status:** alterações verificadas estaticamente, sem compilação Android ou teste de reprodução no dispositivo neste ambiente.
