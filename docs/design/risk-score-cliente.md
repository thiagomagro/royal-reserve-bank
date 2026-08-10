# Risk Score por Cliente — Design Doc

## Problema

**Problema:** hoje o Royal Reserve Bank não tem nenhuma medida de risco associada a quem opera na plataforma; contas e transações são tratadas sem qualquer diferenciação de perfil.

**Resultado esperado:** cada cliente possui um *risk score* consultável em tempo real por outros serviços (ex.: liberação de crédito) e também recalculado em background, considerando saldo, volume e frequência de transações, tempo de casa e KYC.

## Contexto atual (verificado no código)

O sistema é um conjunto de microsserviços Spring Boot 3 / Java 17 (`pom.xml`), com Eureka, Config Server, API Gateway, Kafka e bancos separados por serviço.

Pontos que condicionam diretamente esta demanda:

1. **Não existe entidade "cliente".** O que existe é conta: `account-api/src/main/java/com/royal/reserve/bank/account/api/model/Account.java` tem apenas `id`, `accountNumber`, `accountHolderName`, `balance`, `currency`. Não há CPF/CNPJ, data de abertura, dados de KYC ou qualquer outro atributo cadastral. O "cliente" hoje é, na prática, uma string de nome — e `accountHolderName` não é único, portanto **não há como agrupar as contas de um mesmo cliente**.
2. **Transação não é ligada a conta nem a cliente.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/model/Transaction.java` guarda só `id`, `transactionId` (UUID) e uma lista de `TransactionItems` (`assetCode`, `assetName`, `value` como `int`). O payload de entrada `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/dto/TransactionRequest.java` contém apenas `transactionItemsDtoList` — não há remetente, destinatário nem valor monetário com moeda. Ou seja, **não é possível hoje reconstruir o histórico transacional de um cliente**.
3. **O evento publicado no Kafka é mínimo.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/service/TransactionService.java` envia `new TransactionEvent(transaction.getTransactionId())` no tópico `notificationTopic`; `notification-api/src/main/java/com/royal/reserve/bank/notification/api/NotificationApiApplication.java` só faz log do `transactionId`. Não há stream de eventos rico para alimentar um score.
4. **Não há consulta de contas por cliente.** `account-api/src/main/java/com/royal/reserve/bank/account/api/service/AccountService.java` — em `getAllAccounts()` — lê tudo do Mongo e cacheia a lista inteira em Redis sob a chave `accounts`; `deleteAccountByAccountHolderName` faz `findAll()` e filtra em Java. `account-api/src/main/java/com/royal/reserve/bank/account/api/repository/AccountRepository.java` é um `MongoRepository` sem nenhum método de busca. Qualquer leitura "por cliente" precisa ser criada.
5. **Não há conversão de moeda.** Cada conta tem sua própria `Currency` (`Account.java`; os dados de teste em `account-api/src/main/java/com/royal/reserve/bank/account/api/util/AccountTestData.java` usam EUR, GBP e HUF) e não existe nenhum serviço, tabela ou cliente de câmbio no repositório. Somar saldos das contas de um cliente exige uma moeda base e uma fonte de taxas que hoje não existem.
6. **Não há nenhum job agendado.** Não existe uso de `@Scheduled`/`@EnableScheduling` em nenhum módulo — o modo background precisa ser construído do zero (ou delegado a um agendador externo).
7. **Gateway e segurança.** Rotas são declaradas em `config-files/api-gateway.properties` (`spring.cloud.gateway.routes[n]`); `api-gateway/src/main/java/com/royal/reserve/bank/api/gateway/config/SecurityConfig.java` exige autenticação em `anyExchange()` exceto `/eureka/**` e `/discovery-server/**`. Um novo serviço precisa de rota nova + registro no Eureka. Não há autorização por escopo/role — apenas "token válido".
8. **Infra e build.** Novo módulo implica entrada em `pom.xml` (`<modules>`), serviço no `docker-compose.yml`, arquivo em `config-files/` e, se usar banco próprio, mais um container. Não há workflows de CI em `.github/` (só templates de issue), então a validação é `mvn verify` local.
9. **Dados de teste são semeados via `CommandLineRunner`** (`AccountTestData.java`, `asset-management-api/src/main/java/com/royal/reserve/bank/asset/management/api/util/AssetTestData.java`), e `spring.jpa.hibernate.ddl-auto=create-drop` no account/asset (`config-files/account-api.properties`, `config-files/asset-management-api.properties`) — o ambiente é de demonstração, não produtivo.

