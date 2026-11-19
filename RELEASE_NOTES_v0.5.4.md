# Ocarina v0.5.4 — Motion & Fila Inteligente

- Navbar: uma única cápsula rosa desliza entre as quatro abas com mola amortecida e ícones com tint suave.
- Player: entra de baixo e retorna ao miniplayer com animação vertical quando fechado, incluindo o botão Voltar.
- Fila: camada dentro do player, descendo para uma área de lista abaixo do mini-player; ao fechar a camada sobe de volta.
- Fila inteligente (opcional, habilitada inicialmente): quando chegam ao fim as faixas da fila e a repetição da lista está desativada, oferece a próxima faixa do histórico, curtidas ou descobertas já carregadas. Sem novas requisições adicionais no momento de sugerir.
- Tela da fila: música atual fixa no topo, ordem a seguir, remover, ouvir agora e adicionar sugestões.
- O player, o login Google, os links próprios e as regras do Firestore foram preservados.

## Verificação
Não foi possível executar compilação Android local: é necessário que o GitHub Actions conclua assembleDebug com sucesso. Testes estruturais e análise sintática do Kotlin realizados antes da entrega.
