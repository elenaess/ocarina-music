# Ocarina v0.5.3

- Login inicial minimalista: símbolo Ocarina e duas opções, Google ou e-mail.
- E-mail abre tela secundária (entrar, criar conta e redefinir senha).
- Modo escuro passa a ser o padrão; tema claro segue disponível na edição do perfil.
- Telas internas mostram somente a seta de voltar (sem logo, avatar ou texto no cabeçalho); Busca com header fixo 60 dp e ícone de câmera à direita.
- Corações salvos preenchidos integralmente em rosa.
- Inter como tipografia baixável via Google Fonts no Android (fallback conforme suporte do dispositivo).
- URLs de compartilhamento de música, playlist publicada e perfis públicos com elena.services/ocarina/.
- Android App Links configurado; deploy/validação do domínio depende de assetlinks.json.
- Apagar uma playlist também solicita a retirada de sua publicação compartilhada.
- Firestore Rules atualizadas: coleção pública de snapshots de playlists (somente por compartilhamento explícito).

**A compilação Android final precisa ocorrer no GitHub Actions.**