## Decisões do negócio (respondidas)

- **Granularidade:** o score é **por cliente**, agregando todas as contas dele.
- **Fatores:** saldo, volume de transações, frequência de transações, tempo de casa e KYC.
- **Régua:** o time técnico propõe a régua inicial (abaixo), a ser validada por Risco/Crédito.
- **Modos de operação:** os dois. (a) **Realtime**, consultável por serviços como liberação de crédito, com latência aceitável de **300 ms**; (b) **background**, recálculo **diário**.
- **Moeda base:** **BRL**.
- **Fatores econômicos internos do banco:** ficam **fora da primeira entrega** — o negócio ainda está validando quais serão. O job diário entra desde já, mas apenas com os cinco fatores de cliente; os fatores econômicos entram depois como componente adicional.
- **Backfill:** contas existentes usam como data de abertura a **data de implantação do risk score**.
- **KYC:** será cadastrado no próprio `account-api`, e **opcional** nesta fase para clientes que ainda não o possuem.
- **Acesso:** qualquer usuário com **role de admin** pode consultar.
- **Bureau externo:** fora de escopo por enquanto.
- **Exposição:** score **interno** apenas, não exposto ao cliente final.

> **Atenção — não existem roles hoje.** `api-gateway/src/main/java/com/royal/reserve/bank/api/gateway/config/SecurityConfig.java` apenas valida a assinatura do token e exige `anyExchange().authenticated()`; não há nenhuma verificação de escopo, claim ou role em nenhum módulo. "Admin pode consultar" implica criar a claim de role no provedor de identidade (Auth0, conforme `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` em `config-files/api-gateway.properties`) e passar o gateway a autorizar por ela — trabalho que não existe no sistema atual e que deve ser tratado como item próprio, não como detalhe do score.

### Consequência direta: nenhum dos fatores pedidos existe hoje

| Fator pedido | Dado necessário | Existe hoje? |
|---|---|---|
| Saldo (agregado do cliente) | agrupamento conta→cliente + moeda base | Não (itens 1 e 5) |
| Volume de transações | valor monetário na transação + dono da transação | Não (item 2) |
| Frequência de transações | data/hora da transação + dono da transação | Não (item 2 — `Transaction` não tem timestamp) |
| Tempo de casa | data de abertura do cliente/conta | Não (item 1) |
| KYC | status e validade de KYC | Não (item 1) |

Portanto existe uma **Fase 0 obrigatória de modelo de dados**, anterior a qualquer cálculo de score:

- criar entidade `Customer` no `account-api` (documento/identificação, data de abertura, status de KYC) e passar `Account` a referenciar `customerId`;
- incluir na transação o `customerId` (ou `accountId`) de origem/destino, o valor monetário com moeda e o timestamp;
- enriquecer o evento publicado no Kafka com esses campos (hoje só `transactionId`).

Sem a Fase 0 é possível entregar apenas um score de fachada; com ela, qualquer das abordagens abaixo funciona.

## Régua inicial proposta (para validação de Risco/Crédito)

Escala **0–1000, maior = mais risco**; faixas: **BAIXO 0–299**, **MÉDIO 300–599**, **ALTO 600–1000**. Score = soma ponderada de quatro componentes, cada um normalizado em 0–1000:

