# Ocarina v0.4.1 — login Google + Firebase

## O que funciona no código

- Login Google com Credential Manager e Firebase Authentication.
- Login alternativo com e-mail/senha.
- Perfil com nome editável e avatar compacto salvo no documento privado do usuário (Firestore).
- Se o usuário não tiver enviado avatar, exibe foto da conta Google quando disponível.
- Favoritos sincronizados em `users/{uid}/favorites/{videoId}` e cache local por conta.
- Ícone colorido G fornecido pelo usuário na tela de login.
- Mantém navbar compacta Lucide e player nativo.

## Configuração exigida no Firebase

1. Em **Authentication**, habilite provedor **Google**.
2. Em **Firestore**, cole `firestore.rules` e publique.
3. Use pacote Android **com.ocarina.app** no projeto Firebase `ocarinaapp`.
4. Configure a assinatura persistente para APKs do GitHub:
   - Instale `pkg install -y gh openjdk-17 openssl coreutils` no Termux.
   - Autentique `gh auth login` se necessário.
   - Execute `bash setup-google-signin-termux.sh` (não adicione a chave privada ao Git).
   - Copie o **SHA1** informado ao Firebase > configurações do projeto > seu app Android.
   - O script grava o keystore como segredos criptografados no GitHub Actions. Guarde backup privado da pasta `~/.ocarina-signing`.
5. Depois de registrar o SHA-1, baixe o JSON atualizado no Firebase se futuramente ativar o plugin Google Services. Este projeto inicializa Firebase explicitamente e usa o **cliente Web OAuth** do JSON fornecido.

## Instalação e compilação

O GitHub Actions gera o APK debug. Se o Google der erro `DEVELOPER_ERROR`, verifique SHA1 registrado e pacote `com.ocarina.app`. Cada assinatura/keystore usada para gerar APKs precisa ter seu fingerprint no projeto Firebase. O app antigo `com.ocarina.music` é outro app Android, sem migração local automática.

## Restrições

Esta versão não foi compilada no aparelho. Integração musical não oficial continua sujeita a falhas do provedor. O armazenamento de avatar no Firestore é adequado apenas para fotos pequenas de contas pessoais e não serve como serviço geral de imagens.
