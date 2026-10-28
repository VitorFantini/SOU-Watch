# Google Maps — versão 1.5.1

O aplicativo usa Maps SDK for Android nas telas (Lite no acompanhamento e interativo na tela ampliada) e Maps Static API para o Tile. Os mapas locais continuam disponíveis quando falta chave ou ocorre falha. O APK pessoal 1.5.1 foi instalado no Galaxy Watch 7 e conferido por hash. O Google Maps real e o enquadramento do trajeto completo foram conferidos no emulador nos pontos 11, 26 e 33, usando posições de ônibus simuladas. A validação de campo no relógio permanece em andamento.

## Configuração pessoal

1. Configure seu próprio projeto Google Cloud e as APIs necessárias.
2. Copie `maps.local.properties.example` para `maps.local.properties`, na raiz do projeto.
3. Preencha `MAPS_ANDROID_API_KEY` com sua chave do SDK Android.
4. Opcionalmente, preencha `MAPS_STATIC_API_KEY` com a chave da Maps Static API. Quando esse campo fica vazio, a configuração local reutiliza a chave Android; as APIs permitidas nessa chave precisam ser compatíveis com esse uso.

Também podem ser usadas as variáveis de ambiente de mesmo nome. O arquivo pessoal é ignorado pelo Git. No ambiente de recuperação local, se `maps.local.properties` não existir, a preparação anterior em `recuperacao/google-maps-1.5.properties` é lida como alternativa.

A chave do SDK Android pode ser restringida ao pacote `com.vitorfantini.souwatch.minimo`, certificado de assinatura e API do SDK. As restrições da credencial Static devem ser compatíveis com esse serviço; não presumir que a mesma configuração de aplicação serve para os dois produtos. O projeto não muda restrições ou faturamento no Google Cloud.

Sem chave, nenhum componente Google é iniciado, e o app e os Tiles continuam com o desenho local. O SDK usa o modo clássico sem Map ID. O mapa ampliado não solicita localização pessoal do relógio: os marcadores usam o GPS do ônibus fornecido pela fonte de transporte.

## Modo público sem chave do desenvolvedor

Acrescente `-PpublicBuild=true` aos comandos Gradle usados para uma distribuição pública. Esse parâmetro ignora arquivos pessoais e variáveis de ambiente e gera os campos das duas chaves vazios.

No modo de edição, valide sem APK:

```powershell
.\gradlew.bat compileDebugKotlin compileDebugAndroidTestKotlin testDebugUnitTest lintDebug -PpublicBuild=true
```

Para gerar um APK público, usar `assembleDebug -PpublicBuild=true`. Cada pessoa pode compilar sua versão pessoal com a própria configuração, sem esse parâmetro.

Manter a chave fora do Git não a torna secreta dentro de uma compilação pessoal que a utilize. APKs pessoais com chave não devem ser usados como os artefatos públicos do repositório.

## Funcionamento e alternativas

- O mapa pequeno usa Lite, com toque para abrir a tela ampliada. A instância é mantida nas atualizações do veículo.
- A tela ampliada permite gestos, mostra o percurso e mantém o mesmo veículo da saída escolhida; no contexto do Tile, segue os resultados do Meu ponto.
- Google Maps que não termina de carregar em 15 segundos dá lugar ao desenho local.
- O GPS vencido remove o ônibus, preservando o fundo e o ponto. Uma imagem de ruas não é prova de GPS válido.
- O Tile enquadra ônibus e ponto, pede uma imagem base de ruas e desenha o veículo pela projeção Web Mercator da própria imagem. A versão sem GPS não contém o ônibus.
- Logos e atribuições da imagem Google são preservados. O desenho do ônibus evita a faixa de atribuição inferior.
- Imagens Google não são gravadas no cache PNG persistente do mapa local. Há somente dados transitórios necessários à entrega da imagem atual do Tile.
- Os testes visuais locais usam o provedor local; o teste específico `previewGoogleTrajetoCompleto` usa Google Maps real com posições simuladas e exige uma chave pessoal válida.

## Controle inicial de Maps Static

Limites compartilhados pelas solicitações do Tile **nesta instalação**:

- no máximo 250 tentativas por dia UTC;
- no máximo 8.000 tentativas por mês UTC;
- intervalo mínimo de 60 segundos entre tentativas;
- tentativas com falha também consomem o contador local, de forma conservadora;
- o fundo da entrega atual pode permanecer em memória por até 90 segundos enquanto os marcadores são atualizados;
- em bloqueio de limite ou falha da API, usar o mapa local.

Esses contadores não são cotas do Google Cloud e não controlam chamadas de outras instalações, outros projetos ou terceiros que usem a chave. As cotas e os alertas do Cloud continuam relevantes quando forem iniciados os testes reais. Os limites locais não garantem cobrança zero para a conta inteira.

## Verificação de campo pendente

Validar autenticação, disponibilidade de Google Play services, logos no relógio redondo, gestos, carregamento sem rede, bloqueio de cota, expiração do GPS, bateria e contagem real de requisições. A compilação e os testes unitários não comprovam que o faturamento e as APIs da conta estejam configurados corretamente.

## Referências

- [SDK Android no Wear OS](https://developers.google.com/maps/documentation/android-sdk/wear)
- [Configuração do SDK Android](https://developers.google.com/maps/documentation/android-sdk/config)
- [Maps Static API](https://developers.google.com/maps/documentation/maps-static/start)
- [Preços por produto](https://developers.google.com/maps/billing-and-pricing/pricing)
- [Segurança e credenciais](https://developers.google.com/maps/api-security-best-practices)