| Componente | Peso | Régua proposta (valores em BRL) |
|---|---|---|
| KYC | 30% | verificado e vigente = 0; incompleto/pendente = 500; **ausente = 500** (neutro nesta fase, ver abaixo); expirado = 1000 |
| Tempo de casa | 20% | > 24 meses = 0; 6–24 meses = 400; < 6 meses = 1000; **cliente legado (data de abertura = data de implantação) = 400** |
| Saldo agregado em BRL | 20% | ≥ R$ 50.000 = 0; entre R$ 1.000 e R$ 50.000 = interpolação linear; ≤ R$ 1.000 = 1000 |
| Comportamento transacional (90 dias) | 30% | 0 até 10 transações/mês e valor individual até 3× a mediana do próprio cliente; sobe linearmente a partir daí; ≥ 30 transações/mês **ou** transação ≥ 5× a mediana = 1000; cliente sem histórico = 500 |

Todos os pesos e limiares devem ser parametrizáveis via Config Server (`config-files/`), não constantes em código, para permitir recalibração sem deploy. Cada score deve persistir os **fatores que o compuseram**, tanto para auditoria quanto porque score sem explicação não é acionável por Risco.

**Duas consequências das decisões de backfill e de KYC opcional, que mudam a régua:**

1. Se toda a base legada recebe a data de implantação como data de abertura, no dia 1 **todo cliente existente cai em "< 6 meses" = 1000** no componente de tempo de casa (20% do score). Isso jogaria a base inteira para MÉDIO/ALTO sem nenhuma razão de risco. Por isso a proposta marca o cliente legado com uma flag e usa valor neutro (400) até que ele acumule 6 meses de histórico real.
2. Pelo mesmo motivo, **KYC ausente vale 500 e não 1000** enquanto o cadastro for opcional. Quando o preenchimento de KYC se tornar obrigatório, esse limiar deve ser endurecido — é uma mudança de parâmetro, não de código.

Combinados, esses dois ajustes fazem um cliente legado sem KYC e com saldo mediano nascer em torno de 400–500 (MÉDIO), o que é o comportamento desejado: nem inocenta nem condena por falta de dado.

## Abordagens

Todas assumem a Fase 0 concluída.

### Abordagem A — Score dentro do `account-api`

O `account-api` — que passará a ser o dono do cadastro de cliente — expõe `GET /api/customer/{id}/risk-score` (realtime) e mantém um `@Scheduled` de recálculo, persistindo o resultado junto ao cliente em Mongo. Os dados transacionais vêm do `transaction-api` via Feign ou de um consumo do tópico Kafka.

- **Escopo:** novo pacote de risco no `account-api`, endpoint, job agendado, consumo de eventos.
- **Componentes afetados:** `account-api` (+ dependência Kafka/Feign nova), `config-files/account-api.properties`, cache Redis existente.
- **Esforço:** M
- **Dependências:** Fase 0.
- **Risco de desenho:** acopla cadastro e motor de risco no mesmo serviço; o recálculo em background compete por recursos com o caminho de leitura de contas, e a invalidação atual da chave `accounts` (item 4) fica mais frequente. Se o endpoint calcular sob demanda (agregando contas e chamando o `transaction-api` a cada consulta), o orçamento de **300 ms** fica apertado — o que empurra esta abordagem para também persistir o score, aproximando-a do custo da B sem os ganhos de isolamento.
- **Confiança de sucesso da implantação: 75**

### Abordagem B — Novo microsserviço `risk-score-api` com score persistido, endpoint realtime e recálculo por evento + job

