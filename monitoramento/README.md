# Monitor de validação do SouWatch

A coleta acompanha as linhas **01, 06 e 32** a cada 60 segundos e guarda respostas, horário, duração, status HTTP, validade do GPS e associação do veículo à partida. Executa os testes JVM reais do projeto Kotlin a cada seis horas. Os relatórios são atualizados após os ciclos; cada dia tem HTML, JSON e CSV separados.

## Iniciar e parar

No PowerShell, nesta pasta:

```powershell
.\Iniciar.cmd
.\Parar.cmd
```

Você também pode dar dois cliques em `Iniciar.cmd` e `Parar.cmd`. Esses arquivos funcionam sem alterar a política de execução de scripts do PowerShell. O monitor fica em segundo plano. O computador deve permanecer ligado, conectado e sem suspensão. Não é um serviço na nuvem e não inicia sozinho após reiniciar o Windows. Não modifica o aplicativo do relógio. Para observar o processo no terminal, em vez do iniciador:

```powershell
python monitor.py run
# Iniciar em segundo plano pelo Python:
python monitor.py start
python monitor.py stop
# Uma coleta:
python monitor.py run --once
# Encerrar automaticamente após 24 horas:
python monitor.py run --hours 24
```

`config.json` contém as linhas, intervalos e o fuso de interpretação dos horários da operadora. O fuso é registrado no relatório para tornar os resultados reproduzíveis. Use Python 3.10+ com dados IANA de fuso disponíveis (o Python incluído no Codex já está configurado). Os arquivos `.cmd` usam esse Python quando disponível. Os testes Kotlin requerem o SDK, JDK 21 e o cache Gradle deste projeto; usam modo offline e reexecutam os testes, sem tratar resultado em cache como execução nova.

## Onde ficam os dados

- `saida/dados.sqlite3`: banco com ciclos, consultas, observações e testes.
- `saida/relatorios/ultimo.html`: relatório mais recente.
- `saida/relatorios/AAAA-MM-DD.html`: relatório diário, com CSV e resumo JSON.
- `saida/monitor.log` e `saida/testes-app-*.log`: execução e testes Kotlin.
- `saida/referencias-sou/`: evidências capturadas do celular, quando solicitado.

As respostas brutas são compactadas no banco, com SHA-256. Para extrair uma consulta (substitua 1 pelo ID real):

```powershell
python monitor.py extract --request 1
python monitor.py report --date 2026-10-23
```

Os horários `captured` dos CSVs são timestamps Unix em UTC. O HTML informa o fuso. Preserve o banco, os arquivos de configuração e a versão do projeto junto aos relatórios do TCC. O banco cresce enquanto houver coleta; não há exclusão automática de evidências. Feche o monitor antes de copiar o banco, ou use o backup do SQLite para incluir os dados do WAL.

## Comparação com o aplicativo oficial SOU no celular

A coleta automática acessa a fonte Mobilibus usada pelo app. **Ela não lê automaticamente a tela do SOU.** O celular precisa estar conectado por USB, com depuração USB autorizada, para capturar evidências da interface. Use `adb devices -l` para identificar o serial. Abra no celular o SOU na linha e ponto desejados e execute:

```powershell
python monitor.py capture-sou --serial 'SERIAL_DO_CELULAR'
```

O script só captura se o SOU estiver em primeiro plano. Salva PNG, estrutura acessível quando disponível e hash/horário da evidência. Não navega no celular. O pacote identificado nos registros do projeto é `br.com.sou.app`; se a instalação usar outro pacote, é necessário verificar antes de adaptar o script.

Após conferir a tela, registre as previsões do SOU e do SouWatch. Os valores abaixo são **exemplos de sintaxe**: substitua pelos dados realmente observados. `--at` deve conter o horário da observação com fuso, não o horário em que você digitou o registro depois.

```powershell
python monitor.py reference --source sou --line 01 --vehicle 2035 --departure 16:15 --stop 123456 --eta 8 --at '2026-10-23T16:30:00-03:00' --note 'Caminho da captura conferida'
python monitor.py reference --source souwatch --line 01 --vehicle 2035 --departure 16:15 --stop 123456 --eta 9 --at '2026-10-23T16:30:20-03:00'
```

O relatório só compara pares da mesma linha, veículo, saída e ponto, observados com no máximo 120 segundos de diferença, usando cada observação uma única vez. Nenhum dado de exemplo foi inserido no banco real.

## Medir precisão de chegada

Concordância com o SOU é uma referência útil, mas ambos podem compartilhar o mesmo GPS e errar juntos. Para medir erro de previsão, anote quando o ônibus realmente passa no ponto:

```powershell
python monitor.py reference --source campo --line 01 --vehicle 2035 --departure 16:15 --stop 123456 --at '2026-10-23T16:38:45-03:00' --note 'Passagem observada presencialmente'
```

Quando houver previsão e passagem compatíveis, o relatório calcula erro absoluto médio e viés de cada app. Associa a primeira passagem observada nas quatro horas seguintes. Múltiplas previsões da mesma viagem são correlacionadas; o número de previsões não é o número de viagens independentes. Uma previsão arredondada em minutos tem resolução limitada. A coleta não mede automaticamente bateria, usabilidade, tela congelada nem precisão de chegada física.

## Interpretação dos resultados

- Resposta válida e vazia é consulta bem-sucedida sem veículo informado, não cancelamento confirmado.
- Falta de serviço no calendário, calendário especial, GPS sem hora, GPS antigo e erro de consulta são situações distintas.
- Disponibilidade é a proporção de consultas realizadas com resposta válida. Falha de internet no computador também causa falha de consulta; isso não prova indisponibilidade global da operadora.
- Cobertura indica quantos intervalos do dia efetivamente tiveram ciclos concluídos; não inventa amostras antes do início, durante suspensão ou desligamento.
- Mediana e p95 de latência usam consultas válidas. Erros têm duração e motivo nos CSVs.
- Os testes do projeto cobrem as regras Kotlin. Uma aprovação não comprova que todas as telas e APIs funcionaram naquele instante.

## Testar o próprio monitor

```powershell
python -m unittest discover -s tests -v
```

Os testes usam dados sintéticos isolados, incluindo erro de JSON, GPS, calendário, pareamento, verdade de campo, relatório vazio e falha de infraestrutura do Gradle. Esses dados não são inseridos no banco da coleta.
