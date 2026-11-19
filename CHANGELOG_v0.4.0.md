# Ocarina 0.4.0

- Firebase Authentication (e-mail + senha) e sessão persistente.
- Perfil local/pessoal com nome e foto, JPEG miniatura gravada em documento privado no Firestore.
- Favoritos por usuário, com cache local e atualização da nuvem.
- App ID alinhado ao `google-services.json` fornecido: `com.ocarina.app`.
- Sem necessidade de Firebase Storage, segredos de GitHub Actions ou `google-services.json` no repositório.
- Proteção adicional contra respostas antigas de busca de áudio interromperem a faixa atual.
- Não é uma garantia de reprodução de todas as faixas via fonte não oficial, nem de ausência de travamentos: faltam testes de dispositivo.
