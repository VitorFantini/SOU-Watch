# Histórico de mudanças

## 1.4.7 — 2026-10-28

Consolidação dos ajustes desenvolvidos e conferidos no emulador em 27/10/2026; preparação de documentação e APK em 28/10/2026.

### Novidades

- Minutos estimados em verde após o nome de cada parada adiante do ônibus, na lista de pontos.
- Uma consulta do mesmo veículo por ciclo e projeções das paradas reutilizadas; sem estimativas para GPS vencido, trajeto incerto ou parada já passada.
- Atualização da lista enquanto visível e remoção do destaque quando o GPS expira.
- README com apresentação do projeto, capturas, limites das estimativas e instalação via ADB no smartwatch.

### Correções e ajustes consolidados

- Favoritos identificados por linha, sentido e parada, incluindo tratamento dos nomes antigos ambíguos.
- Abertura pelo Tile com tratamento de novos intents e contexto da configuração salva.
- Mensagens de consulta em português, estados vazios e controles de recuperação legíveis.
- Endereços completos na lista, rolagem e ações de navegação mais claras.
- Botão funcional para abrir o Maps e configuração acessível no mapa vazio.
- Fundo preto, marca e identificação da linha em amarelo; textos claros; palavras Ida e Volta em cores distintas.
- Horários com veículo localizado em caixa verde-escura e ícone em verde-claro.
- Estados programados e sem GPS diferenciados; mapa esquemático local e expiração do Tile.
- Consultas pausadas fora das telas e menor frequência de atualização de elementos da interface.

### Verificação

Compilação debug, 24 testes unitários e lint sem erros verificados em 28/10/2026. Os 16 testes do monitor também passaram. A validação anterior do Tile incluiu três testes Android no emulador. Autonomia no relógio físico e precisão dos minutos em campo ainda não são resultados comprovados.