Módulo novo `risk-score-api` com banco próprio (padrão database-per-service), entidade `CustomerRiskScore` (`customerId`, `score`, faixa, fatores, `calculatedAt`) e três caminhos: (1) `GET /api/risk-score/{customerId}` devolvendo o score persistido — leitura barata, adequada a consulta por serviços de crédito; (2) consumo do evento de transação enriquecido no Kafka, recalculando o cliente afetado; (3) job diário de recálculo geral, onde depois entram os fatores econômicos internos do banco. Dados cadastrais vêm do `account-api` por Feign, no mesmo padrão de `transaction-api/.../client/AssetManagementClient.java`.

- **Escopo:** novo módulo Maven + rota no gateway + serviço no `docker-compose.yml` + arquivo em `config-files/` + novo tópico Kafka; endpoint de leitura de cliente no `account-api`.
- **Componentes afetados:** novo módulo, `account-api`, `transaction-api` (publicação do evento enriquecido — já contemplada na Fase 0), `api-gateway`, `pom.xml`, `docker-compose.yml`, coleção Postman.
- **Esforço:** G
- **Dependências:** Fase 0; criação da role de admin no provedor de identidade para expor a consulta.
- **Latência:** a consulta é uma leitura de um registro já materializado (com cache Redis, padrão já usado em `account-api` e `transaction-api`), portanto confortável dentro dos 300 ms — o cálculo pesado fica no consumidor de eventos e no job.
- **Confiança de sucesso da implantação: 70**

### Abordagem C — `risk-score-api` apenas em batch, com leitura do score materializado

Mesmo serviço da Abordagem B, sem consumo de eventos: o score é recalculado somente pelo job e a consulta devolve o último valor materializado.

- **Escopo:** subconjunto da Abordagem B (sem consumidor Kafka).
- **Componentes afetados:** novo módulo, `account-api`, `api-gateway`, `pom.xml`, `docker-compose.yml`.
- **Esforço:** M
- **Dependências:** Fase 0.
- **Limitação:** atende "consulta rápida", mas **não atende o requisito de realtime** — um cliente que acabou de transacionar é avaliado com dados velhos, exatamente o cenário em que a liberação de crédito mais precisa do score atualizado.
- **Confiança de sucesso da implantação: 80** (confiança alta de implantar, cobertura funcional parcial)

## Trade-offs

| Critério | A — no account-api | B — serviço + eventos + job | C — serviço batch |
|---|---|---|---|
| Complexidade | Média | Alta | Média |
| Risco de entrega | Médio | Médio-alto | Médio |
| Impacto em sistemas existentes | Concentrado no `account-api` | Espalhado (gateway, Kafka, compose, 2 serviços) | Moderado |
| Reversibilidade | Média (código dentro de serviço crítico) | Alta (serviço isolado, desligável) | Alta |
| Atende realtime (300 ms) | Só se materializar o score | Sim | Sim na leitura, com dado defasado |
| Atende background diário | Sim | Sim | Sim |
| Isolamento de falha | Baixo (risco derruba cadastro) | Alto | Alto |
| Auditabilidade | Média | Alta (fatores persistidos + histórico) | Alta |
| Evolução (fatores econômicos, bureau futuro) | Ruim (engorda o `account-api`) | Boa | Boa |

## Riscos

