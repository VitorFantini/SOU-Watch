# Registro da recuperação — 23/10/2026

## Origem preservada

- Dispositivo: Samsung Galaxy Watch 7 SM-L310.
- Aplicativo extraído: `com.vitorfantini.souwatch.minimo`, versão 1.4, código 5.
- APK original: `recuperacao/galaxy-watch7-2026-10-23/souwatch-watch7.apk`.
- SHA-256 original: `66D8A4166FBEAC60CB93503E6C736C46808CE9D03DFEF784C2FFB140AA2B810A`.
- Backup dos fontes anteriores com estrutura pronta para restauração: `recuperacao/galaxy-watch7-2026-10-23/projeto-anterior-restauravel.zip`. O arquivo inicial `projeto-anterior.zip` também foi preservado.
- Referência descompilada pelo JADX: `recuperacao/galaxy-watch7-2026-10-23/decompiled/`.

## Fontes reconstruídos

- `presentation/data/BusRepository.kt`: modelos e consultas Mobilibus; vínculo da viagem à partida; cache de detalhes.
- `presentation/data/RastreamentoCorreto.kt`: calendário, seleção de veículo, validade de GPS, polylines, projeção e passagem.
- `presentation/MainViewModel.kt`: horários/paradas da API, tarefas canceladas ao ocultar a tela e acompanhamento a cada 20 s.
- `presentation/BusScreen.kt` e `MainActivity.kt`: estados, horários identificados, acesso ao Tile e ciclo de vida.
- `presentation/tile/`: configuração persistente, seleção do próximo ônibus elegível, Tile com expiração e mapa local ampliado.
- Manifesto e Gradle: serviço Tile, atividades de configuração/mapa, bibliotecas Tiles/ProtoLayout e pacote da versão recuperada.

## Validações realizadas

- Compilação debug concluída.
- 16 testes unitários: calendário e sentido, associação de partida, continuidade do veículo, duplicidade, validade/virada do dia do GPS, filtragem das duas saídas anteriores, polyline, projeção ambígua, passagem e expiração do Tile.
- 3 testes Android no emulador Galaxy Watch: persistência, mapa com remoção de GPS vencido e timeline/imagens do Tile.
- Lint concluído sem erros; há avisos de dependências antigas, recursos não usados e recomendações de estilo.
- Navegação de linha/sentido/horário/pontos exercitada no emulador com dados reais da Mobilibus.
- Resposta da tabela horária atual consultada e compatível com os modelos restaurados.

## Limites da validação

O código Kotlin foi reconstruído e não é idêntico aos arquivos-fonte originais. A descompilação de Compose e coroutines contém trechos incompletos e exigiu reescrita. As principais regras e componentes foram recuperados, mas não foi feita uma comparação visual pixel a pixel de todas as telas, nem teste de chegada de um ônibus na rua. Os testes Android utilizam dados sintéticos para os estados de previsão e expiração; a navegação também foi verificada com dados reais. O APK do relógio físico foi preservado e não recebeu atualização.


## Ajuste da hora do relógio

O fuso agora é lido de ZoneId.systemDefault() em cada acesso, acompanhando a configuração do relógio. A hora corrente continua vindo do sistema do dispositivo. Compilação e 17 testes unitários aprovados, incluindo mudança de fuso, filtragem de partidas e idade do GPS no horário local.
