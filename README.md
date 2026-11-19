# Ocarina v0.5.0 — redesign (código fonte)

- Login obrigatório na entrada, Google/E-mail; Firebase continua em `com.ocarina.app`.
- Home personalizada com chips, playlists e recomendações.
- Busca visual com categorias; músicas antes de vídeos quando a fonte traz essa classificação.
- Biblioteca em listas, navbar com avatar, perfil em modo visualização e edição separada.
- Diretório de perfis públicos **opt-in**, sem expor e-mail, favoritos ou histórico privado.
- Player em estilo CD; o motor Media3 e o mecanismo de áudio da v0.4.3 foram preservados.
- Ícone grafite com rosa em degradê.

**Antes de testar perfis públicos:** atualize as regras do Firestore com `firestore.rules`.
A busca por perfis usa um prefixo do nome. Só aparecem contas que tenham ativado a opção
**Aparecer na busca** em Editar perfil. A foto pública só é enviada com essa escolha.

**Limitações honestas:** a classificação das faixas depende de marcadores do catálogo não oficial
YouTube Music; podcasts, transmissões ao vivo e vídeos nem sempre vêm identificados pela fonte.
O app não baixa áudio do YouTube. Downloads são arquivos próprios importados pelo usuário.
Este ZIP contém o código, não um APK compilado. Consulte `TEST_REPORT_v0.5.0.txt`.

**Termux:** use o seletor nativo para importar o ZIP e execute `push-to-github.sh`.
**Assinatura:** o workflow preserva a chave estável configurada anteriormente.

---

# Ocarina 0.5.0

Aplicativo Android experimental com player nativo, busca musical, modos claro/escuro e agora conta Firebase.

**Configuração Firebase:** veja [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md). Ative Email/Password e publique [`firestore.rules`](firestore.rules) antes de usar as funções de nuvem.

**Atenção:** devido ao JSON fornecido ter o pacote `com.ocarina.app`, a v0.4.1 instala como um novo app e não substitui o pacote antigo `com.ocarina.music`. Favoritos do app antigo não são transferidos automaticamente.

O código inclui configuração *cliente* do Firebase, que não é uma credencial administrativa. Firebase Authentication e Security Rules protegem acesso aos documentos privados. Não hospede credenciais de administrador neste repositório.

A reprodução via mecanismos alternativos do YouTube é experimental e pode não funcionar em todas as músicas.

# Ocarina — v0.3.3 (experimental)

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
unzip -o ~/storage/downloads/Ocarina-v0.3.3-fluidez.zip -d ~
bash ~/ocarina-music/push-to-github.sh
```

## Licenças

Veja `THIRD_PARTY.md` e `COPYING-GPL-3.0`. Os ícones Lucide são da biblioteca correspondente, não caracteres Unicode.

## Ajustes v0.3.3

- Navegação compacta com 4 ícones Lucide, sem rótulos ou animação Material.
- Área inferior respeita a barra de gestos do Android.
- Início começa em "Continue ouvindo", sem saudação/filtros.
- Sincronização de metadata entre buscas e player corrigida, evitando restaurar faixa anterior.
- Menos camadas na animação do player, imagens de lista decodificadas em tamanho limitado.
- Tipografia usa família sans do sistema por compatibilidade de licença.

Ainda é necessário validar o comportamento em dispositivo real: falhas específicas da fonte
não oficial (por exemplo, vídeos indisponíveis) não podem ser garantidamente corrigidas.


## Login com Google
Veja [GOOGLE_LOGIN_SETUP.md](GOOGLE_LOGIN_SETUP.md) para registrar SHA-1 e configurar assinatura estável nos APKs do GitHub Actions.