- **Risco de dado, o principal:** os cinco fatores pedidos dependem de dados que não existem (tabela acima). A Fase 0 é o verdadeiro caminho crítico; qualquer cronograma que a ignore vai entregar score sem lastro.
- **Contrato público:** incluir cliente, valor e timestamp em `TransactionRequest` afeta `postman/postman-collection.json` e qualquer consumidor externo. Exige campo opcional em transição ou versionamento de rota.
- **Migração de dados:** contas existentes não têm `customerId`; o agrupamento conta→cliente da base legada não é dedutível com segurança, já que `accountHolderName` não é único (item 1) — homônimos seriam fundidos no mesmo cliente. O backfill precisa de critério do negócio ou de conferência manual.
- **Câmbio:** a moeda base é BRL, mas as contas têm moedas próprias (item 5: EUR, GBP, HUF nos dados de teste) e **não existe nenhuma fonte de taxas no sistema**. Converter para BRL exige contratar/expor um serviço de câmbio, ou o componente de saldo fica inutilizável para clientes multimoeda. **Este é o item aberto mais concreto do desenho.**
- **Compliance / LGPD:** score de risco é dado pessoal com finalidade específica. Mesmo sendo interno, precisa de base legal, política de retenção, log de acesso e trilha dos fatores. **Validar com Jurídico/Privacidade antes de persistir score.**
- **Discriminação algorítmica:** régua com peso alto em saldo e tempo de casa penaliza cliente novo e de baixa renda. Precisa de revisão por Risco/Crédito e de registro dos fatores de cada score. Recomenda-se rodar em modo *shadow* (calculando sem consumidor) antes de qualquer uso em decisão de crédito.
- **Segurança:** o acesso restrito a admin **não é implementável no estado atual** — `SecurityConfig.java` não tem nenhuma noção de role. Enquanto essa claim não existir, publicar a rota no gateway torna o score visível a qualquer chamador autenticado; a alternativa provisória é não publicar a rota e permitir apenas chamada serviço-a-serviço via Eureka. Nota lateral: há um JWT hardcoded em `config-files/api-gateway.properties`, o que reforça que o ambiente atual não é produtivo.
- **Latência do realtime:** os 300 ms só são sustentáveis com score materializado e cache. Qualquer desenho que agregue contas e consulte histórico transacional a cada chamada vai depender de duas chamadas de rede internas, e o `transaction-api` já opera com `TimeLimiter`/`Retry` de Resilience4j (`config-files/transaction-api.properties`: `timeout-duration=3s`, `max-attempts=3`) — ou seja, uma dependência que pode legitimamente levar segundos.
- **Carga do job:** o recálculo em background varre todos os clientes; com `AccountService.getAllAccounts()` no formato atual (item 4), o job precisa de paginação própria em vez de reusar essa leitura.
- **Ausência de CI:** sem workflow em `.github/`, a regressão depende de execução local de `mvn verify`.

## Recomendação

Recomendo a **Abordagem B**, precedida da Fase 0 como épico próprio e priorizado. O SLA de 300 ms em realtime combinado com recálculo diário exige score materializado e cálculo fora do caminho de leitura, e colocar esse motor dentro do `account-api` (A) acopla risco a cadastro num serviço crítico sem economizar o custo da persistência. A Abordagem C só se justifica se o negócio aceitar score defasado entre execuções do job.

### Sequência sugerida

1. **Fase 0 — modelo de dados:** `Customer` no `account-api` (com KYC opcional e data de abertura), `Account.customerId`, transação com cliente/valor/timestamp, evento Kafka enriquecido.
2. **Role de admin** no provedor de identidade + autorização por role no gateway.
3. **`risk-score-api`** com os quatro componentes da régua, endpoint de leitura e consumidor de eventos.
4. **Job diário** de recálculo.
5. **Modo shadow** por um período antes de qualquer uso em decisão de crédito.
6. **Fatores econômicos internos** quando o negócio definir quais são.

## Perguntas em aberto

1. **Fonte de taxas de câmbio para BRL** — sem ela o componente de saldo não funciona para contas em outras moedas. Único item bloqueante que resta.
2. Risco/Crédito valida os limiares propostos (R$ 1.000 / R$ 50.000, 10 e 30 transações/mês, 3× e 5× a mediana)?
3. Risco/Crédito concorda com os **valores neutros para cliente legado e KYC ausente**? Sem eles, a base inteira nasce em MÉDIO/ALTO.
4. Como resolver o **agrupamento conta→cliente da base legada**, dado que `accountHolderName` não é único?
5. Quem cria a **role de admin** no Auth0, e em que prazo? É pré-requisito para expor a consulta.
6. Quais serão os **fatores econômicos internos** (definição pendente no negócio) e qual a fonte deles?
