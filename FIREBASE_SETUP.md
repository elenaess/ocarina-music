# Ocarina 0.4.0 — Firebase Auth + Firestore

Projeto Firebase já fornecido: `ocarinaapp`. Esta versão usa os dados **públicos de configuração do app Android** enviados pelo usuário para inicializar Firebase pelo código. Não é necessário colocar `google-services.json` no GitHub, configurar plugin Google Services nem contratar Firebase Storage.

## Configuração única no Console Firebase

1. Abra `https://console.firebase.google.com/project/ocarinaapp/authentication/providers` e habilite o provedor **Email/Password** (não ative senha sem e-mail).
2. No Firebase Console, abra **Firestore Database** e crie um banco **Standard** se ele ainda não existir. Selecione a região adequada e **modo de produção**.
3. Entre em **Firestore Database → Rules**. Substitua pelas regras do arquivo `firestore.rules`, depois clique **Publish**. Essas regras são obrigatórias; não use modo de teste aberto.
4. O aplicativo está cadastrado como `com.ocarina.app`. O Gradle foi configurado com esse `applicationId`, conforme seu `google-services.json`.
5. Abra **Authentication → Users** para verificar os registros; a tela **Perfil** do Ocarina permite criar conta ou entrar com e-mail e senha.

## Dados salvos

- `users/{uid}`: `displayName`, `avatarBase64` (foto JPEG reduzida até 192 px) e `updatedAt`.
- `users/{uid}/favorites/{videoId}`: identificador de faixa, título, artista, miniatura. O avatar é privado a cada conta; nenhuma senha é salva no Firestore (gerenciada pelo Firebase Authentication).
- Reproduções recentes e tema permanecem locais. O Psiu não está conectado.

## Importante: novo identificador de app

O app antigo tinha `applicationId = com.ocarina.music`; o JSON fornecido foi gerado para **`com.ocarina.app`**. Por isso a 0.4.0 instala como **outro app** no Android, sem sobrescrever a versão 0.3.3. Dados locais do aplicativo antigo **não são migrados automaticamente**. Se você tiver favoritos antigos, **não desinstale** o app antigo até confirmar sua biblioteca.

## Sem Storage e sem cobranças previstas no uso pequeno

O Cloud Storage for Firebase geralmente requer Blaze. Nesta versão, o avatar é reduzido e armazenado no documento privado do Firestore, que funciona dentro da cota gratuita de Firestore, sujeita a limites de leituras/gravações. Não substitui um armazenamento de mídia em grande escala; é intencionalmente limitado para poucos perfis.

## Testar

1. Instale o APK gerado pelo GitHub Actions.
2. Abra Perfil e crie uma conta, em seguida altere o nome e a foto.
3. Curta uma música na Busca, feche e abra o app.
4. Entre na mesma conta em outro dispositivo compatível para conferir favoritos e foto sincronizados.

O app não foi compilado/testado em Android neste ambiente. Em caso de falha, confira o log em Actions e, para encerramentos em runtime, colete `adb logcat -d -s AndroidRuntime:E OcarinaPlayback:E`.
